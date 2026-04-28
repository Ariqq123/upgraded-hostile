package com.antigravity.upgradedhostile.handlers;

import com.antigravity.upgradedhostile.util.MobUtil;
import com.antigravity.upgradedhostile.UpgradedHostile;
import com.antigravity.upgradedhostile.managers.BleedManager;
import com.antigravity.upgradedhostile.managers.EvolutionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ZombieHandler {

    private final JavaPlugin plugin;
    private final BleedManager bleedManager;
    private final EvolutionManager evolutionManager;
    private final Map<Location, BlockDamageEntry> blockDamage = new HashMap<>();
    private final int maxDamage;
    private final double detectionRangeSq;
    private final double minRangeSq;
    private final long staleTimeoutMs;
    private final double smellBloodRangeSq;
    private final double smellBloodSpeed;
    private final double normalSpeed;
    private final boolean canPickupBlocks;
    private final boolean canTower;
    private final boolean canBridge;
    private final boolean canSnuffTorches;

    // Run cleanup only every N calls to amortize cost
    private int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 20;

    // Tower cooldown: prevent spam-jumping
    private final Map<UUID, Long> towerCooldowns = new HashMap<>();
    private static final long TOWER_COOLDOWN_MS = 1500; // 1.5 seconds between towers

    // Torch snuff cooldown: 10 second gap between scans per mob
    private final Map<UUID, Long> torchSnuffCooldowns = new HashMap<>();
    private static final long TORCH_SNUFF_COOLDOWN_MS = 10_000L;

    public ZombieHandler(JavaPlugin plugin, FileConfiguration config, BleedManager bleedManager, EvolutionManager evolutionManager) {
        this.plugin = plugin;
        this.bleedManager = bleedManager;
        this.evolutionManager = evolutionManager;
        this.maxDamage = config.getInt("zombie.break-hits", 10);
        double dr = config.getDouble("zombie.detection-range", 4.0);
        double mr = config.getDouble("zombie.min-range", 1.2);
        this.detectionRangeSq = dr * dr;
        this.minRangeSq = mr * mr;
        this.staleTimeoutMs = config.getLong("zombie.stale-timeout-ms", 10000);
        double sbr = config.getDouble("zombie.blood-smell-range", 64.0);
        this.smellBloodRangeSq = sbr * sbr;
        this.smellBloodSpeed = config.getDouble("zombie.blood-smell-speed", 0.35);
        this.normalSpeed = config.getDouble("zombie.normal-speed", 0.23);
        this.canPickupBlocks = config.getBoolean("zombie.can-pickup-blocks", true);
        this.canTower = config.getBoolean("zombie.can-tower", true);
        this.canBridge = config.getBoolean("zombie.can-bridge", true);
        this.canSnuffTorches = config.getBoolean("zombie.can-snuff-torches", true);
    }

    public void handle(Zombie zombie) {
        if (!(zombie.getTarget() instanceof Player target)) {
            // Target acquisition: smell blood
            acquireBleedingTarget(zombie);
            
            // Sabotage: Torch snuffing if idle
            if (zombie.getTarget() == null && canSnuffTorches) {
                attemptTorchSnuff(zombie);
            }
            return;
        }

        if (!MobUtil.sameWorld(zombie, target)) return;

        double distSq = MobUtil.distanceSquaredFast(zombie, target);

        // Apply blood lust speed boost if target is bleeding
        if (bleedManager.isBleeding(target.getUniqueId())) {
            AttributeInstance speedAttr = zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) speedAttr.setBaseValue(smellBloodSpeed);
        } else {
            AttributeInstance speedAttr = zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) speedAttr.setBaseValue(normalSpeed);
        }

        if (distSq < detectionRangeSq && distSq > minRangeSq) {
            attemptBreakBlock(zombie, target);
        }

        // Builder AI
        ItemStack offHand = zombie.getEquipment().getItemInOffHand();
        if (offHand != null && offHand.getType().isBlock()) {
            double dx = target.getX() - zombie.getX();
            double dz = target.getZ() - zombie.getZ();
            double distSq2D = dx * dx + dz * dz;
            double diffY = target.getY() - zombie.getY();

        double evoFactor = evolutionManager.getEvolutionFactor(zombie.getLocation().getChunk());
            // Smarter zombies (higher evolution) use builder AI more consistently
            double buildChance = 0.3 + (evoFactor * 0.7);

            if (canTower && diffY > 1.5 && distSq2D < 4.0 && ThreadLocalRandom.current().nextDouble() < buildChance) {
                attemptTower(zombie);
            } else if (canBridge && distSq2D > 1.0 && ThreadLocalRandom.current().nextDouble() < buildChance) {
                attemptBridge(zombie, target);
            }
        }
    }

    private void acquireBleedingTarget(Zombie zombie) {
        // Simple scan: check online players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!MobUtil.sameWorld(zombie, player)) continue;
            if (bleedManager.isBleeding(player.getUniqueId())) {
                if (MobUtil.distanceSquared(zombie, player) < smellBloodRangeSq) {
                    zombie.setTarget(player);
                    if (plugin instanceof UpgradedHostile) {
                        ((UpgradedHostile) plugin).debug("Zombie at " + zombie.getLocation().getBlockX() + ", " + zombie.getLocation().getBlockZ() + " acquired bleeding target " + player.getName());
                    }
                    return;
                }
            }
        }
    }

    private void attemptTower(Zombie zombie) {
        long now = System.currentTimeMillis();
        Long lastTower = towerCooldowns.get(zombie.getUniqueId());
        if (lastTower != null && now - lastTower < TOWER_COOLDOWN_MS) return;

        if (zombie.getVelocity().getY() < 0.1) {
            towerCooldowns.put(zombie.getUniqueId(), now);
            zombie.setVelocity(zombie.getVelocity().setY(0.42));
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!zombie.isValid()) return;
                ItemStack offHand = zombie.getEquipment().getItemInOffHand();
                if (offHand == null || !offHand.getType().isBlock()) return;

                Block placeBlock = zombie.getLocation().getBlock();
                if (placeBlock.getType().isAir()) {
                    placeBlock.setType(offHand.getType());
                    zombie.getEquipment().setItemInOffHand(null);
                    zombie.getWorld().playSound(placeBlock.getLocation(), placeBlock.getBlockData().getSoundGroup().getPlaceSound(), 1.0f, 1.0f);
                    
                    if (plugin instanceof UpgradedHostile) {
                        ((UpgradedHostile) plugin).debug("Zombie towered up at " + placeBlock.getLocation());
                    }
                }
            }, 8L);
        }
    }

    private void attemptBridge(Zombie zombie, Player target) {
        Vector dir = target.getLocation().toVector().subtract(zombie.getLocation().toVector()).setY(0);
        if (dir.lengthSquared() < 0.1) return;
        dir.normalize();

        Location gapLoc = zombie.getLocation().clone().add(dir).subtract(0, 1, 0);
        Block gapBlock = gapLoc.getBlock();
        Block belowGap = gapBlock.getRelative(BlockFace.DOWN);

        if (gapBlock.getType().isAir() && belowGap.getType().isAir() && zombie.getLocation().clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
            ItemStack offHand = zombie.getEquipment().getItemInOffHand();
            if (offHand == null || !offHand.getType().isBlock()) return;

            gapBlock.setType(offHand.getType());
            zombie.getEquipment().setItemInOffHand(null);
            zombie.getWorld().playSound(gapLoc, gapBlock.getBlockData().getSoundGroup().getPlaceSound(), 1.0f, 1.0f);
            
            if (plugin instanceof UpgradedHostile) {
                ((UpgradedHostile) plugin).debug("Zombie bridged gap at " + gapLoc);
            }
        }
    }

    private void attemptTorchSnuff(Zombie zombie) {
        UUID id = zombie.getUniqueId();
        long now = System.currentTimeMillis();
        if (now - torchSnuffCooldowns.getOrDefault(id, 0L) < TORCH_SNUFF_COOLDOWN_MS) return;
        torchSnuffCooldowns.put(id, now);

        Location loc = zombie.getLocation();
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    if (b.getType() == Material.TORCH || b.getType() == Material.WALL_TORCH) {
                        if (zombie.getLocation().distanceSquared(b.getLocation()) < 2.25) { // 1.5 blocks
                            b.breakNaturally();
                            zombie.swingMainHand();
                            b.getWorld().playSound(b.getLocation(), b.getBlockData().getSoundGroup().getBreakSound(), 1.0f, 1.0f);
                        } else {
                            zombie.getPathfinder().moveTo(b.getLocation());
                        }
                        return; // Only target one torch at a time
                    }
                }
            }
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

        // Clean tower cooldowns for dead zombies
        towerCooldowns.entrySet().removeIf(e -> now - e.getValue() > TOWER_COOLDOWN_MS * 2);
        // Clean torch-snuff cooldowns
        torchSnuffCooldowns.entrySet().removeIf(e -> now - e.getValue() > TORCH_SNUFF_COOLDOWN_MS * 2);
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

        damageBlock(block, zombie);
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

    private void damageBlock(Block block, Zombie zombie) {
        Location loc = block.getLocation();
        BlockDamageEntry entry = blockDamage.getOrDefault(loc, new BlockDamageEntry(0));
        entry.damage++;
        entry.lastHitTime = System.currentTimeMillis();

        if (entry.damage >= maxDamage) {
            Material type = block.getType();
            ItemStack offHand = zombie.getEquipment().getItemInOffHand();
            
            if (canPickupBlocks && (offHand == null || offHand.getType().isAir())) {
                zombie.getEquipment().setItemInOffHand(new ItemStack(type));
                block.setType(Material.AIR);
                if (plugin instanceof UpgradedHostile) {
                    ((UpgradedHostile) plugin).debug("Zombie picked up block " + type.name());
                }
            } else {
                block.breakNaturally();
            }
            
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
