package com.mdt.leaderboard;

import arc.util.CommandHandler;
import arc.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.mod.Plugin;

public final class PlayerLeaderboardPlugin extends Plugin {
    private static final String CONFIG_DIR_NAME = "mdt-player-leaderboard";
    private static final String CONFIG_FILE_NAME = "player-leaderboard.properties";

    private volatile Config config;
    private volatile String activeField;

    @Override
    public void init() {
        try {
            File dataRoot = resolveDataRoot();
            ensureDefaultResources(dataRoot);
            reloadConfig();
            Log.info("MDT Player Leaderboard loaded.");
            Log.info("Config directory: @", dataRoot.getAbsolutePath());
        } catch (IOException exception) {
            throw new RuntimeException("Failed to initialize MDT Player Leaderboard.", exception);
        }
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("leaderboard-show", "[page]", "Show the current leaderboard page.", args -> {
            int page = args.length == 0 ? 1 : parseInt(args[0], 1);
            printLeaderboard(page);
        });

        handler.register("leaderboard-field", "<field>", "Switch the active leaderboard field.", args -> {
            String field = args[0].trim();
            if (field.isEmpty()) {
                Log.err("Leaderboard field cannot be empty.");
                return;
            }
            activeField = field;
            Log.info("Leaderboard field switched to @", activeField);
            printLeaderboard(1);
        });

        handler.register("leaderboard-fields", "[limit]", "List available leaderboard fields from stored data.", args -> {
            int limit = args.length == 0 ? 20 : parseInt(args[0], 20);
            printFields(Math.max(1, limit));
        });

        handler.register("leaderboard-reload", "Reload leaderboard configuration.", args -> {
            try {
                String currentField = activeField;
                reloadConfig();
                if (currentField != null && !currentField.trim().isEmpty()) {
                    activeField = currentField.trim();
                }
                Log.info("Leaderboard reloaded. list=@ field=@", config.listName, activeField);
            } catch (IOException exception) {
                Log.err("Failed to reload leaderboard config: @", exception.getMessage());
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("leaderboard", "[page]", "Show the player leaderboard.", (args, player) -> {
            int page = args.length == 0 ? 1 : parseInt(args[0], 1);
            player.sendMessage(buildLeaderboardMessage(page));
        });
    }

    private void printLeaderboard(int page) {
        for (String line : buildLines(page)) {
            Log.info(line);
        }
    }

    private void printFields(int limit) {
        List<String> fields = discoverFields(limit);
        if (fields.isEmpty()) {
            Log.info("No leaderboard fields discovered in list @.", config.listName);
            return;
        }
        Log.info("Available leaderboard fields: @", String.join(", ", fields));
    }

    private String buildLeaderboardMessage(int page) {
        StringBuilder builder = new StringBuilder();
        for (String line : buildLines(page)) {
            builder.append(line).append("\n");
        }
        return builder.toString().trim();
    }

    private List<String> buildLines(int requestedPage) {
        if (!config.enabled) {
            List<String> lines = new ArrayList<String>();
            lines.add("[scarlet]Leaderboard is disabled by configuration.");
            return lines;
        }
        List<Entry> entries = buildEntries();
        List<String> lines = new ArrayList<String>();
        lines.add(config.title + " field=" + activeField);
        if (entries.isEmpty()) {
            lines.add("[scarlet]No leaderboard data available.");
            return lines;
        }

        int pageSize = Math.max(1, config.displayCount);
        int totalPages = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        int start = (page - 1) * pageSize;
        int end = Math.min(entries.size(), start + pageSize);
        lines.add("[lightgray]page " + page + "/" + totalPages + " total=" + entries.size());
        for (int index = start; index < end; index++) {
            Entry entry = entries.get(index);
            int rank = index + 1;
            lines.add(colorForRank(rank) + "#" + rank + "[] " + entry.displayName + " = [accent]" + entry.value + "[]");
        }
        return lines;
    }

    private List<Entry> buildEntries() {
        Map<String, Map<String, String>> list = listDataList(config.listName);
        List<Entry> entries = new ArrayList<Entry>();
        for (Map.Entry<String, Map<String, String>> objectEntry : list.entrySet()) {
            String value = objectEntry.getValue().get(activeField);
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            Entry entry = new Entry();
            entry.key = objectEntry.getKey();
            entry.value = value.trim();
            entry.displayName = pickDisplayName(entry.key, objectEntry.getValue());
            entries.add(entry);
        }

        Comparator<Entry> comparator = comparatorForOrder();
        Collections.sort(entries, comparator);
        return entries;
    }

    private List<String> discoverFields(int limit) {
        Map<String, Map<String, String>> list = listDataList(config.listName);
        LinkedHashMap<String, Boolean> fields = new LinkedHashMap<String, Boolean>();
        for (Map<String, String> object : list.values()) {
            for (String key : object.keySet()) {
                if (key == null || key.trim().isEmpty()) {
                    continue;
                }
                fields.putIfAbsent(key.trim(), Boolean.TRUE);
                if (fields.size() >= limit) {
                    return new ArrayList<String>(fields.keySet());
                }
            }
        }
        return new ArrayList<String>(fields.keySet());
    }

    private Comparator<Entry> comparatorForOrder() {
        return (left, right) -> {
            Long leftNumber = parseLong(left.value);
            Long rightNumber = parseLong(right.value);
            int result;
            if (leftNumber != null && rightNumber != null) {
                result = leftNumber.compareTo(rightNumber);
            } else {
                result = left.value.compareToIgnoreCase(right.value);
            }
            if ("desc".equalsIgnoreCase(config.sortOrder)) {
                result = -result;
            }
            if (result != 0) {
                return result;
            }
            return left.displayName.compareToIgnoreCase(right.displayName);
        };
    }

    private String pickDisplayName(String key, Map<String, String> object) {
        String name = object.get("lastName");
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        String comid = object.get("comid");
        if (comid != null && !comid.trim().isEmpty()) {
            return comid.trim();
        }
        return key;
    }

    private String colorForRank(int rank) {
        if (rank == 1) return "[" + config.firstColor + "]";
        if (rank == 2) return "[" + config.secondColor + "]";
        if (rank == 3) return "[" + config.thirdColor + "]";
        return "[accent]";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, String>> listDataList(String listName) {
        try {
            Class<?> listDataClass = Class.forName("com.mdt.listdata.ListDataSystemPlugin");
            Method method = listDataClass.getMethod("getList", String.class);
            Object result = method.invoke(null, listName);
            return result == null ? new LinkedHashMap<String, Map<String, String>>() : (Map<String, Map<String, String>>)result;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load leaderboard data.", exception);
        }
    }

    private void reloadConfig() throws IOException {
        Properties properties = new Properties();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(new File(resolveDataRoot(), CONFIG_FILE_NAME)), StandardCharsets.UTF_8);
        try {
            properties.load(reader);
        } finally {
            reader.close();
        }
        config = new Config(
            readBoolean(properties, "leaderboard.enabled", true),
            readString(properties, "leaderboard.listName", "player_profile"),
            readString(properties, "leaderboard.field", "level"),
            readString(properties, "leaderboard.sortOrder", "desc"),
            readInt(properties, "leaderboard.displayCount", 10),
            readString(properties, "display.title", "[accent]Player Leaderboard[]"),
            readString(properties, "display.firstColor", "gold"),
            readString(properties, "display.secondColor", "lightgray"),
            readString(properties, "display.thirdColor", "orange")
        );
        activeField = config.defaultField;
    }

    private File resolveDataRoot() {
        File modsRoot = new File(Vars.dataDirectory.absolutePath(), "mods");
        return new File(new File(modsRoot, "config"), CONFIG_DIR_NAME);
    }

    private void ensureDefaultResources(File dataRoot) throws IOException {
        if (!dataRoot.exists() && !dataRoot.mkdirs() && !dataRoot.isDirectory()) {
            throw new IOException("Unable to create config directory: " + dataRoot.getAbsolutePath());
        }
        copyIfMissing(dataRoot, CONFIG_FILE_NAME);
    }

    private void copyIfMissing(File dataRoot, String resourceName) throws IOException {
        File target = new File(dataRoot, resourceName);
        if (target.exists()) {
            return;
        }
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Missing bundled resource: " + resourceName);
            }
            Files.copy(inputStream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String readString(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private boolean readBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null || value.trim().isEmpty() ? fallback : Boolean.parseBoolean(value.trim());
    }

    private int readInt(Properties properties, String key, int fallback) {
        return parseInt(properties.getProperty(key), fallback);
    }

    private int parseInt(String raw, int fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(Long.parseLong(raw.trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static final class Config {
        private final boolean enabled;
        private final String listName;
        private final String defaultField;
        private final String sortOrder;
        private final int displayCount;
        private final String title;
        private final String firstColor;
        private final String secondColor;
        private final String thirdColor;

        private Config(
            boolean enabled,
            String listName,
            String defaultField,
            String sortOrder,
            int displayCount,
            String title,
            String firstColor,
            String secondColor,
            String thirdColor
        ) {
            this.enabled = enabled;
            this.listName = listName;
            this.defaultField = defaultField;
            this.sortOrder = sortOrder;
            this.displayCount = displayCount;
            this.title = title;
            this.firstColor = firstColor;
            this.secondColor = secondColor;
            this.thirdColor = thirdColor;
        }
    }

    private static final class Entry {
        private String key;
        private String displayName;
        private String value;
    }
}
