package com.antigravity.upgradedhostile.listeners;

import com.antigravity.upgradedhostile.UpgradedHostile;
import com.antigravity.upgradedhostile.managers.EvolutionManager;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EvolutionListener implements Listener {

    private final EvolutionManager evolutionManager;
    private final UpgradedHostile plugin;

    public EvolutionListener(EvolutionManager evolutionManager, UpgradedHostile plugin) {
        this.evolutionManager = evolutionManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonsterDeath(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("general.evolution-enabled", true)) return;

        if (!(event.getEntity() instanceof Monster)) return;
        
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        evolutionManager.recordKill(event.getEntity().getLocation().getChunk());
    }
}
