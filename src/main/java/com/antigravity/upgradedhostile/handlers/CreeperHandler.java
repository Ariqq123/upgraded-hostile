package com.antigravity.upgradedhostile.handlers;

import com.antigravity.upgradedhostile.util.MobUtil;
import com.antigravity.upgradedhostile.managers.EvolutionManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class CreeperHandler {

    // Store entity references directly — isValid() is O(1), no UUID scan needed
    private final Map<UUID, Creeper> modifiedCreepers = new HashMap<>();

    private final double creeperSlowSpeed;
    private final double creeperFastSpeed;
    private final double creeperNormalSpeed;
    private final int surpriseFuseTicks;
    private final int normalFuseTicks;
    private final int rageFuseTicks;       // Even shorter fuse for Rage chunks
    private final double surpriseRangeSq;
    private final double lookingAtThreshold;
    private final EvolutionManager evolutionManager;

    public CreeperHandler(FileConfiguration config, EvolutionManager evolutionManager) {
        this.evolutionManager = evolutionManager;
        this.creeperSlowSpeed = config.getDouble("creeper.slow-speed", 0.2);
        this.creeperFastSpeed = config.getDouble("creeper.fast-speed", 0.4);
        this.creeperNormalSpeed = config.getDouble("creeper.normal-speed", 0.25);
        this.surpriseFuseTicks = config.getInt("creeper.surprise-fuse-ticks", 10);
        this.normalFuseTicks = config.getInt("creeper.normal-fuse-ticks", 30);
        this.rageFuseTicks = config.getInt("creeper.rage-fuse-ticks", 5);
        double sr = config.getDouble("creeper.surprise-range", 2.5);
        this.surpriseRangeSq = sr * sr;
        this.lookingAtThreshold = config.getDouble("creeper.looking-at-threshold", 0.7);
    }

    public void handle(Creeper creeper) {
        if (creeper.getTarget() instanceof Player target) {
            if (!MobUtil.sameWorld(creeper, target)) return;

            modifiedCreepers.put(creeper.getUniqueId(), creeper);

            AttributeInstance speedAttr = creeper.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr == null) return;

            boolean isRaging = evolutionManager.isRaging(creeper.getLocation().getChunk());
            boolean playerLooking = MobUtil.isLookingAt(target, creeper, lookingAtThreshold);

            if (playerLooking) {
                // Player sees the creeper — slow down and use normal fuse
                speedAttr.setBaseValue(creeperSlowSpeed);
                creeper.setMaxFuseTicks(normalFuseTicks);
            } else {
                // Player is NOT looking — stalk fast and trigger surprise
                speedAttr.setBaseValue(creeperFastSpeed);
                // Rage creepers use an even shorter fuse
                creeper.setMaxFuseTicks(isRaging ? rageFuseTicks : surpriseFuseTicks);
                double distSq = MobUtil.distanceSquaredFast(creeper, target);
                if (distSq < surpriseRangeSq && !creeper.isIgnited()) {
                    creeper.ignite();
                }
            }
        } else if (modifiedCreepers.containsKey(creeper.getUniqueId())) {
            resetCreeper(creeper);
        }
    }

    public void cleanup() {
        Iterator<Map.Entry<UUID, Creeper>> it = modifiedCreepers.entrySet().iterator();
        while (it.hasNext()) {
            Creeper creeper = it.next().getValue();
            if (!creeper.isValid()) {
                it.remove();
            }
        }
    }

    private void resetCreeper(Creeper creeper) {
        AttributeInstance speedAttr = creeper.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(creeperNormalSpeed);
        }
        creeper.setMaxFuseTicks(normalFuseTicks);
        modifiedCreepers.remove(creeper.getUniqueId());
    }
}
