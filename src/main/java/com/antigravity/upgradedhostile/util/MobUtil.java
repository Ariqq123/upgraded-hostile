package com.antigravity.upgradedhostile.util;

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
}
