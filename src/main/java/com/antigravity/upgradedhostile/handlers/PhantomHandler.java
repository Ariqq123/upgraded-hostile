package com.antigravity.upgradedhostile.handlers;

import com.antigravity.upgradedhostile.util.MobUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PhantomHandler {

    private final double speedBoostMultiplier;
    private final int persistenceHits;
    private final double maxVelocity; // Fix #5: velocity clamp

    private final Map<UUID, Integer> hitCounts = new HashMap<>();

    // Collect phantoms grouped by target for coordination (built per-tick, not stored)
    private final Map<UUID, List<Phantom>> phantomsByTarget = new HashMap<>();

    public PhantomHandler(FileConfiguration config) {
        this.speedBoostMultiplier = config.getDouble("phantom.speed-boost-multiplier", 1.5);
        this.persistenceHits = config.getInt("phantom.persistence-hits", 3);
        this.maxVelocity = config.getDouble("phantom.max-velocity", 2.0);
    }

    /**
     * Called before iterating entities to reset per-tick state.
     */
    public void beginTick() {
        phantomsByTarget.clear();
    }

    public void handle(Phantom phantom) {
        if (!(phantom.getTarget() instanceof Player target)) return;
        if (!MobUtil.sameWorld(phantom, target)) return;

        phantomsByTarget.computeIfAbsent(target.getUniqueId(), k -> new java.util.ArrayList<>())
                .add(phantom);

        // Behavior 1: Speed burst when player isn't looking up
        if (!MobUtil.isLookingAt(target, phantom, 0.5)) {
            applySpeedBoost(phantom);
        }
    }

    /**
     * Called after all entities have been processed to do coordination.
     */
    public void endTick() {
        for (List<Phantom> phantoms : phantomsByTarget.values()) {
            if (phantoms.size() >= 2) {
                coordinateAttack(phantoms);
            }
        }
    }

    public void cleanup() {
        hitCounts.entrySet().removeIf(e -> {
            // We don't store entity refs for hitCounts, but entries are small (UUID→int).
            // Only clean when the set gets large to amortize cost.
            return false; // Let endTick naturally trim via phantomsByTarget
        });
        // Trim hitCounts if it gets too large (safety valve)
        if (hitCounts.size() > 500) {
            hitCounts.clear();
        }
    }

    private void applySpeedBoost(Phantom phantom) {
        Vector velocity = phantom.getVelocity();
        if (velocity.getY() < -0.1) {
            Vector boosted = velocity.clone().multiply(speedBoostMultiplier);

            // Fix #5: Clamp velocity to prevent compounding to absurd speeds
            if (boosted.length() > maxVelocity) {
                boosted.normalize().multiply(maxVelocity);
            }

            phantom.setVelocity(boosted);
        }
    }

    private void coordinateAttack(List<Phantom> phantoms) {
        if (phantoms.isEmpty()) return;

        Player target = (Player) phantoms.get(0).getTarget();
        if (target == null) return;

        double maxDistSq = 0;
        for (Phantom p : phantoms) {
            double distSq = MobUtil.distanceSquaredFast(p, target);
            if (distSq > maxDistSq) maxDistSq = distSq;
        }

        double syncThresholdSq = maxDistSq * 0.36; // 0.6^2
        for (Phantom phantom : phantoms) {
            double distSq = MobUtil.distanceSquaredFast(phantom, target);
            if (distSq < syncThresholdSq) {
                Vector vel = phantom.getVelocity();
                phantom.setVelocity(vel.multiply(0.5));
            }
        }
    }

    public void recordHit(UUID phantomId) {
        hitCounts.merge(phantomId, 1, Integer::sum);
    }

    public boolean shouldPersist(UUID phantomId) {
        return hitCounts.getOrDefault(phantomId, 0) < persistenceHits;
    }
}
