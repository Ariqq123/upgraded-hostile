package com.antigravity.upgradedhostile.managers;

import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class EvolutionManager {

    private final Map<ChunkKey, EvolutionData> chunkData = new ConcurrentHashMap<>();
    private static final long FLUSH_THRESHOLD_MS = 24 * 60 * 60 * 1000L; // 24 hours
    private static final int MAX_KILLS_FOR_LEVEL = 100; // 100 kills = 1.0 factor

    public void recordKill(Chunk chunk) {
        ChunkKey key = new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        chunkData.compute(key, (k, data) -> {
            if (data == null) {
                return new EvolutionData(1, System.currentTimeMillis());
            }
            data.kills++;
            data.lastKillTime = System.currentTimeMillis();
            return data;
        });
    }

    public double getEvolutionFactor(Chunk chunk) {
        ChunkKey key = new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        EvolutionData data = chunkData.get(key);
        if (data == null) return 0.0;
        
        return Math.min(1.0, (double) data.kills / MAX_KILLS_FOR_LEVEL);
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        chunkData.values().removeIf(data -> now - data.lastKillTime > FLUSH_THRESHOLD_MS);
    }

    public void load(File file) {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("data");
        if (section == null) return;

        long now = System.currentTimeMillis();
        for (String keyStr : section.getKeys(false)) {
            String[] parts = keyStr.split(":");
            if (parts.length != 3) continue;

            int kills = section.getInt(keyStr + ".kills");
            long lastKillTime = section.getLong(keyStr + ".time");

            if (now - lastKillTime < FLUSH_THRESHOLD_MS) {
                chunkData.put(new ChunkKey(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2])), 
                              new EvolutionData(kills, lastKillTime));
            }
        }
    }

    public void save(File file) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection section = config.createSection("data");

        for (Map.Entry<ChunkKey, EvolutionData> entry : chunkData.entrySet()) {
            ChunkKey key = entry.getKey();
            EvolutionData data = entry.getValue();
            String keyStr = key.world + ":" + key.x + ":" + key.z;
            section.set(keyStr + ".kills", data.kills);
            section.set(keyStr + ".time", data.lastKillTime);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            // Use silent fail or logger if available, but for now e.printStackTrace
            e.printStackTrace();
        }
    }

    private static class EvolutionData {
        int kills;
        long lastKillTime;

        EvolutionData(int kills, long lastKillTime) {
            this.kills = kills;
            this.lastKillTime = lastKillTime;
        }
    }

    private static class ChunkKey {
        private final String world;
        private final int x;
        private final int z;

        ChunkKey(String world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkKey chunkKey = (ChunkKey) o;
            return x == chunkKey.x && z == chunkKey.z && Objects.equals(world, chunkKey.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, z);
        }
    }
}
