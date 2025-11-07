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
import java.util.ArrayList;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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
    
    public void openPluginManagerGUI(Player player) {
        pluginManagerGUI.openMainGUI(player);
    }
    
    public List<Plugin> getPlugins() {
        return Arrays.asList(pluginManager.getPlugins());
    }
    
    public Plugin getPlugin(String name) {
        for (Plugin plugin : pluginManager.getPlugins()) {
            if (plugin.getName().equalsIgnoreCase(name)) {
                return plugin;
            }
        }
        return null;
    }

    public Plugin loadPluginFromJar(File jar, Player player) {
        try {
            if (jar == null || !jar.exists() || !jar.isFile() || !jar.getName().toLowerCase().endsWith(".jar")) {
                if (player != null) {
                    player.sendMessage(miniMessage.deserialize(
                        plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-not-found",
                            "<prefix><red>Plugin not found</red><dark_gray>: {plugin}")
                            .replace("{plugin}", jar != null ? jar.getName() : "-")
                    ));
                }
                return null;
            }

            Plugin loaded = null;
            try {
                java.lang.reflect.Method m = pluginManager.getClass().getMethod("loadPlugin", File.class);
                Object obj = m.invoke(pluginManager, jar);
                if (obj instanceof Plugin) loaded = (Plugin) obj;
            } catch (NoSuchMethodException ignore) {
                try {
                    Class<?> spm = Class.forName("org.bukkit.plugin.SimplePluginManager");
                    if (spm.isInstance(pluginManager)) {
                        java.lang.reflect.Method m = spm.getDeclaredMethod("loadPlugin", File.class);
                        m.setAccessible(true);
                        Object obj = m.invoke(pluginManager, jar);
                        if (obj instanceof Plugin) loaded = (Plugin) obj;
                    }
                } catch (Throwable ignored) {}
            }

            if (loaded == null) {
                if (player != null) {
                    player.sendMessage(miniMessage.deserialize(
                        plugin.getConfigManager().getValue("config.yml", "plugin-manager.load-failed",
                            "<prefix><red>Failed to load plugin</red><dark_gray>: </dark_gray><aqua>{plugin}</aqua>")
                            .replace("{plugin}", jar.getName())
                    ));
                }
                return null;
            }

            Plugin existing = getPlugin(loaded.getName());
            if (existing != null && existing != loaded) {
                if (player != null) {
                    player.sendMessage(miniMessage.deserialize(
                        plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-already-enabled",
                            "<prefix><red>Plugin is already enabled</red><dark_gray>: {plugin}</dark_gray></red>")
                            .replace("{plugin}", existing.getName())
                    ));
                }
                return null;
            }

            pluginManager.enablePlugin(loaded);

            if (player != null) {
                player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-enabled",
                        "<prefix><white>Plugin <green>{plugin}</green> has been enabled!</white>")
                        .replace("{plugin}", loaded.getName())
                ));
            }
            return loaded;
        } catch (Throwable t) {
            if (player != null) {
                player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getValue("config.yml", "plugin-manager.load-exception",
                        "<prefix><red>Exception while loading plugin</red><dark_gray>: </dark_gray><aqua>{plugin}</aqua>")
                        .replace("{plugin}", jar != null ? jar.getName() : "-")
                ));
            }
            return null;
        }
    }

    public String getApiVersion(Plugin p) {
        try {
            String v = p.getDescription().getAPIVersion();
            return v != null ? v : "unknown";
        } catch (Throwable t) { return "unknown"; }
    }

    public String getLoadPhase(Plugin p) {
        try {
            return String.valueOf(p.getDescription().getLoad());
        } catch (Throwable t) { return "POSTWORLD"; }
    }

    public List<String> getProvides(Plugin p) {
        try {
            List<String> provides = p.getDescription().getProvides();
            return provides != null ? provides : new ArrayList<>();
        } catch (Throwable t) { return new ArrayList<>(); }
    }

    public String getJarPath(Plugin p) {
        try {
            URL url = p.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (url == null) return "unknown";
            String path = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
            return path;
        } catch (Throwable t) { return "unknown"; }
    }

    public long getJarSizeBytes(Plugin p) {
        try {
            File f = new File(getJarPath(p));
            return f.isFile() ? f.length() : -1L;
        } catch (Throwable t) { return -1L; }
    }

    public long getDataFolderSizeBytes(Plugin p) {
        File dir = p.getDataFolder();
        return dir != null ? folderSize(dir) : -1L;
    }

    public int getDataFolderFileCount(Plugin p) {
        File dir = p.getDataFolder();
        return dir != null ? fileCount(dir) : 0;
    }

    private long folderSize(File f) {
        if (f == null || !f.exists()) return 0L;
        if (f.isFile()) return f.length();
        long total = 0L;
        File[] list = f.listFiles();
        if (list == null) return 0L;
        for (File c : list) total += folderSize(c);
        return total;
    }

    private int fileCount(File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return 1;
        int total = 0;
        File[] list = f.listFiles();
        if (list == null) return 0;
        for (File c : list) total += fileCount(c);
        return total;
    }

    public int getListenerCount(Plugin p) {
        try {
            int count = 0;
            for (org.bukkit.plugin.RegisteredListener l : org.bukkit.event.HandlerList.getRegisteredListeners(p)) {
                if (l.getPlugin() == p) count++;
            }
            return count;
        } catch (Throwable t) { return 0; }
    }

    public List<String> getListenerClassNames(Plugin p) {
        List<String> names = new ArrayList<>();
        try {
            for (org.bukkit.plugin.RegisteredListener l : org.bukkit.event.HandlerList.getRegisteredListeners(p)) {
                if (l.getPlugin() == p) {
                    String n = l.getListener().getClass().getSimpleName();
                    if (!n.isEmpty() && !names.contains(n)) names.add(n);
                }
            }
        } catch (Throwable ignored) {}
        return names;
    }

    public int getTaskCount(Plugin p) {
        try {
            return (int) Bukkit.getScheduler().getPendingTasks().stream().filter(t -> t.getOwner() == p).count();
        } catch (Throwable t) { return 0; }
    }

    public List<Integer> getTaskIds(Plugin p) {
        List<Integer> ids = new ArrayList<>();
        try {
            Bukkit.getScheduler().getPendingTasks().forEach(t -> { if (t.getOwner() == p) ids.add(t.getTaskId()); });
        } catch (Throwable ignored) {}
        return ids;
    }

    public List<Plugin> getDependents(Plugin target) {
        List<Plugin> dependents = new ArrayList<>();
        if (target == null) return dependents;
        String name = target.getName();
        for (Plugin p : pluginManager.getPlugins()) {
            if (p == target) continue;
            try {
                List<String> dep = p.getDescription().getDepend();
                List<String> soft = p.getDescription().getSoftDepend();
                if ((dep != null && dep.stream().anyMatch(d -> d.equalsIgnoreCase(name))) ||
                    (soft != null && soft.stream().anyMatch(d -> d.equalsIgnoreCase(name)))) {
                    dependents.add(p);
                }
            } catch (Throwable ignored) {}
        }
        return dependents;
    }

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
                player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getValue("config.yml", "plugin-manager.cannot-disable-core",
                        "<prefix><red>Cannot disable core system plugin</red><dark_gray>: </dark_gray><aqua>{plugin}</aqua>")
                        .replace("{plugin}", target.getName())
                ));
            }
            return false;
        }

        boolean blockIfDependents = plugin.getConfigManager().getValue("config.yml", "plugin-manager.settings.block-disable-with-dependents", true);
        List<Plugin> dependents = getDependents(target);
        if (blockIfDependents && !dependents.isEmpty()) {
            if (player != null) {
                String list = dependents.stream().map(Plugin::getName).collect(Collectors.joining(
                    plugin.getConfigManager().getValue("config.yml", "plugin-manager.list-separator", ", ")
                ));
                player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getValue("config.yml", "plugin-manager.cannot-disable-has-dependents", 
                    "<prefix><red>Cannot disable {plugin}, dependents: {dependents}</red>")
                        .replace("{plugin}", target.getName())
                        .replace("{dependents}", list.isEmpty() ? "-" : list)
                ));
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
    
    private boolean isSystemPlugin(Plugin plugin) {
        String name = plugin.getName().toLowerCase();
        return name.equals("minecraft") || name.equals("bukkit") || 
               name.equals("spigot") || name.equals("paper") || 
               name.equals("craftbukkit") || name.equals("purpur");
    }
    
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
        // Prevent reloading the core DevSystem plugin to avoid breaking the system
        if (target.getName().equalsIgnoreCase(plugin.getName())) {
            if (player != null) {
                player.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getValue("config.yml", "plugin-manager.cannot-reload-core", 
                    "<prefix><red>Cannot reload core system plugin</red><dark_gray>: </dark_gray><aqua>{plugin}</aqua>")
                        .replace("{plugin}", target.getName())
                ));
            }
            return false;
        }
        boolean smartReload = plugin.getConfigManager().getValue("config.yml", "plugin-manager.settings.smart-reload", true);
        if (!smartReload) {
            pluginManager.disablePlugin(target);
            pluginManager.enablePlugin(target);
        } else {
            List<Plugin> dependents = getDependents(target);
            dependents.sort((a,b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (Plugin dep : dependents) {
                if (dep.isEnabled()) pluginManager.disablePlugin(dep);
            }
            pluginManager.disablePlugin(target);
            pluginManager.enablePlugin(target);
            for (Plugin dep : dependents) {
                if (!dep.isEnabled()) pluginManager.enablePlugin(dep);
            }
        }
        
        if (player != null) {
            player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-reloaded", 
                "<prefix><white>Plugin <yellow>{plugin}</yellow> has been reloaded!</white>")
                    .replace("{plugin}", target.getName())
            ));
        }
        
        return true;
    }
    
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

        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-api", "<white>API: <green>{api}</green></white>")
                .replace("{api}", getApiVersion(target))
        ));
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-load", "<white>Load: <green>{load}</green></white>")
                .replace("{load}", getLoadPhase(target))
        ));

        List<String> provides = getProvides(target);
        String providesJoined = provides.isEmpty() ? "None" : String.join(", ", provides);
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-provides", "<white>Provides: <green>{provides}</green></white>")
                .replace("{provides}", providesJoined)
        ));

        List<String> deps = target.getDescription().getDepend();
        List<String> soft = target.getDescription().getSoftDepend();
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-deps", "<white>Depends: <green>{deps}</green></white>")
                .replace("{deps}", (deps != null && !deps.isEmpty()) ? String.join(", ", deps) : "None")
        ));
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-softdeps", "<white>SoftDepends: <green>{soft}</green></white>")
                .replace("{soft}", (soft != null && !soft.isEmpty()) ? String.join(", ", soft) : "None")
        ));

        String jarPath = getJarPath(target);
        long jarSize = getJarSizeBytes(target);
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-jar", "<white>JAR: <green>{path}</green> <gray>({size} bytes)</gray></white>")
                .replace("{path}", jarPath)
                .replace("{size}", String.valueOf(Math.max(jarSize, 0)))
        ));

        File df = target.getDataFolder();
        long dfSize = getDataFolderSizeBytes(target);
        int dfCount = getDataFolderFileCount(target);
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-data", "<white>DataFolder: <green>{path}</green> <gray>({files} files, {size} bytes)</gray></white>")
                .replace("{path}", (df != null ? df.getAbsolutePath() : "-") )
                .replace("{files}", String.valueOf(dfCount))
                .replace("{size}", String.valueOf(Math.max(dfSize, 0)))
        ));

        int listeners = getListenerCount(target);
        int tasks = getTaskCount(target);
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-listeners-count", "<white>Listeners: <green>{count}</green></white>")
                .replace("{count}", String.valueOf(listeners))
        ));
        player.sendMessage(miniMessage.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-tasks-count", "<white>Tasks: <green>{count}</green></white>")
                .replace("{count}", String.valueOf(tasks))
        ));
    }
    
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