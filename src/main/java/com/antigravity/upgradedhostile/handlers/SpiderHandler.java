package com.antigravity.upgradedhostile.handlers;

import com.antigravity.upgradedhostile.util.MobUtil;
import com.antigravity.upgradedhostile.managers.EvolutionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SpiderHandler {

    private final JavaPlugin plugin;
    private final EvolutionManager evolutionManager;

    // Fix #3: Track locations with pending web cleanup tasks to avoid scheduling duplicates
    private final Set<Location> pendingWebCleanups = new HashSet<>();

    // Offensive web cooldown per-spider (Rage mode only)
    private final Map<UUID, Long> offensiveWebCooldowns = new HashMap<>();
    private static final long OFFENSIVE_WEB_COOLDOWN_MS = 5_000L;

    private final double leapRangeSq;
    private final double leapVelocity;
    private final double ceilingDropRange;
    private final double webHealthThreshold;
    private final boolean enableWebTrap;

    public SpiderHandler(JavaPlugin plugin, FileConfiguration config, EvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.evolutionManager = evolutionManager;
        double lr = config.getDouble("spider.leap-range", 6.0);
        this.leapRangeSq = lr * lr;
        this.leapVelocity = config.getDouble("spider.leap-velocity", 1.2);
        this.ceilingDropRange = config.getDouble("spider.ceiling-drop-range", 4.0);
        this.webHealthThreshold = config.getDouble("spider.web-health-threshold", 8.0);
        this.enableWebTrap = config.getBoolean("spider.enable-web-trap", true);
    }

    public void handle(Spider spider) {
        if (!(spider.getTarget() instanceof Player target)) return;
        if (!MobUtil.sameWorld(spider, target)) return;

        double distSq = MobUtil.distanceSquaredFast(spider, target);

        // Behavior 1: Web trap when fleeing at low health
        if (enableWebTrap && spider.getHealth() < webHealthThreshold) {
            tryPlaceWeb(spider);
        }

        // Behavior 2: Ceiling ambush at night
        long worldTime = spider.getWorld().getTime();
        boolean isNight = worldTime >= 13000 && worldTime <= 23000;
        if (isNight && isOnCeiling(spider) && isPlayerBelow(spider, target, ceilingDropRange)) {
            performCeilingDrop(spider, target);
        }

        // Behavior 3: Leap attack when player isn't looking
        double minLeapSq = 2.0 * 2.0;
        if (distSq <= leapRangeSq && distSq > minLeapSq && !MobUtil.isLookingAt(target, spider, 0.7)) {
            performLeap(spider, target);
        }

        // Behavior 4: Offensive web trap in Rage chunks (blocks escape route)
        if (enableWebTrap && evolutionManager.isRaging(spider.getLocation().getChunk())) {
            attemptOffensiveWeb(spider, target);
        }
    }

    /**
     * Fix #3: No-op cleanup — pendingWebCleanups is self-cleaning via scheduled tasks.
     * We just verify stale entries (blocks that were broken by players before the task ran).
     */
    public void cleanup() {
        Iterator<Location> it = pendingWebCleanups.iterator();
        while (it.hasNext()) {
            Location loc = it.next();
            if (loc.getBlock().getType() != Material.COBWEB) {
                it.remove();
            }
        }
        // Clean offensive web cooldowns for dead spiders
        long now = System.currentTimeMillis();
        offensiveWebCooldowns.entrySet().removeIf(e -> now - e.getValue() > OFFENSIVE_WEB_COOLDOWN_MS * 2);
    }

    private void tryPlaceWeb(Spider spider) {
        Block footBlock = spider.getLocation().getBlock();
        Location loc = footBlock.getLocation();

        // Fix #3: Skip if already pending cleanup for this location
        if (footBlock.getType() != Material.AIR || pendingWebCleanups.contains(loc)) {
            return;
        }

        footBlock.setType(Material.COBWEB);
        pendingWebCleanups.add(loc);

        // Fix #3: Use cached plugin reference instead of string lookup
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (footBlock.getType() == Material.COBWEB) {
                footBlock.setType(Material.AIR);
            }
            pendingWebCleanups.remove(loc);
        }, 100L);
    }

    private boolean isOnCeiling(Spider spider) {
        Block above = spider.getLocation().getBlock().getRelative(BlockFace.UP);
        Block below = spider.getLocation().getBlock().getRelative(BlockFace.DOWN);
        return above.getType().isSolid() && !below.getType().isSolid();
    }

    private boolean isPlayerBelow(Spider spider, Player player, double range) {
        double dx = spider.getLocation().getX() - player.getLocation().getX();
        double dz = spider.getLocation().getZ() - player.getLocation().getZ();
        double horizontalDistSq = dx * dx + dz * dz;
        double verticalDist = spider.getLocation().getY() - player.getLocation().getY();
        return horizontalDistSq < 4.0 && verticalDist > 0 && verticalDist < range;
    }

    private void performCeilingDrop(Spider spider, Player target) {
        Vector toPlayer = target.getLocation().toVector()
                .subtract(spider.getLocation().toVector()).normalize();
        toPlayer.setY(0);
        spider.setVelocity(toPlayer.multiply(0.3));
    }

    private void performLeap(Spider spider, Player target) {
        if (!spider.isOnGround()) return;

        Vector toPlayer = target.getLocation().toVector()
                .subtract(spider.getLocation().toVector()).normalize();
        Vector leapVec = toPlayer.multiply(leapVelocity);
        leapVec.setY(0.5);
        spider.setVelocity(leapVec);
    }

    /**
     * Offensive web trap: In Rage chunks, spiders place webs in the player's escape path
     * (the direction the player is facing/running toward) to block retreat.
     * Gated by a 5s per-spider cooldown.
     */
    private void attemptOffensiveWeb(Spider spider, Player target) {
        UUID id = spider.getUniqueId();
        long now = System.currentTimeMillis();
        if (now - offensiveWebCooldowns.getOrDefault(id, 0L) < OFFENSIVE_WEB_COOLDOWN_MS) return;
        offensiveWebCooldowns.put(id, now);

        // Place web 1.5 blocks ahead in the player's facing direction
        Vector escapeDir = target.getLocation().getDirection().setY(0).normalize();
        Location webLoc = target.getLocation().clone().add(escapeDir.multiply(1.5));
        Block webBlock = webLoc.getBlock();

        if (webBlock.getType() != Material.AIR || pendingWebCleanups.contains(webLoc)) return;

        webBlock.setType(Material.COBWEB);
        pendingWebCleanups.add(webLoc);

        // Auto-remove after 3 seconds (shorter than defensive webs — 60 ticks)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (webBlock.getType() == Material.COBWEB) {
                webBlock.setType(Material.AIR);
            }
            pendingWebCleanups.remove(webLoc);
        }, 60L);
    }
}
