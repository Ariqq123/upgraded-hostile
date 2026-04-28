package com.antigravity.upgradedhostile.handlers;

import com.antigravity.upgradedhostile.util.MobUtil;
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
    private final double surpriseRangeSq;
    private final double lookingAtThreshold;

    public CreeperHandler(FileConfiguration config) {
        this.creeperSlowSpeed = config.getDouble("creeper.slow-speed", 0.2);
        this.creeperFastSpeed = config.getDouble("creeper.fast-speed", 0.4);
        this.creeperNormalSpeed = config.getDouble("creeper.normal-speed", 0.25);
        this.surpriseFuseTicks = config.getInt("creeper.surprise-fuse-ticks", 10);
        this.normalFuseTicks = config.getInt("creeper.normal-fuse-ticks", 30);
        double sr = config.getDouble("creeper.surprise-range", 2.5);
        this.surpriseRangeSq = sr * sr;
        this.lookingAtThreshold = config.getDouble("creeper.looking-at-threshold", 0.7);
    }

    public void handle(Creeper creeper) {
        if (creeper.getTarget() instanceof Player target) {
            if (!MobUtil.sameWorld(creeper, target)) return;

            creeper.setMaxFuseTicks(surpriseFuseTicks);
            modifiedCreepers.put(creeper.getUniqueId(), creeper);

            AttributeInstance speedAttr = creeper.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr == null) return;

            if (MobUtil.isLookingAt(target, creeper, lookingAtThreshold)) {
                speedAttr.setBaseValue(creeperSlowSpeed);
            } else {
                speedAttr.setBaseValue(creeperFastSpeed);
                double distSq = MobUtil.distanceSquared(creeper, target);
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
