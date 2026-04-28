package com.antigravity.upgradedhostile;

import com.antigravity.upgradedhostile.handlers.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Single dispatcher task that iterates all living entities ONCE per tick cycle
 * and delegates to individual handlers by entity type.
 *
 * This replaces 7 separate tasks that each did their own full entity scan.
 * Performance fix #1: O(N) total instead of O(7N).
 *
 * Runs every 5 ticks. Handlers that need slower rates use internal counters.
 */
public class MobAIDispatcher extends BukkitRunnable {

    private final UpgradedHostile plugin;
    private final ZombieHandler zombieHandler;
    private final CreeperHandler creeperHandler;
    private final SkeletonHandler skeletonHandler;
    private final SpiderHandler spiderHandler;
    private final PhantomHandler phantomHandler;
    private final EndermanHandler endermanHandler;
    private final WitchHandler witchHandler;

    private final boolean zombieEnabled;
    private final boolean creeperEnabled;
    private final boolean skeletonEnabled;
    private final boolean spiderEnabled;
    private final boolean phantomEnabled;
    private final boolean endermanEnabled;
    private final boolean witchEnabled;

    // Tick counter for handlers that run at slower rates
    private int tickCount = 0;
    private static final int SLOW_RATE = 2; // Every 2nd cycle = every 10 ticks

    // Cleanup amortization — don't clean every tick
    private int cleanupCounter = 0;
    private static final int CLEANUP_RATE = 10; // Every 10th cycle = every 50 ticks

    public MobAIDispatcher(
            UpgradedHostile plugin,
            ZombieHandler zombieHandler,
            CreeperHandler creeperHandler,
            SkeletonHandler skeletonHandler,
            SpiderHandler spiderHandler,
            PhantomHandler phantomHandler,
            EndermanHandler endermanHandler,
            WitchHandler witchHandler,
            boolean zombieEnabled,
            boolean creeperEnabled,
            boolean skeletonEnabled,
            boolean spiderEnabled,
            boolean phantomEnabled,
            boolean endermanEnabled,
            boolean witchEnabled
    ) {
        this.plugin = plugin;
        this.zombieHandler = zombieHandler;
        this.creeperHandler = creeperHandler;
        this.skeletonHandler = skeletonHandler;
        this.spiderHandler = spiderHandler;
        this.phantomHandler = phantomHandler;
        this.endermanHandler = endermanHandler;
        this.witchHandler = witchHandler;
        this.zombieEnabled = zombieEnabled;
        this.creeperEnabled = creeperEnabled;
        this.skeletonEnabled = skeletonEnabled;
        this.spiderEnabled = spiderEnabled;
        this.phantomEnabled = phantomEnabled;
        this.endermanEnabled = endermanEnabled;
        this.witchEnabled = witchEnabled;
    }

    @Override
    public void run() {
        long startTime = System.nanoTime();
        
        tickCount++;
        boolean isSlowTick = (tickCount % SLOW_RATE == 0);

        // Phantom needs per-tick setup for coordination grouping
        if (phantomEnabled) {
            phantomHandler.beginTick();
        }

        // Single pass over all living entities across all worlds
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {

                // Dispatch by type — most common mob types first for branch prediction
                if (zombieEnabled && isSlowTick && entity instanceof Zombie zombie) {
                    zombieHandler.handle(zombie);
                } else if (creeperEnabled && entity instanceof Creeper creeper) {
                    creeperHandler.handle(creeper);
                } else if (skeletonEnabled && entity instanceof Skeleton skeleton) {
                    skeletonHandler.handle(skeleton);
                } else if (spiderEnabled && isSlowTick && entity instanceof Spider spider) {
                    spiderHandler.handle(spider);
                } else if (phantomEnabled && entity instanceof Phantom phantom) {
                    phantomHandler.handle(phantom);
                } else if (endermanEnabled && isSlowTick && entity instanceof Enderman enderman) {
                    endermanHandler.handle(enderman);
                } else if (witchEnabled && isSlowTick && entity instanceof Witch witch) {
                    witchHandler.handle(witch);
                }
            }
        }

        // Phantom post-processing for coordinated attacks
        if (phantomEnabled) {
            phantomHandler.endTick();
        }

        // Amortized cleanup — not every tick
        if (++cleanupCounter >= CLEANUP_RATE) {
            cleanupCounter = 0;
            if (zombieEnabled) zombieHandler.cleanup();
            if (creeperEnabled) creeperHandler.cleanup();
            if (skeletonEnabled) skeletonHandler.cleanup();
            if (spiderEnabled) spiderHandler.cleanup();
            if (phantomEnabled) phantomHandler.cleanup();
            if (endermanEnabled) endermanHandler.cleanup();
            if (witchEnabled) witchHandler.cleanup();
        }

        long endTime = System.nanoTime();
        if (tickCount % 20 == 0) { // Log every 20 executions (approx 5 seconds)
            plugin.debug("MobAIDispatcher cycle completed in " + ((endTime - startTime) / 1_000_000.0) + "ms");
        }
    }
}
