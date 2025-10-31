package de.steyon.devSystem.pluginmanager.commands;

import de.steyon.devSystem.DevSystem;
import de.steyon.devSystem.pluginmanager.PluginManagerService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

public class LoadCommand implements SubCommand {

    private final DevSystem plugin;
    private final PluginManagerService service;
    private final MiniMessage mm;

    public LoadCommand(DevSystem plugin, PluginManagerService service) {
        this.plugin = plugin;
        this.service = service;
        this.mm = plugin.getMiniMessage();
    }

    @Override
    public String getName() {
        return "load";
    }

    @Override
    public String getDescription() {
        return plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-help.load", "<gray>Loads a plugin jar at runtime");
    }

    @Override
    public String getPermission() {
        return "devsystem.pluginmanager.load";
    }

    @Override
    public List<String> getAliases() {
        return new ArrayList<>();
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(mm.deserialize(
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-usage.load", "<dark_gray>» <gray>/plugmanager load <blue><path-or-url> [sources...]")
            ));
            return;
        }

        String source = args[0];
        try {
            boolean allowUrl = plugin.getConfigManager().getValue("config.yml", "plugin-manager.settings.load-allow-url", true);
            boolean allowLocal = plugin.getConfigManager().getValue("config.yml", "plugin-manager.settings.load-allow-local", true);
            boolean blockMissingDeps = plugin.getConfigManager().getValue("config.yml", "plugin-manager.settings.load-block-missing-deps", true);

            File pluginsDir = plugin.getDataFolder().getParentFile();
            if (pluginsDir == null) pluginsDir = new File("plugins");

            File jar;
            if (isUrl(source)) {
                if (!allowUrl) {
                    player.sendMessage(mm.deserialize(
                        plugin.getConfigManager().getValue("config.yml", "plugin-manager.load-url-not-allowed", "<prefix><red>URL loading is disabled in settings</red>")
                    ));
                    return;
                }
                // Download to plugins folder
                String name = "downloaded-" + System.currentTimeMillis() + ".jar";
                jar = new File(pluginsDir, name);
                downloadToFile(source, jar.toPath());
            } else {
                if (!allowLocal) {
                    player.sendMessage(mm.deserialize(
                        plugin.getConfigManager().getValue("config.yml", "plugin-manager.load-local-not-allowed", "<prefix><red>Local file loading is disabled in settings</red>")
                    ));
                    return;
                }
                // Interpret as local path (relative to plugins folder if not absolute)
                File candidate = new File(source);
                if (!candidate.isAbsolute()) {
                    candidate = new File(pluginsDir, source);
                }
                jar = candidate;
            }

            if (jar == null || !jar.exists()) {
                player.sendMessage(mm.deserialize(
                    plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-not-found", "<prefix><red>Plugin not found</red><dark_gray>: {plugin}")
                        .replace("{plugin}", source)
                ));
                return;
            }

            // Ensure file is in plugins dir (some servers require this)
            if (!jar.getParentFile().equals(pluginsDir)) {
                File moved = new File(pluginsDir, jar.getName());
                Files.copy(jar.toPath(), moved.toPath(), StandardCopyOption.REPLACE_EXISTING);
                jar = moved;
            }

            // Prepare additional sources (for auto-resolve)
            Map<String, File> candidateByName = new HashMap<>();
            // include main jar's meta
            String mainName = safeGetPluginName(jar);
            if (mainName != null) candidateByName.put(mainName.toLowerCase(), jar);

            if (args.length > 1) {
                for (int i = 1; i < args.length; i++) {
                    String src = args[i];
                    File f;
                    if (isUrl(src)) {
                        if (!allowUrl) continue;
                        String name = "downloaded-" + System.currentTimeMillis() + "-" + i + ".jar";
                        f = new File(pluginsDir, name);
                        try { downloadToFile(src, f.toPath()); } catch (Exception ignored) { continue; }
                    } else {
                        if (!allowLocal) continue;
                        f = new File(src);
                        if (!f.isAbsolute()) f = new File(pluginsDir, src);
                    }
                    if (f.exists() && f.isFile()) {
                        String n = safeGetPluginName(f);
                        if (n != null) candidateByName.putIfAbsent(n.toLowerCase(), ensureInPluginsDir(f, pluginsDir));
                    }
                }
            }

            // Resolve hard depends order (load dependencies first)
            List<File> loadOrder = resolveLoadOrder(jar, candidateByName);

            // If blocking missing deps and some cannot be resolved, abort
            if (blockMissingDeps) {
                List<String> missing = getMissingDependenciesConsideringCandidates(jar, candidateByName);
                if (!missing.isEmpty()) {
                    String sep = plugin.getConfigManager().getValue("config.yml", "plugin-manager.list-separator", ", ");
                    player.sendMessage(mm.deserialize(
                        plugin.getConfigManager().getValue("config.yml", "plugin-manager.load-missing-deps", "<prefix><red>Missing required dependencies</red><dark_gray>: </dark_gray><aqua>{deps}</aqua>")
                            .replace("{deps}", missing.stream().collect(Collectors.joining(sep)))
                    ));
                    return;
                }
            }

            // Load in order, skipping those already installed/enabled
            for (File f : loadOrder) {
                String n = safeGetPluginName(f);
                if (n != null && hasPlugin(n)) continue; // already installed
                service.loadPluginFromJar(f, player);
            }

            var loaded = hasPlugin(mainName != null ? mainName : "") ? null : service.loadPluginFromJar(jar, player);
            if (loaded != null) {
                player.sendMessage(mm.deserialize(
                    plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-enabled", "<prefix><white>Plugin <green>{plugin}</green> has been enabled!</white>")
                        .replace("{plugin}", loaded.getName())
                ));
            }
        } catch (Exception ex) {
            player.sendMessage(mm.deserialize(
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.load-exception", "<prefix><red>Exception while loading plugin</red><dark_gray>: </dark_gray><aqua>{plugin}</aqua>")
                    .replace("{plugin}", source)
            ));
        }
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            // Suggest local .jar files in plugins directory
            File pluginsDir = plugin.getDataFolder().getParentFile();
            if (pluginsDir == null) pluginsDir = new File("plugins");
            File[] files = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (files != null) {
                for (File f : files) list.add(f.getName());
            }
        }
        return list;
    }

    private static boolean isUrl(String s) {
        String lower = s.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static void downloadToFile(String urlStr, Path target) throws Exception {
        var url = URI.create(urlStr).toURL();
        try (InputStream in = url.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean hasPlugin(String name) {
        for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
            if (p.getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private String safeGetPluginName(File jar) {
        try (JarFile jf = new JarFile(jar)) {
            JarEntry entry = jf.getJarEntry("plugin.yml");
            if (entry == null) return null;
            try (InputStream is = jf.getInputStream(entry)) {
                PluginDescriptionFile desc = new PluginDescriptionFile(is);
                return desc.getName();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private File ensureInPluginsDir(File f, File pluginsDir) {
        if (f.getParentFile().equals(pluginsDir)) return f;
        try {
            File moved = new File(pluginsDir, f.getName());
            Files.copy(f.toPath(), moved.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return moved;
        } catch (Exception e) {
            return f;
        }
    }

    private List<File> resolveLoadOrder(File mainJar, Map<String, File> candidates) {
        List<File> order = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        dfsAdd(mainJar, candidates, order, visiting, visited);
        return order;
    }

    private void dfsAdd(File jar, Map<String, File> candidates, List<File> order, Set<String> visiting, Set<String> visited) {
        String name = safeGetPluginName(jar);
        if (name == null) return;
        String key = name.toLowerCase();
        if (visited.contains(key)) return;
        if (visiting.contains(key)) return; // cycle guard
        visiting.add(key);
        // add deps first
        for (String dep : getDependsList(jar)) {
            if (hasPlugin(dep)) continue;
            File depJar = candidates.get(dep.toLowerCase());
            if (depJar != null) dfsAdd(depJar, candidates, order, visiting, visited);
        }
        visiting.remove(key);
        visited.add(key);
        if (!order.contains(jar)) order.add(jar);
    }

    private List<String> getDependsList(File jar) {
        try (JarFile jf = new JarFile(jar)) {
            JarEntry entry = jf.getJarEntry("plugin.yml");
            if (entry == null) return List.of();
            try (InputStream is = jf.getInputStream(entry)) {
                PluginDescriptionFile desc = new PluginDescriptionFile(is);
                List<String> d = desc.getDepend();
                return d != null ? d : List.of();
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> getMissingDependenciesConsideringCandidates(File jar, Map<String, File> candidates) {
        List<String> missing = new ArrayList<>();
        for (String dep : getDependsList(jar)) {
            if (hasPlugin(dep)) continue;
            if (!candidates.containsKey(dep.toLowerCase())) missing.add(dep);
        }
        return missing;
    }
}
