package net.livaddons.data;

import net.livaddons.net.ApiClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static final PlayerDataManager INSTANCE = new PlayerDataManager();

    private final Map<UUID, PlayerCosmeticData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastFetchTime = new ConcurrentHashMap<>();
    private final Set<UUID> pendingFetch = ConcurrentHashMap.newKeySet();
    private static final long CACHE_TTL_MS = 15000; // 15 seconds

    public static PlayerDataManager getInstance() {
        return INSTANCE;
    }

    public PlayerCosmeticData getCosmeticData(UUID uuid) {
        if (uuid == null) return null;
        return cache.get(uuid);
    }

    public void updateCache(PlayerCosmeticData data) {
        if (data != null && data.uuid != null && !data.uuid.isEmpty()) {
            try {
                UUID uuid = UUID.fromString(data.uuid);
                cache.put(uuid, data);
                lastFetchTime.put(uuid, System.currentTimeMillis());
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void requestFetchIfMissing(UUID uuid) {
        long now = System.currentTimeMillis();
        if (uuid == null || pendingFetch.contains(uuid)) {
            return;
        }
        if (cache.containsKey(uuid) && (now - lastFetchTime.getOrDefault(uuid, 0L)) < CACHE_TTL_MS) {
            return;
        }

        pendingFetch.add(uuid);
        ApiClient.fetchProfile(uuid).thenAccept(data -> {
            pendingFetch.remove(uuid);
            if (data != null) {
                try {
                    UUID u = UUID.fromString(data.uuid);
                    cache.put(u, data);
                    lastFetchTime.put(u, System.currentTimeMillis());
                } catch (Exception ignored) {}
            }
        });
    }

    public void requestBulkFetch(List<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) return;

        long now = System.currentTimeMillis();
        List<UUID> toFetch = uuids.stream()
                .filter(u -> !pendingFetch.contains(u))
                .filter(u -> !cache.containsKey(u) || (now - lastFetchTime.getOrDefault(u, 0L)) >= CACHE_TTL_MS)
                .toList();

        if (toFetch.isEmpty()) return;

        pendingFetch.addAll(toFetch);
        ApiClient.fetchBulkProfiles(toFetch).thenAccept(map -> {
            pendingFetch.removeAll(toFetch);
            if (map != null) {
                cache.putAll(map);
                long fetchNow = System.currentTimeMillis();
                for (UUID u : map.keySet()) {
                    lastFetchTime.put(u, fetchNow);
                }
            }
        });
    }

    public Collection<PlayerCosmeticData> getAllCosmeticData() {
        return cache.values();
    }
}
