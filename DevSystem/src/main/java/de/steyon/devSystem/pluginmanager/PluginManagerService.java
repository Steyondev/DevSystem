package de.steyon.devSystem.pluginmanager;

import de.steyon.devSystem.DevSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PluginManagerService {

    private final DevSystem plugin;
    private final PluginManager pluginManager;
    private PluginManagerGUI pluginManagerGUI;
    private final MiniMessage miniMessage;
    
    public PluginManagerService(DevSystem plugin) {
        this.plugin = plugin;
        this.pluginManager = Bukkit.getPluginManager();
        this.miniMessage = plugin.getMiniMessage();
        
        initGUI();
    }
    
    private void initGUI() {
        this.pluginManagerGUI = new PluginManagerGUI(plugin, this);
    }
    
    /**
     * Opens the plugin manager GUI for a player
     * @param player The player to show the GUI to
     */
    public void openPluginManagerGUI(Player player) {
        pluginManagerGUI.openMainGUI(player);
    }
    
    /**
     * Gets a list of all plugins on the server
     * @return List of plugins
     */
    public List<Plugin> getPlugins() {
        return Arrays.asList(pluginManager.getPlugins());
    }
    
    /**
     * Gets a plugin by name
     * @param name The name of the plugin
     * @return The plugin, or null if not found
     */
    public Plugin getPlugin(String name) {
        for (Plugin plugin : pluginManager.getPlugins()) {
            if (plugin.getName().equalsIgnoreCase(name)) {
                return plugin;
            }
        }
        return null;
    }
    
    /**
     * Enables a plugin
     * @param target The plugin to enable
     * @param player The player who triggered the action
     * @return true if successful, false otherwise
     */
    public boolean enablePlugin(Plugin target, Player player) {
        if (target == null || target.isEnabled()) {
            return false;
        }
        
        pluginManager.enablePlugin(target);
        
        if (player != null) {
            player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-enabled", 
                "<prefix><white>Plugin <green>{plugin}</green> has been enabled!</white>")
                    .replace("{plugin}", target.getName())
            ));
        }
        
        return true;
    }
    
    /**
     * Disables a plugin
     * @param target The plugin to disable
     * @param player The player who triggered the action
     * @return true if successful, false otherwise
     */
    public boolean disablePlugin(Plugin target, Player player) {
        if (target == null || !target.isEnabled() || target.equals(plugin)) {
            if (target != null && target.equals(plugin) && player != null) {
                player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getValue("config.yml", "plugin-manager.cannot-disable-self", 
                    "<prefix><red>You cannot disable this plugin!</red>")
                ));
            }
            return false;
        }
        
        boolean allowDisableCore = plugin.getConfigManager().getValue("config.yml", "plugin-manager.settings.allow-disable-core-plugins", false);
        if (!allowDisableCore && isSystemPlugin(target)) {
            if (player != null) {
                player.sendMessage(miniMessage.deserialize("<prefix><red>Cannot disable core system plugin: " + target.getName() + "</red>"));
            }
            return false;
        }
        
        pluginManager.disablePlugin(target);
        
        if (player != null) {
            player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-disabled", 
                "<prefix><white>Plugin <red>{plugin}</red> has been disabled!</white>")
                    .replace("{plugin}", target.getName())
            ));
        }
        
        return true;
    }
    
    /**
     * Checks if a plugin is a core system plugin that shouldn't be disabled
     * @param plugin The plugin to check
     * @return true if it's a system plugin
     */
    private boolean isSystemPlugin(Plugin plugin) {
        String name = plugin.getName().toLowerCase();
        return name.equals("minecraft") || name.equals("bukkit") || 
               name.equals("spigot") || name.equals("paper") || 
               name.equals("craftbukkit") || name.equals("purpur");
    }
    
    /**
     * Reloads a plugin
     * @param target The plugin to reload
     * @param player The player who triggered the action
     * @return true if successful, false otherwise
     */
    public boolean reloadPlugin(Plugin target, Player player) {
        if (target == null || !target.isEnabled()) {
            if (player != null) {
                player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-not-enabled", 
                    "<prefix><red>Plugin is not enabled!</red>")
                ));
            }
            return false;
        }
        
        pluginManager.disablePlugin(target);
        pluginManager.enablePlugin(target);
        
        if (player != null) {
            player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-reloaded", 
                "<prefix><white>Plugin <yellow>{plugin}</yellow> has been reloaded!</white>")
                    .replace("{plugin}", target.getName())
            ));
        }
        
        return true;
    }
    
    /**
     * Displays plugin information to a player
     * @param target The plugin to show information about
     * @param player The player to show the information to
     */
    public void showPluginInfo(Plugin target, Player player) {
        if (target == null) {
            return;
        }
        
        Component header = miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-header", 
            "<gradient:green:blue>Plugin Information: {plugin}</gradient>")
                .replace("{plugin}", target.getName())
        );
        
        player.sendMessage(header);
        player.sendMessage(Component.empty());
        
        String enabledText = target.isEnabled() 
            ? plugin.getConfigManager().getValue("config.yml", "plugin-manager.status-enabled", "<green>Enabled</green>")
            : plugin.getConfigManager().getValue("config.yml", "plugin-manager.status-disabled", "<red>Disabled</red>");
            
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-name", "<white>Name: <green>{name}</green></white>")
                .replace("{name}", target.getName())
        ));
        
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-version", "<white>Version: <green>{version}</green></white>")
                .replace("{version}", target.getDescription().getVersion())
        ));
        
        String authorName = "Unknown";
        if (target.getDescription().getAuthors() != null && !target.getDescription().getAuthors().isEmpty()) {
            authorName = String.join(", ", target.getDescription().getAuthors());
        }
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-authors", "<white>Author(s): <green>{authors}</green></white>")
                .replace("{authors}", authorName)
        ));
        
        String desc = "No description available";
        if (target.getDescription().getDescription() != null) {
            desc = target.getDescription().getDescription();
        }
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-description", "<white>Description: <green>{description}</green></white>")
                .replace("{description}", desc)
        ));
        
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-status", "<white>Status: {status}</white>")
                .replace("{status}", enabledText)
        ));
    }
    
    /**
     * Displays a list of plugins to a player
     * @param player The player to show the plugin list to
     */
    public void listPlugins(Player player) {
        List<Plugin> plugins = getPlugins();
        int enabledCount = (int) plugins.stream().filter(Plugin::isEnabled).count();
        
        Component header = miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.list-header", 
            "<gradient:green:blue>Plugin List</gradient> <gray>({enabled}/{total})</gray>")
                .replace("{enabled}", String.valueOf(enabledCount))
                .replace("{total}", String.valueOf(plugins.size()))
        );
        
        player.sendMessage(header);
        player.sendMessage(Component.empty());
        
        boolean showVersion = plugin.getConfigManager().getValue("config.yml", "plugin-manager.settings.show-plugin-version-in-list", true);
        String enabledFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.list-format-enabled", "<green>{plugin}</green>");
        String disabledFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.list-format-disabled", "<red>{plugin}</red>");
        String separator = plugin.getConfigManager().getValue("config.yml", "plugin-manager.list-separator", "<gray>, </gray>");
        
        String pluginList = plugins.stream()
            .map(p -> {
                String template = p.isEnabled() ? enabledFormat : disabledFormat;
                String name = p.getName();
                if (showVersion) {
                    name += " (" + p.getDescription().getVersion() + ")";
                }
                return template.replace("{plugin}", name);
            })
            .collect(Collectors.joining(separator));
            
        player.sendMessage(miniMessage.deserialize(pluginList));
    }
} 