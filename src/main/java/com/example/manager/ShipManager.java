package com.example.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.objects.Ship;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ShipManager {
    private static final ShipManager instance = new ShipManager();
    private static final Map<UUID, Ship> ships = new HashMap<>();
    private static final Map<UUID, Map<String, Object>> stands = new HashMap<>();


    public static ShipManager getInstance() {
        return instance;
    }

    public void registerShip(Ship ship, ArmorStand wheelStand, List<ArmorStand> cannonStands) {

        // лучше придумать не мог
        Map<String, Object> wheelMap = new HashMap<>();
        wheelMap.put("type", "wheel");
        wheelMap.put("ship", ship);
        stands.put(wheelStand.getUniqueId(), wheelMap);

        for (ArmorStand cannonStand : cannonStands) {
            Map<String, Object> cannonMap = new HashMap<>();
            cannonMap.put("type", "cannon");
            cannonMap.put("ship", ship);
            stands.put(cannonStand.getUniqueId(), cannonMap);
        }
    }

    public void setShipCaptain(Player player, Ship ship) {
        ships.put(player.getUniqueId(), ship);
    }

    public Ship getShipByCaptain(Player player) {
        return ships.get(player.getUniqueId());
    }

    public void removeShipCaptain(Player player) {
        ships.remove(player.getUniqueId());
    }

    public boolean isShipCaptain(Player player) {
        return ships.containsKey(player.getUniqueId());
    }


    public Ship getShipByStand(Entity entity) {
        return (Ship) stands.get(entity.getUniqueId()).get("ship");
    }
}
