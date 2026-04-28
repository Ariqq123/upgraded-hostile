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

    private static final int SLOT_ZOMBIE = 13;
    private static final int SLOT_CREEPER = 21;
    private static final int SLOT_SKELETON = 23;
    private static final int SLOT_SPIDER = 29;
    private static final int SLOT_PHANTOM = 31;
    private static final int SLOT_ENDERMAN = 33;
    private static final int SLOT_WITCH = 39;
    private static final int SLOT_DEBUG = 41;

    public static void open(UpgradedHostile plugin, Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        FileConfiguration config = plugin.getConfig();

        // Add sleek glass pane background
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Place items in a Diamond/Star layout
        inv.setItem(SLOT_ZOMBIE, createToggleItem(Material.ROTTEN_FLESH, "Zombie AI", config.getBoolean("zombie.enabled", true)));
        inv.setItem(SLOT_CREEPER, createToggleItem(Material.GUNPOWDER, "Creeper AI", config.getBoolean("creeper.enabled", true)));
        inv.setItem(SLOT_SKELETON, createToggleItem(Material.BONE, "Skeleton AI", config.getBoolean("skeleton.enabled", true)));
        inv.setItem(SLOT_SPIDER, createToggleItem(Material.SPIDER_EYE, "Spider AI", config.getBoolean("spider.enabled", true)));
        inv.setItem(SLOT_PHANTOM, createToggleItem(Material.PHANTOM_MEMBRANE, "Phantom AI", config.getBoolean("phantom.enabled", true)));
        inv.setItem(SLOT_ENDERMAN, createToggleItem(Material.ENDER_PEARL, "Enderman AI", config.getBoolean("enderman.enabled", true)));
        inv.setItem(SLOT_WITCH, createToggleItem(Material.GLASS_BOTTLE, "Witch AI", config.getBoolean("witch.enabled", true)));
        inv.setItem(SLOT_DEBUG, createToggleItem(Material.REDSTONE, "Debug Mode", config.getBoolean("general.debug", false)));

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
        if (slot < 0 || slot >= 54) return;

        FileConfiguration config = plugin.getConfig();
        boolean toggled = false;

        if (slot == SLOT_ZOMBIE) {
            config.set("zombie.enabled", !config.getBoolean("zombie.enabled", true));
            toggled = true;
        } else if (slot == SLOT_CREEPER) {
            config.set("creeper.enabled", !config.getBoolean("creeper.enabled", true));
            toggled = true;
        } else if (slot == SLOT_SKELETON) {
            config.set("skeleton.enabled", !config.getBoolean("skeleton.enabled", true));
            toggled = true;
        } else if (slot == SLOT_SPIDER) {
            config.set("spider.enabled", !config.getBoolean("spider.enabled", true));
            toggled = true;
        } else if (slot == SLOT_PHANTOM) {
            config.set("phantom.enabled", !config.getBoolean("phantom.enabled", true));
            toggled = true;
        } else if (slot == SLOT_ENDERMAN) {
            config.set("enderman.enabled", !config.getBoolean("enderman.enabled", true));
            toggled = true;
        } else if (slot == SLOT_WITCH) {
            config.set("witch.enabled", !config.getBoolean("witch.enabled", true));
            toggled = true;
        } else if (slot == SLOT_DEBUG) {
            config.set("general.debug", !config.getBoolean("general.debug", false));
            toggled = true;
        }

        if (toggled) {
            plugin.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Setting updated! Reloading AI...");
            plugin.reloadPlugin();
            open(plugin, player); // Reopen/refresh the GUI
        }
    }
}
