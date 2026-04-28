package com.antigravity.upgradedhostile.guis;

import com.antigravity.upgradedhostile.UpgradedHostile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ControlGUI implements Listener {

    private static final String TITLE = "UpgradedHostile Control";
    private final UpgradedHostile plugin;

    public ControlGUI(UpgradedHostile plugin) {
        this.plugin = plugin;
    }

    public static void open(UpgradedHostile plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE);
        FileConfiguration config = plugin.getConfig();

        inv.setItem(0, createToggleItem(Material.ROTTEN_FLESH, "Zombie AI", config.getBoolean("zombie.enabled", true)));
        inv.setItem(1, createToggleItem(Material.GUNPOWDER, "Creeper AI", config.getBoolean("creeper.enabled", true)));
        inv.setItem(2, createToggleItem(Material.BONE, "Skeleton AI", config.getBoolean("skeleton.enabled", true)));
        inv.setItem(3, createToggleItem(Material.SPIDER_EYE, "Spider AI", config.getBoolean("spider.enabled", true)));
        inv.setItem(4, createToggleItem(Material.PHANTOM_MEMBRANE, "Phantom AI", config.getBoolean("phantom.enabled", true)));
        inv.setItem(5, createToggleItem(Material.ENDER_PEARL, "Enderman AI", config.getBoolean("enderman.enabled", true)));
        inv.setItem(6, createToggleItem(Material.GLASS_BOTTLE, "Witch AI", config.getBoolean("witch.enabled", true)));
        
        // Use a generic toggle for Bleeding if we want, or general debug. Let's do debug since it's in general
        inv.setItem(8, createToggleItem(Material.REDSTONE, "Debug Mode", config.getBoolean("general.debug", false)));

        player.openInventory(inv);
    }

    private static ItemStack createToggleItem(Material mat, String name, boolean enabled) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String color = enabled ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
            String status = enabled ? "Enabled" : "Disabled";
            meta.setDisplayName(color + ChatColor.BOLD + name);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Status: " + color + status,
                    "",
                    ChatColor.YELLOW + "Click to toggle!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 9) return;

        FileConfiguration config = plugin.getConfig();
        boolean toggled = false;

        switch (slot) {
            case 0:
                config.set("zombie.enabled", !config.getBoolean("zombie.enabled", true));
                toggled = true;
                break;
            case 1:
                config.set("creeper.enabled", !config.getBoolean("creeper.enabled", true));
                toggled = true;
                break;
            case 2:
                config.set("skeleton.enabled", !config.getBoolean("skeleton.enabled", true));
                toggled = true;
                break;
            case 3:
                config.set("spider.enabled", !config.getBoolean("spider.enabled", true));
                toggled = true;
                break;
            case 4:
                config.set("phantom.enabled", !config.getBoolean("phantom.enabled", true));
                toggled = true;
                break;
            case 5:
                config.set("enderman.enabled", !config.getBoolean("enderman.enabled", true));
                toggled = true;
                break;
            case 6:
                config.set("witch.enabled", !config.getBoolean("witch.enabled", true));
                toggled = true;
                break;
            case 8:
                config.set("general.debug", !config.getBoolean("general.debug", false));
                toggled = true;
                break;
        }

        if (toggled) {
            plugin.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Setting updated! Reloading AI...");
            plugin.reloadPlugin();
            open(plugin, player); // Reopen/refresh the GUI
        }
    }
}
