package com.antigravity.upgradedhostile;

import com.antigravity.upgradedhostile.handlers.*;
import com.antigravity.upgradedhostile.listeners.BleedListener;
import com.antigravity.upgradedhostile.listeners.DrownedListener;
import com.antigravity.upgradedhostile.listeners.EvolutionListener;
import com.antigravity.upgradedhostile.listeners.RageListener;
import com.antigravity.upgradedhostile.managers.BleedManager;
import com.antigravity.upgradedhostile.managers.EvolutionManager;
import com.antigravity.upgradedhostile.commands.UHostileCommand;
import com.antigravity.upgradedhostile.guis.ControlGUI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

/**
 * UpgradedHostile — A Minecraft plugin for smarter hostile mobs.
 *
 * @author azreyzaako
 */
public class UpgradedHostile extends JavaPlugin {

    private BukkitTask dispatcherTask;
    private BleedManager bleedManager;
    private BukkitTask bleedTask;
    private EvolutionManager evolutionManager;
    private BukkitTask evolutionCleanupTask;
    private BukkitTask evolutionSaveTask;
    private boolean debugMode;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // Initialize Persistent Managers — read rage threshold from config early
        double rageThreshold = getConfig().getDouble("territorial-rage.threshold", 0.6);
        this.evolutionManager = new EvolutionManager(rageThreshold);
        File evoFile = new File(getDataFolder(), "evolution_data.yml");
        this.evolutionManager.load(evoFile);

        // Cleanup evolution data every 30 minutes (36000 ticks)
        this.evolutionCleanupTask = getServer().getScheduler().runTaskTimer(this, evolutionManager::cleanup, 36000L, 36000L);
        // Periodic Save every 10 minutes (12000 ticks)
        this.evolutionSaveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> evolutionManager.save(evoFile), 12000L, 12000L);

        // Register commands
        getCommand("uhostile").setExecutor(new UHostileCommand(this));
        
        reloadPlugin();
    }

    public void reloadPlugin() {
        // Cleanup old tasks
        if (dispatcherTask != null) dispatcherTask.cancel();
        if (bleedTask != null) bleedTask.cancel();
        
        // Unregister old listeners to prevent duplicates
        HandlerList.unregisterAll(this);
        
        reloadConfig();
        FileConfiguration config = getConfig();
        
        this.debugMode = config.getBoolean("general.debug", false);

        // Initialize Managers
        this.bleedManager = new BleedManager();
        this.bleedTask = this.bleedManager.runTaskTimer(this, 20L, 5L);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new BleedListener(this, bleedManager, config), this);
        getServer().getPluginManager().registerEvents(new EvolutionListener(evolutionManager, this), this);
        getServer().getPluginManager().registerEvents(new ControlGUI(this), this);
        // DrownedListener will be registered after handler creation below

        boolean zombieEnabled = config.getBoolean("zombie.enabled", true);
        boolean creeperEnabled = config.getBoolean("creeper.enabled", true);
        boolean skeletonEnabled = config.getBoolean("skeleton.enabled", true);
        boolean spiderEnabled = config.getBoolean("spider.enabled", true);
        boolean phantomEnabled = config.getBoolean("phantom.enabled", true);
        boolean endermanEnabled = config.getBoolean("enderman.enabled", true);
        boolean witchEnabled = config.getBoolean("witch.enabled", true);
        boolean drownedEnabled = config.getBoolean("drowned.enabled", true);

        // Territorial Rage config
        boolean rageEnabled = config.getBoolean("territorial-rage.enabled", true);
        boolean rageAuraEnabled = config.getBoolean("territorial-rage.aura-particles", true);
        double rageDamageMultiplier = config.getDouble("territorial-rage.damage-multiplier", 1.25);
        double rageRevengeRange = config.getDouble("territorial-rage.revenge-range", 24.0);

        // Register Rage Listener
        getServer().getPluginManager().registerEvents(
                new RageListener(evolutionManager, rageDamageMultiplier, rageRevengeRange, rageEnabled), this);

        // Create handlers
        ZombieHandler zombieHandler = new ZombieHandler(this, config, bleedManager, evolutionManager);
        CreeperHandler creeperHandler = new CreeperHandler(config, evolutionManager);
        SkeletonHandler skeletonHandler = new SkeletonHandler(this, config, evolutionManager);
        SpiderHandler spiderHandler = new SpiderHandler(this, config, evolutionManager);
        PhantomHandler phantomHandler = new PhantomHandler(config);
        EndermanHandler endermanHandler = new EndermanHandler(config, evolutionManager);
        WitchHandler witchHandler = new WitchHandler(config);
        DrownedHandler drownedHandler = new DrownedHandler(this, config, evolutionManager);

        // Register Drowned Listener
        getServer().getPluginManager().registerEvents(new DrownedListener(drownedHandler), this);

        // Single consolidated dispatcher — one entity scan for all mob types
        MobAIDispatcher dispatcher = new MobAIDispatcher(
                this, zombieHandler, creeperHandler, skeletonHandler,
                spiderHandler, phantomHandler, endermanHandler, witchHandler, drownedHandler,
                evolutionManager,
                zombieEnabled, creeperEnabled, skeletonEnabled,
                spiderEnabled, phantomEnabled, endermanEnabled, witchEnabled, drownedEnabled,
                rageAuraEnabled && rageEnabled
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
        if (drownedEnabled) enabledCount++;

        getLogger().info("UpgradedHostile has been enabled! (" + enabledCount + " mob AIs active)");
    }

    @Override
    public void onDisable() {
        if (dispatcherTask != null) {
            dispatcherTask.cancel();
        }
        if (bleedTask != null) {
            bleedTask.cancel();
        }
        if (evolutionCleanupTask != null) {
            evolutionCleanupTask.cancel();
        }
        if (evolutionSaveTask != null) {
            evolutionSaveTask.cancel();
        }
        if (evolutionManager != null) {
            evolutionManager.save(new File(getDataFolder(), "evolution_data.yml"));
        }
        getLogger().info("UpgradedHostile has been disabled!");
    }

    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
