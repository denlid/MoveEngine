package com.example.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ArmorStandCommand implements CommandExecutor {

    private ArmorStand armorStand; // Сохраняем последний заспавненный ArmorStand

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может использовать только игрок!");
            return true;
        }
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("spawnarmor")) {
            Location spawnLoc;

            // Парсим координаты (первые 3 аргумента если есть)
            if (args.length >= 3) {
                try {
                    double x = parseCoordinate(player.getLocation().getX(), args[0]);
                    double y = parseCoordinate(player.getLocation().getY(), args[1]);
                    double z = parseCoordinate(player.getLocation().getZ(), args[2]);
                    spawnLoc = new Location(player.getWorld(), x, y, z);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cНеверный формат координат!");
                    return true;
                }
            } else {
                spawnLoc = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(2));
            }

            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);

            // Значения по умолчанию
            boolean gravity = true;
            boolean visible = true;
            boolean small = false;
            boolean marker = false;
            boolean silent = false;
            boolean collidable = true;
            boolean baseplate = true;
            boolean nameVisible = false;
            String customName = null;

            // Парсим параметры key:value начиная с 4-го аргумента (index 3)
            for (int i = 3; i < args.length; i++) {
                String arg = args[i].toLowerCase();
                if (arg.startsWith("gravity:")) gravity = Boolean.parseBoolean(arg.substring(8));
                else if (arg.startsWith("visible:")) visible = Boolean.parseBoolean(arg.substring(8));
                else if (arg.startsWith("small:")) small = Boolean.parseBoolean(arg.substring(6));
                else if (arg.startsWith("marker:")) marker = Boolean.parseBoolean(arg.substring(7));
                else if (arg.startsWith("silent:")) silent = Boolean.parseBoolean(arg.substring(7));
                else if (arg.startsWith("collidable:")) collidable = Boolean.parseBoolean(arg.substring(11));
                else if (arg.startsWith("baseplate:")) baseplate = Boolean.parseBoolean(arg.substring(10));
                else if (arg.startsWith("namevisible:")) nameVisible = Boolean.parseBoolean(arg.substring(12));
                else if (arg.startsWith("name:")) customName = args[i].substring(5);
            }

            stand.setGravity(gravity);
            stand.setVisible(visible);
            stand.setSmall(small);
            stand.setMarker(marker);
            stand.setSilent(silent);
            stand.setCollidable(collidable);
            stand.setBasePlate(baseplate);
            stand.setCustomName(customName);
            stand.setCustomNameVisible(nameVisible);

            armorStand = stand; // сохраняем для последующих команд

            player.sendMessage("§aArmorStand заспавнен на " + formatLocation(spawnLoc) + " с параметрами:");
            player.sendMessage("§a gravity=" + gravity + ", visible=" + visible + ", small=" + small);
            player.sendMessage("§a marker=" + marker + ", silent=" + silent + ", collidable=" + collidable);
            player.sendMessage("§a baseplate=" + baseplate + ", namevisible=" + nameVisible + ", name=" + (customName == null ? "null" : customName));

            return true;
        }

        if (command.getName().equalsIgnoreCase("velocity")) {
            if (armorStand == null || armorStand.isDead()) {
                player.sendMessage("§cСначала заспавни ArmorStand командой /spawnarmor");
                return true;
            }

            if (args.length != 3) {
                player.sendMessage("§eИспользование: /velocity <x> <y> <z>");
                return true;
            }

            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);

                armorStand.setVelocity(new Vector(x, y, z));
                player.sendMessage("§aVelocity установлен: X=" + x + " Y=" + y + " Z=" + z);
            } catch (NumberFormatException e) {
                player.sendMessage("§cВсе параметры должны быть числами!");
            }
            return true;
        }

        return false;
    }

    private double parseCoordinate(double base, String input) {
        if (input.startsWith("~")) {
            if (input.length() == 1) return base;
            return base + Double.parseDouble(input.substring(1));
        }
        return Double.parseDouble(input);
    }

    private String formatLocation(Location loc) {
        return String.format("X=%.2f Y=%.2f Z=%.2f", loc.getX(), loc.getY(), loc.getZ());
    }

}
