package com.antigravity.upgradedhostile.handlers;

import com.antigravity.upgradedhostile.util.MobUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Witch;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class WitchHandler {

    private final Map<UUID, Witch> modifiedWitches = new HashMap<>();

    private final double kiteDistanceSq;
    private final double kiteSpeed;
    private final double normalSpeed;
    private final double healThreshold;

    public WitchHandler(FileConfiguration config) {
        double kd = config.getDouble("witch.kite-distance", 8.0);
        this.kiteDistanceSq = kd * kd;
        this.kiteSpeed = config.getDouble("witch.kite-speed", 0.35);
        this.normalSpeed = config.getDouble("witch.normal-speed", 0.25);
        this.healThreshold = config.getDouble("witch.heal-health-threshold", 13.0);
    }

    public void handle(Witch witch) {
        if (!(witch.getTarget() instanceof Player target)) {
            if (modifiedWitches.containsKey(witch.getUniqueId())) {
                resetWitch(witch);
            }
            return;
        }

        if (!MobUtil.sameWorld(witch, target)) return;

        modifiedWitches.put(witch.getUniqueId(), witch);
        double distSq = MobUtil.distanceSquared(witch, target);

        if (distSq < kiteDistanceSq) {
            performKite(witch, target);
        }

        if (witch.getHealth() < healThreshold) {
            prioritizeSelfHeal(witch);
        }
    }

    public void cleanup() {
        Iterator<Map.Entry<UUID, Witch>> it = modifiedWitches.entrySet().iterator();
        while (it.hasNext()) {
            Witch witch = it.next().getValue();
            if (!witch.isValid()) {
                it.remove();
            }
        }
    }

    private void performKite(Witch witch, Player target) {
        org.bukkit.util.Vector awayFromPlayer = witch.getLocation().toVector()
                .subtract(target.getLocation().toVector()).normalize();

        org.bukkit.Location retreatLoc = witch.getLocation().clone()
                .add(awayFromPlayer.multiply(3.0));

        Block ground = retreatLoc.clone().subtract(0, 1, 0).getBlock();
        Block body = retreatLoc.getBlock();

        if (ground.getType().isSolid() && !body.getType().isSolid()) {
            AttributeInstance speedAttr = witch.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(kiteSpeed);
            }
            witch.getPathfinder().moveTo(retreatLoc);
        }
    }

    private void prioritizeSelfHeal(Witch witch) {
        if (!witch.hasPotionEffect(PotionEffectType.REGENERATION)) {
            witch.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, 60, 0, false, false
            ));
        }

        if (witch.getFireTicks() > 0 && !witch.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            witch.addPotionEffect(new PotionEffect(
                    PotionEffectType.FIRE_RESISTANCE, 200, 0, false, false
            ));
        }
    }

    private void resetWitch(Witch witch) {
        AttributeInstance speedAttr = witch.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(normalSpeed);
        }
        modifiedWitches.remove(witch.getUniqueId());
    }
}
