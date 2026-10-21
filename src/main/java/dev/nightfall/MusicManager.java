package dev.nightfall;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the active Windows media session. Spotify desktop and Yandex Music
 * clients/browsers are supported when Windows exposes their playback session
 * through GlobalSystemMediaTransportControls.
 */
public final class MusicManager {
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(3, r -> {
        Thread t = new Thread(r, "NIGHTFALL-Media");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();
    private static final Pattern LRC_LINE = Pattern.compile("^\\[(\\d{1,2}):(\\d{2})(?:[.](\\d{1,3}))?\\](.*)$");
    private static final Path SCRIPT_PATH = MinecraftClient.getInstance().runDirectory.toPath()
            .resolve("config/nightfall/media-bridge.ps1");

    private static volatile MediaState state = MediaState.empty();
    private static volatile List<LyricLine> lyrics = List.of();
    private static volatile String lyricTrackKey = "";
    private static volatile String lyricError = "";
    private static volatile long lastLyricsAttemptAt;

    private MusicManager() {}

    public static void init() {
        try {
            Files.createDirectories(SCRIPT_PATH.getParent());
            try (var in = MusicManager.class.getResourceAsStream("/assets/nightfall/media-bridge.ps1")) {
                if (in != null) Files.copy(in, SCRIPT_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            NightfallClient.LOGGER.warn("Could not prepare NIGHTFALL media bridge", e);
        }
        EXECUTOR.scheduleWithFixedDelay(MusicManager::poll, 1, 3, TimeUnit.SECONDS);
    }

    public static MediaState state() { return state; }

    public static String currentLyric() {
        List<LyricLine> current = lyrics;
        if (current.isEmpty()) return "";
        long position = state.positionNow();
        String line = "";
        for (LyricLine entry : current) {
            if (entry.timeMs() > position) break;
            line = entry.text();
        }
        return line;
    }

    public static List<LyricLine> lyrics() { return lyrics; }
    public static String lyricError() { return lyricError; }

    public static void command(String action) {
        if (!List.of("toggle", "next", "previous").contains(action)) return;
        EXECUTOR.execute(() -> {
            String output = executePowerShell(action);
            if (output.isBlank()) NightfallClient.LOGGER.debug("Media command returned no output: {}", action);
            try { Thread.sleep(350); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            poll();
        });
    }

    private static void poll() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            state = MediaState.unsupported();
            return;
        }
        String output = executePowerShell("status");
        if (output.isBlank()) {
            state = MediaState.unavailable();
            lyrics = List.of();
            lyricTrackKey = "";
            lyricError = "";
            lastLyricsAttemptAt = 0L;
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(output).getAsJsonObject();
            boolean ok = json.has("ok") && json.get("ok").getAsBoolean();
            String title = string(json, "title");
            String artist = string(json, "artist");
            String app = string(json, "app");
            String status = string(json, "status");
            long pos = number(json, "positionMs");
            long duration = number(json, "durationMs");
            MediaState next = new MediaState(ok, title, artist, app, status, pos, duration, System.currentTimeMillis());
            state = next;
            if (!title.isBlank()) {
                String key = title + "\u0000" + artist;
                if (!key.equals(lyricTrackKey)) {
                    lyrics = List.of();
                    lyricError = "";
                    lyricTrackKey = key;
                    lastLyricsAttemptAt = 0L;
                }
                long now = System.currentTimeMillis();
                // Retry periodically when LRCLIB is offline, rate-limited, or the track has no synced entry.
                if (lyrics.isEmpty() && now - lastLyricsAttemptAt >= 30_000L) {
                    lastLyricsAttemptAt = now;
                    EXECUTOR.execute(() -> fetchLyrics(title, artist, key));
                }
            } else {
                lyrics = List.of();
                lyricTrackKey = "";
                lyricError = "";
                lastLyricsAttemptAt = 0L;
            }
        } catch (Exception e) {
            NightfallClient.LOGGER.debug("Could not parse Windows media session: {}", e.getMessage());
            state = MediaState.unavailable();
        }
    }

    private static String executePowerShell(String action) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win") || !Files.exists(SCRIPT_PATH)) return "";
        Process process = null;
        try {
            process = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-File", SCRIPT_PATH.toString(), action)
                    .redirectErrorStream(true).start();
            if (!process.waitFor(2500, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return "";
            }
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int last = out.lastIndexOf('{');
            if (last > 0) out = out.substring(last);
            return out;
        } catch (Exception e) {
            if (process != null) process.destroyForcibly();
            NightfallClient.LOGGER.debug("Windows media bridge unavailable: {}", e.getMessage());
            return "";
        }
    }

    private static void fetchLyrics(String title, String artist, String key) {
        try {
            String url = "https://lrclib.net/api/search?track_name=" + encode(title) + "&artist_name=" + encode(artist);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "NIGHTFALL/1.0 (Minecraft mod)")
                    .GET().build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (!key.equals(lyricTrackKey) || response.statusCode() != 200) return;
            JsonElement root = JsonParser.parseString(response.body());
            if (!root.isJsonArray()) return;
            JsonArray array = root.getAsJsonArray();
            JsonObject best = null;
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject candidate = element.getAsJsonObject();
                if (candidate.has("syncedLyrics") && !candidate.get("syncedLyrics").isJsonNull()
                        && !candidate.get("syncedLyrics").getAsString().isBlank()) {
                    best = candidate;
                    break;
                }
            }
            if (best == null) {
                lyricError = "Синхронные тексты не найдены";
                return;
            }
            String raw = best.get("syncedLyrics").getAsString();
            List<LyricLine> parsed = new ArrayList<>();
            for (String line : raw.split("\\R")) {
                Matcher matcher = LRC_LINE.matcher(line.trim());
                if (!matcher.matches()) continue;
                long minutes = Long.parseLong(matcher.group(1));
                long seconds = Long.parseLong(matcher.group(2));
                String fraction = matcher.group(3);
                long millis = 0;
                if (fraction != null) {
                    if (fraction.length() == 1) millis = Integer.parseInt(fraction) * 100L;
                    else if (fraction.length() == 2) millis = Integer.parseInt(fraction) * 10L;
                    else millis = Integer.parseInt(fraction.substring(0, 3));
                }
                String text = matcher.group(4).trim();
                if (!text.isBlank()) parsed.add(new LyricLine((minutes * 60 + seconds) * 1000 + millis, text));
            }
            parsed.sort(Comparator.comparingLong(LyricLine::timeMs));
            if (key.equals(lyricTrackKey)) {
                lyrics = List.copyOf(parsed);
                lyricError = parsed.isEmpty() ? "Синхронные тексты не найдены" : "";
            }
        } catch (Exception e) {
            if (key.equals(lyricTrackKey)) lyricError = "Не удалось загрузить lyrics";
            NightfallClient.LOGGER.debug("Lyrics lookup failed: {}", e.getMessage());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
    private static String string(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }
    private static long number(JsonObject obj, String key) {
        try { return obj.has(key) ? obj.get(key).getAsLong() : 0L; } catch (Exception ignored) { return 0L; }
    }

    public record LyricLine(long timeMs, String text) {}

    public record MediaState(boolean ok, String title, String artist, String app, String status,
                             long positionMs, long durationMs, long sampledAt) {
        static MediaState empty() { return new MediaState(false, "", "", "", "Waiting", 0, 0, System.currentTimeMillis()); }
        static MediaState unavailable() { return new MediaState(false, "", "", "", "Unavailable", 0, 0, System.currentTimeMillis()); }
        static MediaState unsupported() { return new MediaState(false, "", "", "", "Windows only", 0, 0, System.currentTimeMillis()); }
        public boolean playing() { return status.toLowerCase().contains("playing"); }
        public long positionNow() { return Math.max(0, positionMs + (playing() ? System.currentTimeMillis() - sampledAt : 0)); }
    }
}
