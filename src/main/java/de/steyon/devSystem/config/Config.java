package de.steyon.devSystem.config;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Config {

    private final Plugin plugin;
    
    @Getter
    private final Map<String, FileConfiguration> configs;
    
    @Getter
    private final Map<String, File> configFiles;
    
    @Getter
    private FileConfiguration mainConfig;
    
    @Getter
    private String prefix;
    
    private final Map<String, Object> valueCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> configValueCache = new ConcurrentHashMap<>();
    
    private final Map<String, Map<String, Object>> defaultValues = new HashMap<>();
    
    @Getter
    private int configVersion = 1;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
        
        this.prefix = "<gradient:green:blue>DevSystem</gradient> <dark_gray>»</dark_gray> ";
        
        setupDefaultValues();
        
        createDefaultConfig();
        
        loadPrefix();
    }
    
    private void loadPrefix() {
        String configPrefix = mainConfig.getString("messages.prefix");
        if (configPrefix != null && !configPrefix.isEmpty()) {
            this.prefix = configPrefix;
        }
    }
    
    private void setupDefaultValues() {
        Map<String, Object> messageDefaults = new HashMap<>();
        
        messageDefaults.put("prefix", "<bold><gradient:green:blue>DevSystem</gradient></bold> <dark_gray>»</dark_gray> ");
        messageDefaults.put("welcome", "<prefix><white>Welcome to the <gradient:green:blue>DevSystem</gradient>!</white>");
        messageDefaults.put("reload", "<prefix><white>Configuration has been <green>reloaded</green>!</white>");
        messageDefaults.put("no-permission", "<prefix><red>You don't have permission to use this command!</red>");
        
        Map<String, String> pluginDefaults = new HashMap<>();
        pluginDefaults.put("enabled", "<prefix><white>Plugin has been <green>enabled</green>!</white>");
        pluginDefaults.put("disabled", "<prefix><white>Plugin has been <red>disabled</red>!</white>");
        pluginDefaults.put("checking-updates", "<prefix><white>Checking for updates...</white>");
        pluginDefaults.put("starting", "<prefix><white>Starting DevSystem...</white>");
        pluginDefaults.put("config-loaded", "<prefix><white>Configuration has been <green>loaded</green>!</white>");
        messageDefaults.put("plugin", pluginDefaults);

        Map<String, String> errorDefaults = new HashMap<>();
        errorDefaults.put("config-loaded", "<prefix><white>Configuration has been <green>loaded</green>!</white>");
        errorDefaults.put("folder-created", "<prefix><gray>Folder <aqua>{name}</aqua> <gray>created successfully.");
        errorDefaults.put("folder-exists", "<prefix><gray>Folder <aqua>{name}</aqua> <gray>already exists.");
        errorDefaults.put("folder-deleted", "<prefix><gray>Folder <aqua>{name}</aqua> <gray>deleted successfully.");
        errorDefaults.put("folder-not-exist", "<prefix><gray>Folder <aqua>{name}</aqua> <gray>does not exist.");
        errorDefaults.put("folder-delete-fail", "<prefix><gray>Failed to delete folder <aqua>{name}</aqua>.");

        errorDefaults.put("file-deleted", "<prefix><gray>File <aqua>{name}</aqua> <gray>deleted successfully.");
        errorDefaults.put("file-delete-fail", "<prefix><gray>Failed to delete file <aqua>{name}</aqua>.");
        errorDefaults.put("file-not-exist", "<prefix><gray>File <aqua>{name}</aqua> <gray>does not exist.");
        messageDefaults.put("error", errorDefaults);
        
        defaultValues.put("messages", messageDefaults);
    }

    private void createDefaultConfig() {
        saveDefaultConfig("config.yml");
        this.mainConfig = configs.get("config.yml");
        
        handleConfigVersion();
        
        plugin.getServer().getConsoleSender().sendMessage(
                getPluginMessage("config-loaded")
        );
    }
    
    private void handleConfigVersion() {
        int currentVersion = mainConfig.getInt("config-version", 1);
        this.configVersion = currentVersion;
        
        if (currentVersion < getLatestConfigVersion()) {
            plugin.getLogger().info("Outdated config detected (v" + currentVersion + "). Updating to v" + getLatestConfigVersion());
            migrateConfig(currentVersion);
        }
    }
    
    public int getLatestConfigVersion() {
        return 1;
    }
    
    private void migrateConfig(int fromVersion) {
        if (fromVersion < 2) {
        }
        
        mainConfig.set("config-version", getLatestConfigVersion());
        
        try {
            mainConfig.save(configFiles.get("config.yml"));
            plugin.getLogger().info("Config successfully migrated to v" + getLatestConfigVersion());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save migrated config", e);
        }
        
        clearCache();
    }

    public void saveDefaultConfig(String configName) {
        File configFile = new File(plugin.getDataFolder(), configName);
        
        if (!configFile.exists()) {
            plugin.saveResource(configName, false);
        }
        
        FileConfiguration config = loadConfig(configFile);
        configs.put(configName, config);
        configFiles.put(configName, configFile);
    }
    
    private FileConfiguration loadConfig(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        try {
            InputStream defaultStream = plugin.getResource(file.getName());
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                config.setDefaults(defaultConfig);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not load default config for " + file.getName(), e);
        }
        
        return config;
    }

    public FileConfiguration getConfig(String name) {
        return configs.getOrDefault(name, null);
    }
    
    public FileConfiguration createConfig(String name, FileConfiguration defaults) {
        File configFile = new File(plugin.getDataFolder(), name);
        
        try {
            if (!configFile.exists()) {
                if (configFile.createNewFile()) {
                    plugin.getLogger().info("Created new config file: " + name);
                }
            }
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            
            if (defaults != null) {
                config.setDefaults(defaults);
                config.options().copyDefaults(true);
                config.save(configFile);
            }
            
            configs.put(name, config);
            configFiles.put(name, configFile);
            
            return config;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create config file: " + name, e);
            return null;
        }
    }

    public void saveConfig(String name) {
        FileConfiguration config = configs.get(name);
        File configFile = configFiles.get(name);
        
        if (config == null || configFile == null) {
            plugin.getServer().getConsoleSender().sendMessage(
                    getErrorMessage("config-save")
            );
            return;
        }
        
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getServer().getConsoleSender().sendMessage(
                    getErrorMessage("config-save")
            );
            plugin.getLogger().log(Level.SEVERE, "Could not save config: " + name, e);
        }
    }

    public void reloadConfig(String name) {
        File configFile = configFiles.get(name);
        
        if (configFile == null) {
            plugin.getServer().getConsoleSender().sendMessage(
                    getErrorMessage("config-load")
            );
            return;
        }
        
        FileConfiguration config = loadConfig(configFile);
        configs.put(name, config);
        
        if (name.equals("config.yml")) {
            this.mainConfig = config;
            handleConfigVersion();
            loadPrefix();
        }
        
        clearCacheForConfig(name);
        
        plugin.getServer().getConsoleSender().sendMessage(
                getMessage("reload")
        );
    }

    public void reloadAllConfigs() {
        for (String name : configs.keySet()) {
            reloadConfig(name);
        }
        
        clearCache();
    }
    
    public ConfigurationSection getSection(String config, String path) {
        FileConfiguration configuration = configs.get(config);
        if (configuration == null) return null;
        
        return configuration.getConfigurationSection(path);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getValue(String config, String path, T defaultValue) {
        String cacheKey = config + ":" + path;
        
        if (valueCache.containsKey(cacheKey)) {
            return (T) valueCache.get(cacheKey);
        }
        
        FileConfiguration configuration = configs.get(config);
        if (configuration == null) return defaultValue;
        
        Object value = configuration.get(path, defaultValue);
        
        if (value instanceof String) {
            value = ((String) value).replace("<prefix>", prefix);
        }
        
        valueCache.put(cacheKey, value);
        
        return (T) value;
    }
    
    public void setValue(String config, String path, Object value) {
        FileConfiguration configuration = configs.get(config);
        if (configuration == null) return;
        
        configuration.set(path, value);
        
        String cacheKey = config + ":" + path;
        valueCache.put(cacheKey, value);
    }
    
    public void clearCache() {
        valueCache.clear();
        configValueCache.clear();
    }
    
    public void clearCacheForConfig(String config) {
        valueCache.entrySet().removeIf(entry -> entry.getKey().startsWith(config + ":"));
        
        configValueCache.remove(config);
    }

    private String getRawMessage(String path, String defaultMessage) {
        String configMessage = mainConfig.getString("messages." + path);
        if (configMessage != null) {
            return configMessage;
        }
        
        String[] parts = path.split("\\.", 2);
        if (parts.length == 2) {
            Map<String, Object> sectionDefaults = defaultValues.get("messages");
            if (sectionDefaults != null) {
                Map<String, Object> subsection = (Map<String, Object>) sectionDefaults.get(parts[0]);
                if (subsection != null && subsection.containsKey(parts[1])) {
                    return (String) subsection.get(parts[1]);
                }
            }
        } else {
            Map<String, Object> sectionDefaults = defaultValues.get("messages");
            if (sectionDefaults != null && sectionDefaults.containsKey(path)) {
                return (String) sectionDefaults.get(path);
            }
        }
        
        return defaultMessage;
    }

    public Component getMessage(String path) {
        String defaultMessage = "";
        String message = getRawMessage(path, defaultMessage);
        return MiniMessage.miniMessage().deserialize(message.replace("<prefix>", prefix));
    }
    
    public Component getPluginMessage(String path) {
        String defaultMessage = "";
        String message = getRawMessage("plugin." + path, defaultMessage);
        return MiniMessage.miniMessage().deserialize(message.replace("<prefix>", prefix));
    }
    
    public Component getErrorMessage(String path) {
        String defaultMessage = "";
        String message = getRawMessage("error." + path, defaultMessage);
        return MiniMessage.miniMessage().deserialize(message.replace("<prefix>", prefix));
    }
    
    public List<String> getStringList(String path, List<String> defaultList) {
        List<String> list = mainConfig.getStringList(path);
        return list.isEmpty() ? defaultList : list;
    }
    
    public boolean hasPath(String path) {
        return mainConfig.contains(path);
    }
    
    public List<String> getKeys(String path, boolean deep) {
        ConfigurationSection section = mainConfig.getConfigurationSection(path);
        if (section == null) return List.of();
        
        return List.copyOf(section.getKeys(deep));
    }


    public Map<String, Object> getMessages() {
        return defaultValues.get("messages");
    }
    
    public String getMessage(String category, String key, String... placeholders) {
        String message = ((HashMap<String, String>) getMessages().get(category)).getOrDefault(key, "<red>Message not found: " + key + "</red>");
        message = message.replace("<prefix>", getPrefix());

        if (placeholders != null) {
            String placeholderKey = getPlaceholderKey(category);
            for (String placeholder : placeholders) {
                message = message.replace("{" + placeholderKey + "}", placeholder);
            }
        }

        return message;
    }

    private String getPlaceholderKey(String category) {
        return switch(category) {
            case "error", "plugin" -> "name";
            default -> throw new IllegalStateException("Unexpected value: " + category);
        };
    }

} 