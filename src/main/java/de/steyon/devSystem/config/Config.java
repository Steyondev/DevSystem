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
    
    // Value caching system to improve performance
    private final Map<String, Object> valueCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> configValueCache = new ConcurrentHashMap<>();
    
    // Default values by section for when config entries don't exist
    private final Map<String, Map<String, Object>> defaultValues = new HashMap<>();
    
    // Current config version
    @Getter
    private int configVersion = 1;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
        
        // Default prefix value, will be updated by loadPrefix() after config is loaded
        this.prefix = "<gradient:green:blue>DevSystem</gradient> <dark_gray>»</dark_gray> ";
        
        // Setup default values
        setupDefaultValues();
        
        // Create default configs
        createDefaultConfig();
        
        // Load prefix from config if available
        loadPrefix();
    }
    
    /**
     * Loads prefix from config or uses the default
     */
    private void loadPrefix() {
        String configPrefix = mainConfig.getString("messages.prefix");
        if (configPrefix != null && !configPrefix.isEmpty()) {
            this.prefix = configPrefix;
        }
    }
    
    /**
     * Setup default values for all configuration sections
     */
    private void setupDefaultValues() {
        // Messages defaults
        Map<String, Object> messageDefaults = new HashMap<>();
        
        messageDefaults.put("prefix", "<bold><gradient:green:blue>DevSystem</gradient></bold> <dark_gray>»</dark_gray> ");
        messageDefaults.put("welcome", "<prefix><white>Welcome to the <gradient:green:blue>DevSystem</gradient>!</white>");
        messageDefaults.put("reload", "<prefix><white>Configuration has been <green>reloaded</green>!</white>");
        messageDefaults.put("no-permission", "<prefix><red>You don't have permission to use this command!</red>");
        
        // Plugin message defaults
        Map<String, String> pluginDefaults = new HashMap<>();
        pluginDefaults.put("enabled", "<prefix><white>Plugin has been <green>enabled</green>!</white>");
        pluginDefaults.put("disabled", "<prefix><white>Plugin has been <red>disabled</red>!</white>");
        pluginDefaults.put("checking-updates", "<prefix><white>Checking for updates...</white>");
        pluginDefaults.put("starting", "<prefix><white>Starting DevSystem...</white>");
        pluginDefaults.put("config-loaded", "<prefix><white>Configuration has been <green>loaded</green>!</white>");
        messageDefaults.put("plugin", pluginDefaults);

        // Error message defaults
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
        
        // Register all defaults
        defaultValues.put("messages", messageDefaults);
    }

    /**
     * Creates the default configuration files
     */
    private void createDefaultConfig() {
        // Main config
        saveDefaultConfig("config.yml");
        this.mainConfig = configs.get("config.yml");
        
        // Check for config version and update if needed
        handleConfigVersion();
        
        // Send message
        plugin.getServer().getConsoleSender().sendMessage(
                getPluginMessage("config-loaded")
        );
    }
    
    /**
     * Handles config versioning and migrations if needed
     */
    private void handleConfigVersion() {
        int currentVersion = mainConfig.getInt("config-version", 1);
        this.configVersion = currentVersion;
        
        // If config is outdated, perform migration
        if (currentVersion < getLatestConfigVersion()) {
            plugin.getLogger().info("Outdated config detected (v" + currentVersion + "). Updating to v" + getLatestConfigVersion());
            migrateConfig(currentVersion);
        }
    }
    
    /**
     * Gets the latest config version
     * @return Latest config version
     */
    public int getLatestConfigVersion() {
        return 1; // Increment when you make breaking changes to config structure
    }
    
    /**
     * Migrates config from one version to another
     * @param fromVersion Current config version
     */
    private void migrateConfig(int fromVersion) {
        // Example migration logic for future use
        if (fromVersion < 2) {
            // Migrate from v1 to v2name
            // mainConfig.set("new-section.new-value", "value");
        }
        
        // Set the new version
        mainConfig.set("config-version", getLatestConfigVersion());
        
        // Save changes
        try {
            mainConfig.save(configFiles.get("config.yml"));
            plugin.getLogger().info("Config successfully migrated to v" + getLatestConfigVersion());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save migrated config", e);
        }
        
        // Clear caches
        clearCache();
    }

    /**
     * Saves a default config file if it doesn't exist
     * @param configName Name of the config file
     */
    public void saveDefaultConfig(String configName) {
        File configFile = new File(plugin.getDataFolder(), configName);
        
        if (!configFile.exists()) {
            plugin.saveResource(configName, false);
        }
        
        FileConfiguration config = loadConfig(configFile);
        configs.put(configName, config);
        configFiles.put(configName, configFile);
    }
    
    /**
     * Loads a config file with proper encoding
     * @param file File to load
     * @return Loaded configuration
     */
    private FileConfiguration loadConfig(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // Load defaults from jar if they exist
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

    /**
     * Gets a configuration
     * @param name Config name
     * @return FileConfiguration if found, null otherwise
     */
    public FileConfiguration getConfig(String name) {
        return configs.getOrDefault(name, null);
    }
    
    /**
     * Creates a new config file
     * @param name Config name
     * @param defaults Default configuration
     * @return Created configuration
     */
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

    /**
     * Saves a config to disk
     * @param name Config name
     */
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

    /**
     * Reloads a config from disk
     * @param name Config name
     */
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
            // Re-check config version after reload
            handleConfigVersion();
            // Reload prefix
            loadPrefix();
        }
        
        // Clear cached values for this config
        clearCacheForConfig(name);
        
        plugin.getServer().getConsoleSender().sendMessage(
                getMessage("reload")
        );
    }

    /**
     * Reloads all configurations
     */
    public void reloadAllConfigs() {
        for (String name : configs.keySet()) {
            reloadConfig(name);
        }
        
        // Clear all caches
        clearCache();
    }
    
    /**
     * Gets a configuration section
     * @param config Config name
     * @param path Section path
     * @return ConfigurationSection if found, null otherwise
     */
    public ConfigurationSection getSection(String config, String path) {
        FileConfiguration configuration = configs.get(config);
        if (configuration == null) return null;
        
        return configuration.getConfigurationSection(path);
    }
    
    /**
     * Gets a value from config with caching
     * @param config Config name
     * @param path Value path
     * @param defaultValue Default value if not found
     * @param <T> Value type
     * @return Config value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String config, String path, T defaultValue) {
        String cacheKey = config + ":" + path;
        
        // Check cache first
        if (valueCache.containsKey(cacheKey)) {
            return (T) valueCache.get(cacheKey);
        }
        
        // Get from config
        FileConfiguration configuration = configs.get(config);
        if (configuration == null) return defaultValue;
        
        Object value = configuration.get(path, defaultValue);
        
        // Cache the result
        valueCache.put(cacheKey, value);
        
        return (T) value;
    }
    
    /**
     * Sets a value in config
     * @param config Config name
     * @param path Value path
     * @param value Value to set
     */
    public void setValue(String config, String path, Object value) {
        FileConfiguration configuration = configs.get(config);
        if (configuration == null) return;
        
        configuration.set(path, value);
        
        // Update cache
        String cacheKey = config + ":" + path;
        valueCache.put(cacheKey, value);
    }
    
    /**
     * Clears all value caches
     */
    public void clearCache() {
        valueCache.clear();
        configValueCache.clear();
    }
    
    /**
     * Clears cache for a specific config
     * @param config Config name
     */
    public void clearCacheForConfig(String config) {
        // Clear direct cache entries
        valueCache.entrySet().removeIf(entry -> entry.getKey().startsWith(config + ":"));
        
        // Clear section cache
        configValueCache.remove(config);
    }

    /**
     * Gets a raw message with proper fallback to defaults
     * @param path Message path
     * @param defaultMessage Default message if not in config or defaults
     * @return Raw message string
     */
    private String getRawMessage(String path, String defaultMessage) {
        // Check config first
        String configMessage = mainConfig.getString("messages." + path);
        if (configMessage != null) {
            return configMessage;
        }
        
        // Check defaults next
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
        
        // Fallback to provided default
        return defaultMessage;
    }

    /**
     * Gets a formatted message component
     * @param path Message path
     * @return Formatted component
     */
    public Component getMessage(String path) {
        String defaultMessage = "";
        String message = getRawMessage(path, defaultMessage);
        return MiniMessage.miniMessage().deserialize(message.replace("<prefix>", prefix));
    }
    
    /**
     * Gets a formatted plugin message component
     * @param path Message path
     * @return Formatted component
     */
    public Component getPluginMessage(String path) {
        String defaultMessage = "";
        String message = getRawMessage("plugin." + path, defaultMessage);
        return MiniMessage.miniMessage().deserialize(message.replace("<prefix>", prefix));
    }
    
    /**
     * Gets a formatted error message component
     * @param path Message path
     * @return Formatted component
     */
    public Component getErrorMessage(String path) {
        String defaultMessage = "";
        String message = getRawMessage("error." + path, defaultMessage);
        return MiniMessage.miniMessage().deserialize(message.replace("<prefix>", prefix));
    }
    
    /**
     * Gets a list of strings from config with fallback
     * @param path Path to string list
     * @param defaultList Default list if not found
     * @return List of strings
     */
    public List<String> getStringList(String path, List<String> defaultList) {
        List<String> list = mainConfig.getStringList(path);
        return list.isEmpty() ? defaultList : list;
    }
    
    /**
     * Checks if a path exists in config
     * @param path Path to check
     * @return True if exists, false otherwise
     */
    public boolean hasPath(String path) {
        return mainConfig.contains(path);
    }
    
    /**
     * Gets all keys in a section
     * @param path Section path
     * @param deep Whether to get keys recursively
     * @return Set of keys or empty set if section not found
     */
    public List<String> getKeys(String path, boolean deep) {
        ConfigurationSection section = mainConfig.getConfigurationSection(path);
        if (section == null) return List.of();
        
        return List.copyOf(section.getKeys(deep));
    }


    public Map<String, Object> getMessages() {
        return defaultValues.get("messages");
    }
    
    /**
     * Gets a message with placeholders
     * @param category
     * @param key
     * @param placeholders
     * @return
     */
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

    /**
     * Gets the placeholder key for a given category
     * @param category Category name
     * @return Placeholder key
     */
    private String getPlaceholderKey(String category) {
        return switch(category) {
            case "error", "plugin" -> "name";
            default -> throw new IllegalStateException("Unexpected value: " + category);
        };
    }

} 