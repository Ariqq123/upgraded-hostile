package com.antigravity.upgradedhostile.listeners;

import com.antigravity.upgradedhostile.managers.EvolutionManager;
import com.antigravity.upgradedhostile.util.MobUtil;
import org.bukkit.Chunk;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Handles Territorial Rage cross-cutting events:
 *   1. Damage scaling  — Rage mobs deal a bonus multiplier on each hit.
 *   2. Revenge aggro   — Killing a Rage mob makes all nearby Rage mobs re-target the killer.
 *
 * @author azreyzaako
 */
public class RageListener implements Listener {

    private final EvolutionManager evolutionManager;
    private final double damageMultiplier;
    private final double revengeRangeSq;
    private final boolean rageEnabled;

    public RageListener(EvolutionManager evolutionManager,
                        double damageMultiplier,
                        double revengeRange,
                        boolean rageEnabled) {
        this.evolutionManager = evolutionManager;
        this.damageMultiplier = damageMultiplier;
        this.revengeRangeSq = revengeRange * revengeRange;
        this.rageEnabled = rageEnabled;
    }

    /**
     * Damage scaling: Rage mobs deal multiplied damage.
     * Uses HIGH priority to run after armor/resistance calculations but before damage is applied.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!rageEnabled) return;
        if (!(event.getDamager() instanceof Monster attacker)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Chunk chunk = attacker.getLocation().getChunk();
        if (!evolutionManager.isRaging(chunk)) return;

        event.setDamage(event.getDamage() * damageMultiplier);
    }

    /**
     * Revenge aggro: When a player kills a Rage mob, all nearby Rage mobs in the same chunk
     * immediately converge on that player.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!rageEnabled) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!(event.getEntity() instanceof Monster)) return;

        Chunk chunk = event.getEntity().getLocation().getChunk();
        if (!evolutionManager.isRaging(chunk)) return;

        // Scan nearby entities and force Rage mobs to target the killer
        for (Entity nearby : killer.getNearbyEntities(24, 24, 24)) {
            if (!(nearby instanceof Monster mob)) continue;
            if (!mob.isValid()) continue;
            // Only apply revenge if this mob is also in a Rage chunk
            if (!evolutionManager.isRaging(mob.getLocation().getChunk())) continue;

            mob.setTarget(killer);
        }

        // Play a subtle "rage roar" sound at the death location to signal the escalation
        event.getEntity().getWorld().playSound(
                event.getEntity().getLocation(),
                Sound.ENTITY_ENDER_DRAGON_GROWL,
                0.4f, 1.8f
        );
    }
}
