package com.example.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.example.objects.Ship;
import com.example.manager.ShipManager;

public class ShipHitboxCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        Ship ship = ShipManager.getInstance().getShipByCaptain(player);
        if (ship == null) {
            player.sendMessage("\u00a7cУ тебя нет активного корабля.");
            return true;
        }

        if (ship.isHitboxEnabled()) {
            ship.stopHitboxPersistent();
            player.sendMessage("§cХитбокс отключён.");
        } else {
            ship.showHitboxPersistent(player);
            player.sendMessage("§aХитбокс включён.");
        }
        return true;
    }
}