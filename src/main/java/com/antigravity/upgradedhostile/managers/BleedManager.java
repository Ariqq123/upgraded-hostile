package com.antigravity.upgradedhostile.managers;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BleedManager extends BukkitRunnable {

    // ConcurrentHashMap to allow safe access from multiple threads/tasks if needed
    private final Map<UUID, Long> bleedingPlayers = new ConcurrentHashMap<>();

    /**
     * Start bleeding for a player.
     * @param player The player
     * @param durationTicks How long the bleed lasts
     */
    public void startBleeding(Player player, long durationTicks) {
        long expiry = System.currentTimeMillis() + (durationTicks * 50); // 1 tick = 50ms
        bleedingPlayers.put(player.getUniqueId(), expiry);
    }

    /**
     * Check if a player is currently bleeding.
     */
    public boolean isBleeding(UUID playerId) {
        Long expiry = bleedingPlayers.get(playerId);
        return expiry != null && expiry > System.currentTimeMillis();
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> it = bleedingPlayers.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID uuid = entry.getKey();
            Long expiry = entry.getValue();

            if (now > expiry) {
                it.remove();
                continue;
            }

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }

            // Spawn redstone dust particles at feet
            // Note: REDSTONE is used for red color. In newer versions, REDSTONE is used with Particle.DustOptions
            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.RED, 1.0F);
            player.getWorld().spawnParticle(
                    Particle.REDSTONE,
                    player.getLocation().add(0, 0.2, 0),
                    5, // count
                    0.2, 0.1, 0.2, // offset
                    dustOptions
            );
        }
    }
}
