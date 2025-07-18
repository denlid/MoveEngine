package com.example.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ShipControl implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;

        giveControl(player, "Вперёд");
        giveControl(player, "Назад");
        giveControl(player, "Влево");
        giveControl(player, "Вправо");

        player.sendMessage("§aВыдано управление кораблём!");
        return true;
    }

    private void giveControl(Player player, String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + name);
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
    }
}
