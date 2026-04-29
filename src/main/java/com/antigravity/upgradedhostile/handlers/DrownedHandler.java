package com.antigravity.upgradedhostile.handlers;

import com.antigravity.upgradedhostile.UpgradedHostile;
import com.antigravity.upgradedhostile.managers.EvolutionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DrownedHandler {

    private final UpgradedHostile plugin;
    private final EvolutionManager evolutionManager;
    private final Map<UUID, Long> harpoonCooldowns = new HashMap<>();

    private final long harpoonCooldownMs;
    private final double minEvolutionFactor;
    private final double pullStrength;

    public DrownedHandler(UpgradedHostile plugin, FileConfiguration config, EvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
        this.harpoonCooldownMs = config.getLong("drowned.harpoon-cooldown-ms", 12000L);
        this.minEvolutionFactor = config.getDouble("drowned.min-evolution-factor", 0.4);
        this.pullStrength = config.getDouble("drowned.pull-strength", 0.3);
    }

    public void handle(Drowned drowned) {
        if (!(drowned.getTarget() instanceof Player target)) return;
        if (drowned.getWorld() != target.getWorld()) return;

        // Basic water-circle: if in water and target is within 8 blocks, strafe around them
        if (drowned.isInWater()) {
            double distSq = drowned.getLocation().distanceSquared(target.getLocation());
            if (distSq < 64.0 && distSq > 4.0) { // 8 blocks max, 2 blocks min
                org.bukkit.util.Vector toPlayer = target.getLocation().toVector()
                        .subtract(drowned.getLocation().toVector()).normalize();
                // Perpendicular strafe vector (rotate 90°)
                org.bukkit.util.Vector strafe = new org.bukkit.util.Vector(-toPlayer.getZ(), 0, toPlayer.getX());
                org.bukkit.Location strafeLoc = drowned.getLocation().clone().add(strafe.multiply(2.0));
                drowned.getPathfinder().moveTo(strafeLoc);
            }
        }
    }

    public void attemptHarpoon(Drowned drowned, Player target, ItemStack tridentItem) {
        if (tridentItem == null || tridentItem.getType() != Material.TRIDENT) return;
        if (!tridentItem.hasItemMeta() || !tridentItem.getItemMeta().hasEnchant(org.bukkit.enchantments.Enchantment.LOYALTY)) return;

        UUID id = drowned.getUniqueId();
        long now = System.currentTimeMillis();
        if (now - harpoonCooldowns.getOrDefault(id, 0L) < harpoonCooldownMs) return;

        // Check evolution
        double evoFactor = evolutionManager.getEvolutionFactor(drowned.getLocation().getChunk());
        if (evoFactor < minEvolutionFactor) return;

        harpoonCooldowns.put(id, now);
        startHarpoonTask(drowned, target);
    }

    private void startHarpoonTask(Drowned drowned, Player target) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 30;

            @Override
            public void run() {
                if (ticks >= maxTicks || !drowned.isValid() || !target.isValid()) {
                    this.cancel();
                    return;
                }

                if (drowned.getWorld() != target.getWorld()) {
                    this.cancel();
                    return;
                }

                double distSq = drowned.getLocation().distanceSquared(target.getLocation());
                if (distSq > 400 || distSq < 1.44) { // 20 blocks max, 1.2 blocks min
                    this.cancel();
                    return;
                }

                // Physics Tug
                if (target.isInWater()) {
                    // Pull Drowned to Player
                    Vector dir = target.getLocation().toVector().subtract(drowned.getLocation().toVector()).normalize();
                    drowned.setVelocity(drowned.getVelocity().add(dir.multiply(pullStrength)));
                } else {
                    // Pull Player to Drowned (Land to Water)
                    Vector dir = drowned.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
                    // Add slight downward force to drag player under
                    dir.setY(dir.getY() - 0.1);
                    target.setVelocity(target.getVelocity().add(dir.multiply(pullStrength)));
                }

                // Visual Particles
                spawnHarpoonParticles(drowned.getLocation(), target.getLocation());

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnHarpoonParticles(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize();

        for (double d = 0; d < distance; d += 0.5) {
            Location loc = start.clone().add(direction.clone().multiply(d));
            start.getWorld().spawnParticle(Particle.WATER_WAKE, loc, 1, 0, 0, 0, 0);
        }
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        harpoonCooldowns.entrySet().removeIf(e -> now - e.getValue() > harpoonCooldownMs * 2);
    }
}
