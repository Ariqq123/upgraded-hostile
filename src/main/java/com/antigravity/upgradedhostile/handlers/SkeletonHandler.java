package com.antigravity.upgradedhostile.handlers;

import com.antigravity.upgradedhostile.util.MobUtil;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class SkeletonHandler {

    // Entity ref tracking — no Bukkit.getEntity needed
    private final Map<UUID, Skeleton> modifiedSkeletons = new HashMap<>();
    private final Map<UUID, Boolean> strafeDirection = new HashMap<>();

    private final double strafeSpeed;
    private final double normalSpeed;
    private final double coverSeekRangeSq;
    private final double strafeThresholdSq;

    public SkeletonHandler(FileConfiguration config) {
        this.strafeSpeed = config.getDouble("skeleton.strafe-speed", 0.35);
        this.normalSpeed = config.getDouble("skeleton.normal-speed", 0.25);
        double csr = config.getDouble("skeleton.cover-seek-range", 8.0);
        double st = config.getDouble("skeleton.strafe-bow-distance", 12.0);
        this.coverSeekRangeSq = csr * csr;
        this.strafeThresholdSq = st * st;
    }

    public void handle(Skeleton skeleton) {
        if (!(skeleton.getTarget() instanceof Player target)) {
            if (modifiedSkeletons.containsKey(skeleton.getUniqueId())) {
                resetSkeleton(skeleton);
            }
            return;
        }

        if (!MobUtil.sameWorld(skeleton, target)) return;

        UUID id = skeleton.getUniqueId();
        modifiedSkeletons.put(id, skeleton);

        double distSq = MobUtil.distanceSquared(skeleton, target);

        if (isPlayerAimingBow(target) && distSq < strafeThresholdSq) {
            performStrafe(skeleton, target, id);
        }

        if (MobUtil.isLookingAt(target, skeleton, 0.7) && distSq < coverSeekRangeSq) {
            seekCover(skeleton, target);
        }
    }

    /**
     * Cleanup: check isValid() on stored refs — O(1) per entry, no UUID scan.
     * Also cleans strafeDirection to fix leak #6.
     */
    public void cleanup() {
        Iterator<Map.Entry<UUID, Skeleton>> it = modifiedSkeletons.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Skeleton> entry = it.next();
            if (!entry.getValue().isValid()) {
                strafeDirection.remove(entry.getKey()); // Fix #6: clean both maps
                it.remove();
            }
        }
    }

    private boolean isPlayerAimingBow(Player player) {
        if (player.getActiveItem() == null) return false;
        String itemName = player.getActiveItem().getType().name();
        return (itemName.contains("BOW") || itemName.contains("CROSSBOW"))
                && player.isHandRaised();
    }

    private void performStrafe(Skeleton skeleton, Player target, UUID id) {
        if (Math.random() < 0.1) {
            strafeDirection.put(id, !strafeDirection.getOrDefault(id, true));
        }

        boolean goLeft = strafeDirection.getOrDefault(id, true);
        Vector toPlayer = target.getLocation().toVector()
                .subtract(skeleton.getLocation().toVector()).normalize();

        Vector strafe = goLeft
                ? new Vector(-toPlayer.getZ(), 0, toPlayer.getX())
                : new Vector(toPlayer.getZ(), 0, -toPlayer.getX());

        Location strafeLoc = skeleton.getLocation().clone().add(strafe.multiply(1.5));

        Block ground = strafeLoc.clone().subtract(0, 1, 0).getBlock();
        Block body = strafeLoc.getBlock();
        if (ground.getType().isSolid() && !body.getType().isSolid()) {
            AttributeInstance speedAttr = skeleton.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(strafeSpeed);
            }
            skeleton.getPathfinder().moveTo(strafeLoc);
        }
    }

    private void seekCover(Skeleton skeleton, Player target) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block adjacent = skeleton.getLocation().getBlock().getRelative(face);
            if (adjacent.getType().isSolid()) {
                Location coverSpot = adjacent.getLocation().clone()
                        .add(face.getModX() * 1.5, 0, face.getModZ() * 1.5);

                Block coverGround = coverSpot.clone().subtract(0, 1, 0).getBlock();
                Block coverBody = coverSpot.getBlock();
                if (coverGround.getType().isSolid() && !coverBody.getType().isSolid()) {
                    skeleton.getPathfinder().moveTo(coverSpot);
                    return;
                }
            }
        }
    }

    private void resetSkeleton(Skeleton skeleton) {
        UUID id = skeleton.getUniqueId();
        AttributeInstance speedAttr = skeleton.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(normalSpeed);
        }
        modifiedSkeletons.remove(id);
        strafeDirection.remove(id); // Fix #6
    }
}
