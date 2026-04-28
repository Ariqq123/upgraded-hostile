package com.antigravity.upgradedhostile.handlers;

import com.antigravity.upgradedhostile.util.MobUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ZombieHandler {

    private final JavaPlugin plugin;
    private final Map<Location, BlockDamageEntry> blockDamage = new HashMap<>();

    private final int maxDamage;
    private final double detectionRangeSq;
    private final double minRangeSq;
    private final long staleTimeoutMs;

    // Run cleanup only every N calls to amortize cost
    private int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 20;

    public ZombieHandler(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.maxDamage = config.getInt("zombie.break-hits", 10);
        double dr = config.getDouble("zombie.detection-range", 4.0);
        double mr = config.getDouble("zombie.min-range", 1.2);
        this.detectionRangeSq = dr * dr;
        this.minRangeSq = mr * mr;
        this.staleTimeoutMs = config.getLong("zombie.stale-timeout-ms", 10000);
    }

    public void handle(Zombie zombie) {
        if (!(zombie.getTarget() instanceof Player target)) return;
        if (!MobUtil.sameWorld(zombie, target)) return;

        double distSq = MobUtil.distanceSquared(zombie, target);
        if (distSq < detectionRangeSq && distSq > minRangeSq) {
            attemptBreakBlock(zombie, target);
        }
    }

    public void cleanup() {
        if (++cleanupCounter < CLEANUP_INTERVAL) return;
        cleanupCounter = 0;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Location, BlockDamageEntry>> it = blockDamage.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, BlockDamageEntry> entry = it.next();
            if (now - entry.getValue().lastHitTime > staleTimeoutMs) {
                sendBlockDamageToNearby(entry.getKey(), -1);
                it.remove();
            }
        }
    }

    private void attemptBreakBlock(Zombie zombie, Player target) {
        Vector direction = target.getLocation().toVector()
                .subtract(zombie.getLocation().toVector()).normalize();
        Location eyeLocation = zombie.getEyeLocation();

        for (double d = 0.5; d <= 1.5; d += 0.5) {
            Location checkLoc = eyeLocation.clone().add(direction.clone().multiply(d));

            Block block = checkLoc.getBlock();
            if (tryDamage(block, zombie)) return;

            Block footBlock = checkLoc.clone().subtract(0, 1, 0).getBlock();
            if (tryDamage(footBlock, zombie)) return;
        }
    }

    private boolean tryDamage(Block block, Zombie zombie) {
        if (!isBreakable(block)) return false;

        damageBlock(block);
        zombie.swingMainHand();
        block.getWorld().playSound(
                block.getLocation(),
                block.getBlockData().getSoundGroup().getHitSound(),
                0.5f, 0.5f
        );
        return true;
    }

    private boolean isBreakable(Block block) {
        Material type = block.getType();
        if (!type.isSolid() || type.isAir()) return false;
        return !type.name().contains("DOOR")
                && type != Material.BEDROCK
                && type != Material.BARRIER
                && type != Material.OBSIDIAN
                && type != Material.END_PORTAL_FRAME
                && type != Material.COMMAND_BLOCK
                && type != Material.CHAIN_COMMAND_BLOCK
                && type != Material.REPEATING_COMMAND_BLOCK;
    }

    private void damageBlock(Block block) {
        Location loc = block.getLocation();
        BlockDamageEntry entry = blockDamage.getOrDefault(loc, new BlockDamageEntry(0));
        entry.damage++;
        entry.lastHitTime = System.currentTimeMillis();

        if (entry.damage >= maxDamage) {
            block.breakNaturally();
            blockDamage.remove(loc);
            sendBlockDamageToNearby(loc, -1);
        } else {
            blockDamage.put(loc, entry);
            float progress = (float) entry.damage / maxDamage;
            sendBlockDamageToNearby(loc, progress);
            block.getWorld().spawnParticle(
                    Particle.BLOCK_CRACK, loc.clone().add(0.5, 0.5, 0.5),
                    10, 0.2, 0.2, 0.2, block.getBlockData()
            );
        }
    }

    private void sendBlockDamageToNearby(Location loc, float progress) {
        for (Player player : loc.getWorld().getNearbyPlayers(loc, 32)) {
            player.sendBlockDamage(loc, Math.max(progress, 0f));
        }
    }

    private static class BlockDamageEntry {
        int damage;
        long lastHitTime;
        BlockDamageEntry(int damage) {
            this.damage = damage;
            this.lastHitTime = System.currentTimeMillis();
        }
    }
}
