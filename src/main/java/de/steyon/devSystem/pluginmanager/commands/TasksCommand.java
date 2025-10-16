package de.steyon.devSystem.pluginmanager.commands;

import de.steyon.devSystem.DevSystem;
import de.steyon.devSystem.pluginmanager.PluginManagerService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TasksCommand implements SubCommand {

    private final DevSystem plugin;
    private final PluginManagerService service;
    private final MiniMessage mm;

    public TasksCommand(DevSystem plugin, PluginManagerService service) {
        this.plugin = plugin;
        this.service = service;
        this.mm = plugin.getMiniMessage();
    }

    @Override
    public String getName() { return "tasks"; }

    @Override
    public String getDescription() {
        return plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-help.tasks", "Shows scheduled tasks for a plugin");
    }

    @Override
    public String getPermission() { return "devsystem.pluginmanager.tasks"; }

    @Override
    public List<String> getAliases() {
        List<String> a = new ArrayList<>();
        a.add("t");
        return a;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            String usage = plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-usage.tasks", "/plugmanager tasks <plugin>");
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
        int count = service.getTaskCount(target);
        List<Integer> ids = service.getTaskIds(target);
        String idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.list-separator", ", ")
        ));
        player.sendMessage(mm.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.tasks-header", "<aqua><bold>Tasks</bold></aqua> <gray>for</gray> <green>{plugin}</green>")
                .replace("{plugin}", target.getName())
        ));
        player.sendMessage(mm.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.tasks-count", "<white>Total: <green>{count}</green></white>")
                .replace("{count}", String.valueOf(count))
        ));
        player.sendMessage(mm.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.tasks-ids", "<white>IDs: <green>{ids}</green></white>")
                .replace("{ids}", idsStr.isEmpty() ? "None" : idsStr)
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
