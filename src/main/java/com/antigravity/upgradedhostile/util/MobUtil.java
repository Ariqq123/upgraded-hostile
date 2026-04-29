package com.antigravity.upgradedhostile.util;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Shared utility methods to avoid duplication across handlers.
 * All methods are static and allocation-light.
 *
 * Paper-optimized:
 *   - distanceSquaredFast uses Entity.getX/Y/Z() primitives — no Location object.
 *   - sameWorld uses identity check (==) — faster than equals().
 *   - isLookingAt still needs getLocation() for getDirection(), unavoidable.
 */
public final class MobUtil {

    private MobUtil() {}

    /**
     * Check if a player is looking at a given entity (within a cone defined by threshold).
     * @param threshold dot-product threshold (0.7 ≈ 45° FOV)
     */
    public static boolean isLookingAt(Player player, LivingEntity entity, double threshold) {
        double dx = entity.getX() - player.getX();
        double dy = entity.getY() - player.getY();
        double dz = entity.getZ() - player.getZ();
        double lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq == 0) return true;

        double invLen = 1.0 / Math.sqrt(lenSq);
        dx *= invLen;
        dy *= invLen;
        dz *= invLen;

        // getDirection() still needs a Location, but it's one per call at call site — acceptable.
        Vector dir = player.getLocation().getDirection();
        double dot = dx * dir.getX() + dy * dir.getY() + dz * dir.getZ();
        return dot > threshold;
    }

    /**
     * Check if two living entities are in the same world.
     * Uses identity (==) instead of equals() for faster comparison.
     */
    public static boolean sameWorld(LivingEntity a, LivingEntity b) {
        return a.getWorld() == b.getWorld();
    }

    /**
     * Squared distance using Paper's primitive getX/Y/Z() — no Location object created.
     */
    public static double distanceSquaredFast(Entity a, Entity b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Squared distance between two entities (avoids sqrt when only comparing).
     * Kept for compatibility; prefer distanceSquaredFast where possible.
     */
    public static double distanceSquared(LivingEntity a, LivingEntity b) {
        return distanceSquaredFast(a, b);
    }

    /**
     * Distance between two entities.
     */
    public static double distance(LivingEntity a, LivingEntity b) {
        return Math.sqrt(distanceSquaredFast(a, b));
    }

    /**
     * Find the nearest torch (TORCH or WALL_TORCH) within a cubic scan around a mob.
     * Returns null if none found. Replaces the duplicated O(n³) scan in Zombie/SkeletonHandler.
     *
     * @param entity The mob scanning for torches
     * @param radius Half-extent of the scan cube in blocks (e.g., 3 = 7×5×7 area)
     * @return Nearest torch Block, or null
     */
    public static Block findNearestTorch(LivingEntity entity, int radius) {
        Location loc = entity.getLocation();
        Block nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    if (b.getType() == Material.TORCH || b.getType() == Material.WALL_TORCH) {
                        double distSq = loc.distanceSquared(b.getLocation());
                        if (distSq < nearestDistSq) {
                            nearestDistSq = distSq;
                            nearest = b;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    /**
     * Spawn the Territorial Rage crimson aura around a mob.
     * Called by the dispatcher every slow tick for Rage-chunk mobs.
     * Low-cost: only 4 particles, no allocation beyond Particle.DustOptions.
     */
    public static void spawnRageAura(LivingEntity entity) {
        Location center = entity.getLocation().add(0, 1.0, 0);
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.2f);
        entity.getWorld().spawnParticle(Particle.REDSTONE, center, 4, 0.35, 0.5, 0.35, dust);
    }
}

