package com.example.objects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.manager.ShipManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import static com.example.Main.getPlugin;
import static com.example.utils.GeometryUtils.getTouchedCells;
import static com.example.utils.GeometryUtils.rotateAroundY;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.SimpleManualAnimator;

public class Ship {
    private final World world;
    private final Location center;
    private float yaw;

    private final int width = 10;
    private final int length = 4;
    private final int height = 2;

    private final Set<Location> lastBlocks = new HashSet<>();

    private boolean hitboxEnabled = false;
    private BukkitTask hitboxTask;

    private ArmorStand shipStand;
    private ModeledEntity modeledEntity;
    private ActiveModel activeModel;

    private ArmorStand wheelStand; // штурвал
    private final Vector wheelOffset = new Vector(0, 4.0, 0);

    private final List<Vector> cannonsOffsets = List.of(new Vector(0, 10.0, 0));


    public Ship(Location center, float yaw) {
        this.center = center.getBlock().getLocation();
        this.yaw = yaw;
        this.world = center.getWorld();
    }

    public void spawn() {
        spawnShipDisplay();
        spawnWheel();
        placeBlocks();

        ShipManager.getInstance().registerShip(this, wheelStand, List.of());
    }


    /* ---------- КОЛИЗИЯ ---------- */

    public void placeBlocks() {
        for (Location loc : lastBlocks) {
            world.getBlockAt(loc).setType(Material.AIR);
        }

        List<int[]> newCords = getTouchedCells(
            center.getX(),
            center.getY() + 0.5,
            center.getZ(),
            width,
            height,
            length,
            yaw
        );

        List<Location> newBlocks = new ArrayList<>();

        for (int[] cords : newCords) {
            Location loc = new Location(world, cords[0], cords[1], cords[2]);
            world.getBlockAt(loc).setType(Material.BARRIER);
            newBlocks.add(loc);
        }

        lastBlocks.clear();
        lastBlocks.addAll(newBlocks);
    }

    /* ---------- ОТРИСОВКА МОДЕЛИ ---------- */

    private void spawnShipDisplay() {
        // Спавним невидимый ArmorStand без физики
        shipStand = center.getWorld().spawn(center, ArmorStand.class);
        shipStand.setInvisible(true);
        shipStand.setGravity(false);
        shipStand.setMarker(true);
        shipStand.setSilent(true);
        shipStand.setCollidable(false);
        shipStand.setBasePlate(false);
        shipStand.setSmall(false);

        modeledEntity = ModelEngineAPI.createModeledEntity(shipStand);

        activeModel = ModelEngineAPI.createActiveModel("galleon");
        if (activeModel == null) {
            Bukkit.getLogger().warning("[Ship] Модель 'galleon' не найдена!");
            return;
        }

        // Добавляем модель к ArmorStand
        modeledEntity.addModel(activeModel, true);

        // Обновляем позицию и поворот
        updateDisplay();
    }


    public void updateDisplay() {
        if (shipStand == null) return;

        shipStand.teleport(center.clone());
        updateWheelTransform();

        activeModel.getBone("bone").ifPresent(modelBone -> {
            float yawRad = (float) Math.toRadians(yaw+90);

            SimpleManualAnimator animator = new SimpleManualAnimator();
            animator.getRotation().rotationY(yawRad);
            modelBone.setManualAnimator(animator);
        });
    }


    /* ---------- ШТУРВАЛ ---------- */

    private void spawnWheel() {
        wheelStand = world.spawn(center, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setGravity(false);
            as.setMarker(false);     // НЕ marker — иначе нельзя ездить!
            as.setSilent(true);
            as.setCollidable(false);
            as.setBasePlate(false);
            as.setSmall(true);
            as.setCustomNameVisible(false);

            as.getEquipment().setHelmet(new ItemStack(Material.OAK_FENCE));
        });
        updateWheelTransform(); // позиционируем
    }

    private void updateWheelTransform() {
        if (wheelStand == null) return;
        // повернём локальный оффсет по yaw корабля
        Vector rotated = rotateAroundY(wheelOffset.clone(), yaw); // твоя утилита
        Location seatLoc = center.clone().add(rotated);
        seatLoc.setYaw(yaw);

        Entity passenger = wheelStand.getPassenger();
        wheelStand.eject();
        wheelStand.teleport(seatLoc);
        wheelStand.setPassenger(passenger);
    }

    public void takeWheel(Player player) {
        if (wheelStand == null) return;
        if (!wheelStand.getPassengers().contains(player)) {
            wheelStand.addPassenger(player);
            ShipManager.getInstance().setShipCaptain(player, this);
        }
    }

    public boolean isPlayerAtWheel(Player p) {
        return wheelStand != null && wheelStand.getPassengers().contains(p);
    }


    /* ---------- ДВИЖЕНИЕ ---------- */

    public void moveForward() {
        double radians = Math.toRadians(-yaw);
        Vector offset = new Vector(Math.cos(radians), 0, Math.sin(radians));
        moveSmoothly(offset, 10); // 10 шагов
    }


    public void moveBackward() {
        double radians = Math.toRadians(-yaw);
        Vector offset = new Vector(-Math.cos(radians), 0, -Math.sin(radians));
        moveSmoothly(offset, 10); // 10 шагов назад

        updateDisplay();
    }

    public void rotateRight() {
        rotateSmoothly(15f, 30); // 15 градусов за 10 шагов
    }

    public void rotateLeft() {
        rotateSmoothly(-15f, 30); // 15 градусов за 10 шагов
    }


    public void moveSmoothly(Vector offset, int steps) {
        Vector stepOffset = offset.clone().multiply(1.0 / steps);

        new BukkitRunnable() {
            int currentStep = 0;

            @Override
            public void run() {
                if (currentStep >= steps) {
                    cancel();
                    return;
                }

                center.add(stepOffset);
                placeBlocks();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isPlayerOnShip(player)) {
                        Location current = player.getLocation();
                        Location newLoc = current.clone().add(stepOffset); // плавный шаг

                        // Важно: сохраняем yaw/pitch
                        newLoc.setYaw(current.getYaw());
                        newLoc.setPitch(current.getPitch());

                        player.teleport(newLoc);
                    }
                }

                updateDisplay();
                currentStep++;
            }
        }.runTaskTimer(getPlugin(), 0L, 1L); // 1 тик = 0.05 сек
    }

    public void rotateSmoothly(float totalAngle, int steps) {
        float angleStep = totalAngle / steps;

        new BukkitRunnable() {
            int currentStep = 0;

            @Override
            public void run() {
                if (currentStep >= steps) {
                    cancel();
                    return;
                }

                double radians = Math.toRadians(angleStep);

                // Поворот корабля
                yaw += angleStep;
                placeBlocks();

                updateDisplay();

                // Поворот всех игроков вокруг center
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!isPlayerOnShip(player)) continue;

                    Location loc = player.getLocation();

                    double dx = loc.getX() - center.getX();
                    double dz = loc.getZ() - center.getZ();

                    double rotatedX = dx * Math.cos(radians) + dz * Math.sin(radians);
                    double rotatedZ = -dx * Math.sin(radians) + dz * Math.cos(radians);

                    Location newLoc = new Location(
                            loc.getWorld(),
                            center.getX() + rotatedX,
                            loc.getY(),
                            center.getZ() + rotatedZ,
                            loc.getYaw() - angleStep,
                            loc.getPitch()
                    );

                    player.teleport(newLoc);
                }

                currentStep++;
            }
        }.runTaskTimer(getPlugin(), 0L, 1L);
    }


    /* ---------- ОТОБРАЖЕНИЕ ХИТБОКСОВ ---------- */

    public void showHitbox(Player player) {
        Set<Location> visualPoints = new HashSet<>();
        double step = 0.5;
        double halfWidth = width / 2.0;
        double halfLength = length / 2.0;

        double radians = Math.toRadians(-yaw);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        // 8 углов (x, y, z)
        double[] xs = {-halfWidth, halfWidth};
        double[] ys = {0, height};
        double[] zs = {-halfLength, halfLength};

        // 12 рёбер (по два разных координата)
        for (double y : ys) {
            for (double x1 : xs) {
                for (double z1 : zs) {
                    for (double x2 : xs) {
                        for (double z2 : zs) {
                            // Только по одному изменяющемуся направлению
                            int changes = 0;
                            if (x1 != x2) changes++;
                            if (z1 != z2) changes++;
                            if (changes != 1) continue;

                            double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2));
                            int points = (int) (length / step);

                            for (int i = 0; i <= points; i++) {
                                double t = i / (double) points;
                                double x = x1 + t * (x2 - x1);
                                double z = z1 + t * (z2 - z1);

                                // Поворот вокруг Y
                                double rx = x * cos - z * sin;
                                double rz = x * sin + z * cos;

                                Location loc = center.clone().add(rx, y, rz);
                                visualPoints.add(loc);
                            }
                        }
                    }
                }
            }
        }

        // Вертикальные рёбра
        for (double x : xs) {
            for (double z : zs) {
                for (double y1 = 0; y1 <= height; y1 += step) {
                    double rx = x * cos - z * sin;
                    double rz = x * sin + z * cos;
                    Location loc = center.clone().add(rx, y1, rz);
                    visualPoints.add(loc);
                }
            }
        }

        for (Location loc : visualPoints) {
            player.spawnParticle(Particle.REDSTONE, loc, 1, new Particle.DustOptions(org.bukkit.Color.LIME, 1));
        }
    }


    public void showHitboxPersistent(Player player) {
        hitboxEnabled = true;

        if (hitboxTask != null) {
            hitboxTask.cancel();
        }

        hitboxTask = new BukkitRunnable() {
            @Override
            public void run() {
                showHitbox(player); // Ваш текущий метод для отображения хитбокса
            }
        }.runTaskTimer(getPlugin(), 0L, 5L); // Каждую секунду
    }

    public void stopHitboxPersistent() {
        hitboxEnabled = false;

        if (hitboxTask != null) {
            hitboxTask.cancel();
            hitboxTask = null;
        }
    }

    public boolean isHitboxEnabled() {
        return hitboxEnabled;
    }


    public boolean isPlayerOnShip(Player player) {
        Location loc = player.getLocation();
        double dx = loc.getX() - center.getX();
        double dz = loc.getZ() - center.getZ();
        double dy = loc.getY() - center.getY();

        double radians = Math.toRadians(-yaw);
        double localX = dx * Math.cos(radians) + dz * Math.sin(radians);
        double localZ = -dx * Math.sin(radians) + dz * Math.cos(radians);

        return Math.abs(localX) <= width / 2.0 &&
            Math.abs(localZ) <= length / 2.0 &&
            dy >= 0 && dy <= height;
    }

}
