package net.livaddons.net;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.livaddons.data.PlayerCosmeticData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ApiClient {
    private static final String API_BASE_URL = "https://livaddons.rape.ink";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private static String getApiBaseUrl() {
        return API_BASE_URL;
    }

    private static String generateServerId(String uuid) {
        try {
            String raw = uuid + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, bytes).toString(16);
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    public static CompletableFuture<PlayerCosmeticData> fetchProfile(UUID uuid) {
        String url = getApiBaseUrl() + "/api/user/" + uuid.toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return parseProfile(response.body());
                    }
                    return null;
                })
                .exceptionally(ex -> {
                    System.err.println("[LivAddons] Error fetching profile for " + uuid + ": " + ex.getMessage());
                    return null;
                });
    }

    public static CompletableFuture<Map<UUID, PlayerCosmeticData>> fetchBulkProfiles(List<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        String url = getApiBaseUrl() + "/api/users/bulk";
        JsonObject jsonBody = new JsonObject();
        jsonBody.add("uuids", GSON.toJsonTree(uuids.stream().map(UUID::toString).toList()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .timeout(Duration.ofSeconds(5))
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    Map<UUID, PlayerCosmeticData> result = new HashMap<>();
                    if (response.statusCode() == 200) {
                        Type type = new TypeToken<Map<String, JsonObject>>(){}.getType();
                        Map<String, JsonObject> map = GSON.fromJson(response.body(), type);
                        if (map != null) {
                            for (Map.Entry<String, JsonObject> entry : map.entrySet()) {
                                try {
                                    UUID uuid = UUID.fromString(entry.getKey());
                                    PlayerCosmeticData data = parseProfile(entry.getValue().toString());
                                    if (data != null) {
                                        result.put(uuid, data);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    return result;
                })
                .exceptionally(ex -> {
                    System.err.println("[LivAddons] Error bulk fetching profiles: " + ex.getMessage());
                    return Collections.emptyMap();
                });
    }

    public static CompletableFuture<Map<UUID, PlayerCosmeticData>> fetchAllProfiles() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getApiBaseUrl() + "/api/users/cosmetics"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    Map<UUID, PlayerCosmeticData> result = new HashMap<>();
                    if (response.statusCode() != 200) {
                        return result;
                    }

                    Type type = new TypeToken<Map<String, JsonObject>>(){}.getType();
                    Map<String, JsonObject> map = GSON.fromJson(response.body(), type);
                    if (map == null) {
                        return result;
                    }

                    for (Map.Entry<String, JsonObject> entry : map.entrySet()) {
                        try {
                            UUID uuid = UUID.fromString(entry.getKey());
                            PlayerCosmeticData data = parseProfile(entry.getValue().toString());
                            if (data != null) {
                                result.put(uuid, data);
                            }
                        } catch (Exception ignored) {}
                    }
                    return result;
                })
                .exceptionally(ex -> {
                    System.err.println("[LivAddons] Error fetching cosmetic directory: " + ex.getMessage());
                    return Collections.emptyMap();
                });
    }

    // Register custom session with official Mojang Session Server.
    // Only the randomly generated serverId (not the accessToken) is sent to our backend.
    public static CompletableFuture<SyncResult> syncProfile(PlayerCosmeticData data) {
        return CompletableFuture.supplyAsync(() -> {
            Minecraft client = Minecraft.getInstance();
            User user = client.getUser();
            if (user == null || user.getAccessToken() == null || user.getAccessToken().isEmpty()) {
                return new SyncResult(false, "Invalid session token. Please re-login.");
            }

            String serverId = generateServerId(data.uuid);

            try {
                // Register session directly with Mojang Session Server join endpoint (1.1.0 Fix)
                JsonObject joinPayload = new JsonObject();
                joinPayload.addProperty("accessToken", user.getAccessToken());
                joinPayload.addProperty("selectedProfile", user.getProfileId().toString().replace("-", ""));
                joinPayload.addProperty("serverId", serverId);

                HttpRequest joinRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/join"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(joinPayload.toString()))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> joinResponse = HTTP_CLIENT.send(joinRequest, HttpResponse.BodyHandlers.ofString());
                if (joinResponse.statusCode() != 204 && joinResponse.statusCode() != 200) {
                    return new SyncResult(false, "Mojang authentication failed (" + joinResponse.statusCode() + ")");
                }
            } catch (Exception e) {
                System.err.println("[LivAddons] Mojang join failed: " + e.getMessage());
                return new SyncResult(false, "Mojang session authentication failed: " + e.getMessage());
            }

            String url = getApiBaseUrl() + "/api/user/sync";
            JsonObject jsonPayload = GSON.toJsonTree(data).getAsJsonObject();
            jsonPayload.addProperty("serverId", serverId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload.toString()))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            try {
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return new SyncResult(true, "Saved Successfully");
                } else if (response.statusCode() == 401) {
                    return new SyncResult(false, "Mojang session verification failed (401)");
                } else {
                    return new SyncResult(false, "Server error (" + response.statusCode() + ")");
                }
            } catch (Exception ex) {
                System.err.println("[LivAddons] Error syncing profile: " + ex.getMessage());
                return new SyncResult(false, "Connection error: " + ex.getMessage());
            }
        });
    }

    public record SyncResult(boolean success, String message) {}

    private static PlayerCosmeticData parseProfile(String jsonStr) {
        try {
            JsonObject obj = GSON.fromJson(jsonStr, JsonObject.class);
            PlayerCosmeticData data = new PlayerCosmeticData();
            data.uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : "";
            data.username = obj.has("username") ? obj.get("username").getAsString() : "";
            data.customNick = obj.has("customNick") ? obj.get("customNick").getAsString() : "";
            data.colorStart = obj.has("colorStart") ? obj.get("colorStart").getAsString() : "#FFFFFF";
            data.colorEnd = obj.has("colorEnd") ? obj.get("colorEnd").getAsString() : "#FFFFFF";
            data.isBold = obj.has("isBold") && obj.get("isBold").getAsBoolean();
            data.isItalic = obj.has("isItalic") && obj.get("isItalic").getAsBoolean();
            data.visualHeight = obj.has("visualHeight") ? obj.get("visualHeight").getAsFloat() : 1.0f;
            if (obj.has("cosmetics") && obj.get("cosmetics").isJsonArray()) {
                Type listType = new TypeToken<List<String>>(){}.getType();
                data.cosmetics = GSON.fromJson(obj.get("cosmetics"), listType);
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
