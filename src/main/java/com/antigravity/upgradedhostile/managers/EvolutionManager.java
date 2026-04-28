package com.antigravity.upgradedhostile.managers;

import org.bukkit.Chunk;
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
