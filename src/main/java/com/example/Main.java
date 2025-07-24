package com.example;

import com.example.commands.ArmorStandCommand;
import org.bukkit.plugin.java.JavaPlugin;

import com.example.commands.ShipControl;
import com.example.commands.ShipHitboxCommand;
import com.example.commands.SpawnShip;
import com.example.listeners.ShipControlListener;
import com.example.listeners.ShipWheelListener;

public class Main extends JavaPlugin {

    private static Main plugin;

    @Override
    public void onEnable() {
        plugin = this;

        getCommand("spawnship").setExecutor(new SpawnShip());
        getCommand("shipcontrol").setExecutor(new ShipControl());
        getCommand("shiphitbox").setExecutor(new ShipHitboxCommand());

        ArmorStandCommand commandExecutor = new ArmorStandCommand();
        getCommand("velocity").setExecutor(commandExecutor);
        getCommand("spawnarmor").setExecutor(commandExecutor);

        getServer().getPluginManager().registerEvents(new ShipControlListener(), this);
        getServer().getPluginManager().registerEvents(new ShipWheelListener(), this);

        getLogger().info("Плагин включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Плагин выключен!");
    }

    public static Main getPlugin() {
        return plugin;
    }
}