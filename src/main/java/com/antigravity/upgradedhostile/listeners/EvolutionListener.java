package com.antigravity.upgradedhostile.listeners;

import com.antigravity.upgradedhostile.managers.EvolutionManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EvolutionListener implements Listener {

    private final EvolutionManager evolutionManager;
    private final FileConfiguration config;

    public EvolutionListener(EvolutionManager evolutionManager, FileConfiguration config) {
        this.evolutionManager = evolutionManager;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonsterDeath(EntityDeathEvent event) {
        if (!config.getBoolean("general.evolution-enabled", true)) return;

        if (!(event.getEntity() instanceof Monster)) return;
        
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        evolutionManager.recordKill(event.getEntity().getLocation().getChunk());
    }
}
