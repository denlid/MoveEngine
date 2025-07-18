package com.example.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.example.objects.Ship;
import com.example.manager.ShipManager;

public class SpawnShip implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        float yaw = player.getLocation().getYaw();
        Ship ship = new Ship(player.getLocation(), yaw);
        ship.spawn();

        player.sendMessage("§aКорабль успешно заспавнен!");
        return true;
    }
}
