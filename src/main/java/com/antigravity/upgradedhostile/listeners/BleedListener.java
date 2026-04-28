package com.antigravity.upgradedhostile.listeners;

import com.antigravity.upgradedhostile.UpgradedHostile;
import com.antigravity.upgradedhostile.managers.BleedManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Random;

public class BleedListener implements Listener {

    private final UpgradedHostile plugin;
    private final BleedManager bleedManager;
    private final double bleedChance;
    private final long bleedDuration;
    private final Random random = new Random();

    public BleedListener(UpgradedHostile plugin, BleedManager bleedManager, FileConfiguration config) {
        this.plugin = plugin;
        this.bleedManager = bleedManager;
        this.bleedChance = config.getDouble("general.bleed-chance", 0.15);
        this.bleedDuration = config.getLong("general.bleed-duration-ticks", 200);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only bleed if hit by a monster (smell blood logic)
        if (!(event.getDamager() instanceof Monster)) {
            return;
        }

        if (random.nextDouble() < bleedChance) {
            bleedManager.startBleeding(player, bleedDuration);
            plugin.debug(player.getName() + " started bleeding after being hit by " + event.getDamager().getType().name());
        }
    }
}
