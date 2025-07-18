package com.example.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.example.objects.Ship;
import com.example.manager.ShipManager;

public class ShipControlListener implements Listener {

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.ARROW || !item.hasItemMeta()) return;

        String name = item.getItemMeta().getDisplayName().replace("§e", "");

        Ship ship = ShipManager.getInstance().getShipByCaptain(event.getPlayer());
        if (ship == null) {
            event.getPlayer().sendMessage("§cТы не создал корабль!");
            return;
        }

        switch (name) {
            case "Вперёд" -> {
                ship.moveForward();
            }
            case "Назад" -> {
                ship.moveBackward();
            }
            case "Влево" -> {
                ship.rotateLeft();
            }
            case "Вправо" -> {
                ship.rotateRight();
            }
        }

        event.setCancelled(true);
    }
}
