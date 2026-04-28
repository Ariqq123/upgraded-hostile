package com.antigravity.upgradedhostile.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Shared utility methods to avoid duplication across handlers.
 * All methods are static and allocation-light.
 */
public final class MobUtil {

    private MobUtil() {}

    /**
     * Check if a player is looking at a given entity (within a cone defined by threshold).
     * @param threshold dot-product threshold (0.7 ≈ 45° FOV)
     */
    public static boolean isLookingAt(Player player, LivingEntity entity, double threshold) {
        double dx = entity.getLocation().getX() - player.getLocation().getX();
        double dy = entity.getLocation().getY() - player.getLocation().getY();
        double dz = entity.getLocation().getZ() - player.getLocation().getZ();
        double lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq == 0) return true;

        double invLen = 1.0 / Math.sqrt(lenSq);
        dx *= invLen;
        dy *= invLen;
        dz *= invLen;

        Vector dir = player.getLocation().getDirection();
        double dot = dx * dir.getX() + dy * dir.getY() + dz * dir.getZ();
        return dot > threshold;
    }

    /**
     * Check if two living entities are in the same world.
     */
    public static boolean sameWorld(LivingEntity a, LivingEntity b) {
        return a.getWorld().equals(b.getWorld());
    }

    /**
     * Squared distance between two entities (avoids sqrt when only comparing).
     */
    public static double distanceSquared(LivingEntity a, LivingEntity b) {
        double dx = a.getLocation().getX() - b.getLocation().getX();
        double dy = a.getLocation().getY() - b.getLocation().getY();
        double dz = a.getLocation().getZ() - b.getLocation().getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Distance between two entities.
     */
    public static double distance(LivingEntity a, LivingEntity b) {
        return Math.sqrt(distanceSquared(a, b));
    }
}
