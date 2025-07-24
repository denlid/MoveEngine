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
import org.bukkit.entity.ItemDisplay;
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

    private BukkitTask tickTask;
    private int tickCounter = 0;

    private Location lastCenterSnapshot;
    private float lastYawSnapshot;


    public Ship(Location center, float yaw) {
        this.center = center.getBlock().getLocation();
        this.yaw = yaw;
        this.world = center.getWorld();

        this.lastCenterSnapshot = this.center.clone();
        this.lastYawSnapshot = yaw;
    }

    public void spawn() {
        spawnShipDisplay();
        spawnWheel();
        placeBlocks();

        ShipManager.getInstance().registerShip(this, wheelStand, List.of());

        startTickUpdater();
    }


    private void startTickUpdater() {
        if (tickTask != null) return; // уже запущен
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                onTick();
            }
        }.runTaskTimer(getPlugin(), 0L, 1L); // каждый тик
    }

    private void onTick() {

        syncAfterTransform();

    }

    private void syncAfterTransform() {
        // проверим, сместилось ли что-то относительно снапшота
        boolean moved = !center.equals(lastCenterSnapshot);
        boolean rotated = Math.abs(yaw - lastYawSnapshot) > 0.0001f;
        if (!moved && !rotated) return; // нечего делать

        // 1) обновляем коллизионные барьеры
        placeBlocks();

        // 2) модель и штурвал
        updateDisplay();
        updateWheelTransform(); // (updateDisplay уже её зовёт, но оставим на всякий случай)

        // 3) пассажиры
        syncPassengers(lastCenterSnapshot, lastYawSnapshot, center, yaw);

        // сохраняем снапшот
        lastCenterSnapshot = center.clone();
        lastYawSnapshot = yaw;
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
        shipStand = center.getWorld().spawn(center.clone().add(0, 10, 0), ArmorStand.class);
        shipStand.setInvisible(true);
        shipStand.setGravity(true);
        shipStand.setMarker(false);
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
        //shipStand.setVelocity(center.clone().toVector().subtract(lastCenterSnapshot.toVector()));


        /*Location currentLoc = shipStand.getLocation();
        Vector currentPos = currentLoc.toVector();
        Vector targetPos = center.clone().toVector(); // центр, куда должна прийти модель

        Vector velocity = targetPos.subtract(currentPos); // смещение от текущей позиции к целевой

        System.out.println(center.clone());
        System.out.println(center.clone().toVector());
        System.out.println(lastCenterSnapshot);
        System.out.println(lastCenterSnapshot.toVector());
        System.out.println(center.clone().toVector().subtract(currentPos));
        System.out.println(center.clone().toVector().subtract(currentPos).multiply(20));
        System.out.println(velocity);
        System.out.println("=========================");

        shipStand.setVelocity(velocity);*/

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
        center.add(offset.multiply(0.4));
    }


    public void moveBackward() {
        double radians = Math.toRadians(-yaw);
        Vector offset = new Vector(Math.cos(radians), 0, Math.sin(radians));
        center.subtract(offset.multiply(0.4));
    }

    public void rotateRight() {
        yaw += 1;
    }

    public void rotateLeft() {
        yaw -= 1;
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


    /* ---------- УТИЛИТЫ ---------- */

    private void syncPassengers(Location oldCenter, float oldYaw, Location newCenter, float newYaw) {
        // Если вызывается НЕ каждый тик — поменяй на своё значение.
        int ticksBetweenSync = 1; // сколько тиков прошло между old и new состояниями
        double scale = 1.0D / ticksBetweenSync; // скорость = смещение / тики

        // Перенос центра
        double dX = newCenter.getX() - oldCenter.getX();
        double dY = newCenter.getY() - oldCenter.getY();
        double dZ = newCenter.getZ() - oldCenter.getZ();

        // Нормализуем разницу поворота ([-180;180]) чтобы не крутило на полный круг
        float dYaw = wrapYawDiff(newYaw - oldYaw);
        double rad = Math.toRadians(dYaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isPlayerOnShip(player)) continue;

            Location loc = player.getLocation();
            double relX = loc.getX() - oldCenter.getX();
            double relZ = loc.getZ() - oldCenter.getZ();

            // Повернутая относительная позиция
            double rotX =  relX * cos + relZ * sin;
            double rotZ = -relX * sin + relZ * cos;

            // Разница между "как должно быть после шага" и текущим rel-положением
            double dRelX = rotX - relX;
            double dRelZ = rotZ - relZ;

            // Итоговое смещение игрока за интервал
            double moveX = dX + dRelX;
            double moveY = dY;       // вращаем только по Yaw, поэтому только сдвиг по Y
            double moveZ = dZ + dRelZ;

            // Скорость (вектора в Minecraft — блоков/тик примерно)
            Vector vel = new Vector(moveX * scale, moveY * scale, moveZ * scale);

            // Мягкое ограничение, чтобы не вызывало античит / резких рывков (подбери под себя)
            double max = 3.0; // блоков/тик
            if (vel.lengthSquared() > max * max) {
                vel.normalize().multiply(max);
            }

            player.setVelocity(vel);

            // Обновление ориентации игрока опционально:
            // Velocity НЕ повернёт игрока. Если нужно повернуть камеру, используй teleport с флагами,
            // или Paper's player.setRotation(...), или packet.
            // player.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw() + dYaw, loc.getPitch()));
        }
    }

    /**
     * Приводит разницу углов к диапазону [-180; 180], чтобы не получить резкий скачок >180°.
     */
    private float wrapYawDiff(float diff) {
        diff = diff % 360f;
        if (diff > 180f) diff -= 360f;
        if (diff < -180f) diff += 360f;
        return diff;
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