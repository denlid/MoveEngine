package com.example.listeners;

import com.example.manager.ShipManager;
import com.example.objects.Ship;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.UUID;


public class ShipLeaveWheelListener implements Listener {

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        LivingEntity entity = event.getExited();

        Player player = (Player) entity.getPassengers().get(0);
        System.out.println("asqqdqdqd");
        if (ShipManager.getInstance().isShipCaptain(player)) {
            System.out.println("bbbabaddjd");
            ShipManager.getInstance().removeShipCaptain(player);
        }
    }
}