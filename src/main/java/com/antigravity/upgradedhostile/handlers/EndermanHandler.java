package com.antigravity.upgradedhostile.handlers;

import com.antigravity.upgradedhostile.util.MobUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class EndermanHandler {

    // No attribute modifications → no tracking set needed (was leak source)

    private final double flankRangeSq;
    private final double blockPlaceRangeSq;
    private final boolean enableBlockWeaponization;

    public EndermanHandler(FileConfiguration config) {
        double fr = config.getDouble("enderman.flank-range", 10.0);
        double bpr = config.getDouble("enderman.block-place-range", 3.0);
        this.flankRangeSq = fr * fr;
        this.blockPlaceRangeSq = bpr * bpr;
        this.enableBlockWeaponization = config.getBoolean("enderman.enable-block-weaponization", true);
    }

    public void handle(Enderman enderman) {
        if (!(enderman.getTarget() instanceof Player target)) return;
        if (!MobUtil.sameWorld(enderman, target)) return;

        double distSq = MobUtil.distanceSquaredFast(enderman, target);
        double minFlankSq = 3.0 * 3.0;

        if (distSq > minFlankSq && distSq < flankRangeSq) {
            tryFlankTeleport(enderman, target);
        }

        if (enableBlockWeaponization && distSq < blockPlaceRangeSq) {
            tryPlaceBlockToObstruct(enderman, target);
        }
    }

    public void cleanup() {
        // No state to clean — enderman handler is stateless
    }

    private void tryFlankTeleport(Enderman enderman, Player target) {
        if (ThreadLocalRandom.current().nextDouble() > 0.15) return;

        Vector behindPlayer = target.getLocation().getDirection().multiply(-2.0);
        Location behindLoc = target.getLocation().clone().add(behindPlayer);
        behindLoc.setY(target.getLocation().getY());

        Block ground = behindLoc.clone().subtract(0, 1, 0).getBlock();
        Block body = behindLoc.getBlock();
        Block head = behindLoc.clone().add(0, 1, 0).getBlock();

        if (ground.getType().isSolid() && !body.getType().isSolid() && !head.getType().isSolid()) {
            behindLoc.setDirection(target.getLocation().toVector()
                    .subtract(behindLoc.toVector()));
            enderman.teleport(behindLoc);
        }
    }

    private void tryPlaceBlockToObstruct(Enderman enderman, Player target) {
        if (enderman.getCarriedBlock() == null) return;
        if (ThreadLocalRandom.current().nextDouble() > 0.10) return;

        Vector playerDir = target.getLocation().getDirection();
        playerDir.setY(0).normalize();

        Location placeLoc = target.getLocation().clone().add(playerDir.multiply(1.5));
        Block placeBlock = placeLoc.getBlock();

        if (placeBlock.getType() == Material.AIR) {
            Material carriedType = enderman.getCarriedBlock().getMaterial();
            placeBlock.setType(carriedType);
            enderman.setCarriedBlock(null);
        }
    }
}
