package com.antigravity.upgradedhostile;

import com.antigravity.upgradedhostile.handlers.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class UpgradedHostile extends JavaPlugin {

    private BukkitTask dispatcherTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        boolean zombieEnabled = config.getBoolean("zombie.enabled", true);
        boolean creeperEnabled = config.getBoolean("creeper.enabled", true);
        boolean skeletonEnabled = config.getBoolean("skeleton.enabled", true);
        boolean spiderEnabled = config.getBoolean("spider.enabled", true);
        boolean phantomEnabled = config.getBoolean("phantom.enabled", true);
        boolean endermanEnabled = config.getBoolean("enderman.enabled", true);
        boolean witchEnabled = config.getBoolean("witch.enabled", true);

        // Create handlers
        ZombieHandler zombieHandler = new ZombieHandler(this, config);
        CreeperHandler creeperHandler = new CreeperHandler(config);
        SkeletonHandler skeletonHandler = new SkeletonHandler(config);
        SpiderHandler spiderHandler = new SpiderHandler(this, config);
        PhantomHandler phantomHandler = new PhantomHandler(config);
        EndermanHandler endermanHandler = new EndermanHandler(config);
        WitchHandler witchHandler = new WitchHandler(config);

        // Single consolidated dispatcher — one entity scan for all mob types
        MobAIDispatcher dispatcher = new MobAIDispatcher(
                zombieHandler, creeperHandler, skeletonHandler,
                spiderHandler, phantomHandler, endermanHandler, witchHandler,
                zombieEnabled, creeperEnabled, skeletonEnabled,
                spiderEnabled, phantomEnabled, endermanEnabled, witchEnabled
        );

        // Run every 5 ticks (0.25s) — internal rate limiting handles slower mobs
        dispatcherTask = dispatcher.runTaskTimer(this, 20L, 5L);

        int enabledCount = 0;
        if (zombieEnabled) enabledCount++;
        if (creeperEnabled) enabledCount++;
        if (skeletonEnabled) enabledCount++;
        if (spiderEnabled) enabledCount++;
        if (phantomEnabled) enabledCount++;
        if (endermanEnabled) enabledCount++;
        if (witchEnabled) enabledCount++;

        getLogger().info("UpgradedHostile has been enabled! (" + enabledCount + " mob AIs active)");
    }

    @Override
    public void onDisable() {
        if (dispatcherTask != null) {
            dispatcherTask.cancel();
        }
        getLogger().info("UpgradedHostile has been disabled!");
    }
}
