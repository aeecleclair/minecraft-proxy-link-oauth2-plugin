package cz.bloodbear.oauth2client.velocity.utils;


import com.google.gson.*;
import cz.bloodbear.oauth2client.core.utils.Config;
import cz.bloodbear.oauth2client.velocity.OAuth2Client;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class JsonConfig implements Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;
    private JsonObject jsonData;

    private final String filename;

    public JsonConfig(Path dataDirectory, String filename) {
        this.configPath = dataDirectory.resolve(filename);
        this.filename = filename;
        createDefaultConfig(filename);
        load();
    }

    private void createDefaultConfig(String filename) {
        if (!Files.exists(configPath)) {
            try (InputStream inputStream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(filename))) {
                Files.createDirectories(configPath.getParent());
                Files.copy(inputStream, configPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void mergeMessagesFromDefaults(String filename) {
        JsonObject defaultMessages = loadDefaultMessages(filename);
        JsonObject actualMessages = jsonData;

        boolean changed = mergeJsonObjectsRecursive(actualMessages, defaultMessages);
        if (changed) {
            createBackup();
            save();
        }
    }

    private void createBackup() {
        try {
            Path backupDir = configPath.getParent().resolve("_backup");
            Files.createDirectories(backupDir);

            String timestamp = java.time.LocalDateTime.now()
                    .toString()
                    .replace(":", "-")
                    .replace(".", "-");

            String backupFileName = configPath.getFileName().toString().replace(".json", "") + "_" + timestamp + ".json";
            Path backupFile = backupDir.resolve(backupFileName);

            Files.copy(configPath, backupFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Config backup created: " + backupFile.toAbsolutePath());

        } catch (IOException e) {
            OAuth2Client.getInstance().getLogger().error("An error occurred while creating config backup: {}", e.getMessage());
        }
    }


    private boolean mergeJsonObjectsRecursive(JsonObject target, JsonObject source) {
        boolean changed = false;

        for (String key : source.keySet()) {
            JsonElement sourceElement = source.get(key);

            if (sourceElement.isJsonObject()) {
                JsonObject targetChild;
                if (target.has(key) && target.get(key).isJsonObject()) {
                    targetChild = target.getAsJsonObject(key);
                } else {
                    targetChild = new JsonObject();
                    target.add(key, targetChild);
                    changed = true;
                }

                if (mergeJsonObjectsRecursive(targetChild, sourceElement.getAsJsonObject())) {
                    changed = true;
                }

            } else {
                if (!target.has(key)) {
                    target.add(key, sourceElement);
                    changed = true;
                }
            }
        }

        return changed;
    }

    private JsonObject loadDefaultMessages(String filename) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (inputStream != null) {
                Reader reader = new java.io.InputStreamReader(inputStream);
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonObject();
    }

    public void load() {
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath.getFileName().toString());
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            jsonData = JsonParser.parseReader(reader).getAsJsonObject();
            mergeMessagesFromDefaults(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(configPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(jsonData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JsonObject getSection(String sectionPath) {
        String[] keys = sectionPath.split("\\.");
        JsonObject current = jsonData;

        for (String key : keys) {
            if (current.has(key) && current.get(key).isJsonObject()) {
                current = current.getAsJsonObject(key);
            } else {
                JsonObject newSection = new JsonObject();
                current.add(key, newSection);
                current = newSection;
            }
        }
        return current;
    }

    public String getString(String path, String defaultValue) {
        String[] keys = path.split("\\.");
        JsonObject section = getSection(String.join(".", Arrays.copyOfRange(keys, 0, keys.length - 1)));
        String key = keys[keys.length - 1];

        return section.has(key) ? section.get(key).getAsString() : defaultValue;
    }

    public Integer getInt(String path, Integer defaultValue) {
        String[] keys = path.split("\\.");
        JsonObject section = getSection(String.join(".", Arrays.copyOfRange(keys, 0, keys.length - 1)));
        String key = keys[keys.length - 1];

        return section.has(key) ? section.get(key).getAsInt() : defaultValue;
    }

    public Boolean getBoolean(String path, Boolean defaultValue) {
        String[] keys = path.split("\\.");
        JsonObject section = getSection(String.join(".", Arrays.copyOfRange(keys, 0, keys.length - 1)));
        String key = keys[keys.length - 1];

        return section.has(key) ? section.get(key).getAsBoolean() : defaultValue;
    }

}