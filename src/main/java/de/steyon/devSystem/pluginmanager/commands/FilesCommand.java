package de.steyon.devSystem.pluginmanager.commands;

import de.steyon.devSystem.DevSystem;
import de.steyon.devSystem.pluginmanager.PluginManagerService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FilesCommand implements SubCommand {

    private final DevSystem plugin;
    private final PluginManagerService service;
    private final MiniMessage mm;

    public FilesCommand(DevSystem plugin, PluginManagerService service) {
        this.plugin = plugin;
        this.service = service;
        this.mm = plugin.getMiniMessage();
    }

    @Override
    public String getName() { return "files"; }

    @Override
    public String getDescription() {
        return plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-help.files", "Shows data folder and jar info for a plugin");
    }

    @Override
    public String getPermission() { return "devsystem.pluginmanager.files"; }

    @Override
    public List<String> getAliases() {
        List<String> a = new ArrayList<>();
        a.add("f");
        return a;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            String usage = plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-usage.files", "/plugmanager files <plugin>");
            player.sendMessage(mm.deserialize(
                plugin.getConfigManager().getValue("config.yml", "messages.command.usage", "<prefix><red>Usage: {usage}</red>")
                    .replace("{usage}", usage)
            ));
            return;
        }
        String name = args[0];
        Plugin target = service.getPlugin(name);
        if (target == null) {
            player.sendMessage(mm.deserialize(
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-not-found", "<prefix><red>Plugin not found: {plugin}</red>")
                    .replace("{plugin}", name)
            ));
            return;
        }
        String jarPath = service.getJarPath(target);
        long jarSize = service.getJarSizeBytes(target);
        File df = target.getDataFolder();
        long dfSize = service.getDataFolderSizeBytes(target);
        int dfCount = service.getDataFolderFileCount(target);

        player.sendMessage(mm.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.files-header", "<aqua><bold>Files</bold></aqua> <gray>for</gray> <green>{plugin}</green>")
                .replace("{plugin}", target.getName())
        ));
        player.sendMessage(mm.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-jar", "<white>JAR: <green>{path}</green> <gray>({size} bytes)</gray></white>")
                .replace("{path}", jarPath)
                .replace("{size}", String.valueOf(Math.max(jarSize, 0)))
        ));
        player.sendMessage(mm.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-data", "<white>DataFolder: <green>{path}</green> <gray>({files} files, {size} bytes)</gray></white>")
                .replace("{path}", (df != null ? df.getAbsolutePath() : "-"))
                .replace("{files}", String.valueOf(dfCount))
                .replace("{size}", String.valueOf(Math.max(dfSize, 0)))
        ));
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return service.getPlugins().stream().map(Plugin::getName).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
