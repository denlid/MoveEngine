package com.example.listeners;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import com.example.objects.Ship;
import com.example.manager.ShipManager;

public class ShipWheelListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof ArmorStand)) return;
        System.out.println("aaaaaa");
        Ship ship = ShipManager.getInstance().getShipByStand(clicked);
        if (ship == null) return;
        System.out.println("bbbbbb");

        event.setCancelled(true); // чтобы не взять предмет и т.п.

        Player player = event.getPlayer();

        ship.takeWheel(player);

        System.out.println("ccccccc");
    }
}
