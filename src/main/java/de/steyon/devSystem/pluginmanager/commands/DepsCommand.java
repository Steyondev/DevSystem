package de.steyon.devSystem.pluginmanager.commands;

import de.steyon.devSystem.DevSystem;
import de.steyon.devSystem.pluginmanager.PluginManagerService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DepsCommand implements SubCommand {

    private final DevSystem plugin;
    private final PluginManagerService service;
    private final MiniMessage mm;

    public DepsCommand(DevSystem plugin, PluginManagerService service) {
        this.plugin = plugin;
        this.service = service;
        this.mm = plugin.getMiniMessage();
    }

    @Override
    public String getName() { return "deps"; }

    @Override
    public String getDescription() {
        return plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-help.deps", "Shows dependencies and dependents for a plugin");
    }

    @Override
    public String getPermission() { return "devsystem.pluginmanager.deps"; }

    @Override
    public List<String> getAliases() {
        List<String> a = new ArrayList<>();
        a.add("dep");
        return a;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            String usage = plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-usage.deps", "/plugmanager deps <plugin>");
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
        List<String> deps = target.getDescription().getDepend();
        List<String> soft = target.getDescription().getSoftDepend();
        List<Plugin> dependents = service.getDependents(target);

        String sep = plugin.getConfigManager().getValue("config.yml", "plugin-manager.list-separator", ", ");
        String depsStr = (deps != null && !deps.isEmpty()) ? String.join(sep, deps) : "None";
        String softStr = (soft != null && !soft.isEmpty()) ? String.join(sep, soft) : "None";
        String dependentsStr = dependents.isEmpty() ? "None" : dependents.stream().map(Plugin::getName).collect(Collectors.joining(sep));

        player.sendMessage(mm.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.deps-header", "<aqua><bold>Dependencies</bold></aqua> <gray>for</gray> <green>{plugin}</green>")
                .replace("{plugin}", target.getName())
        ));
        player.sendMessage(mm.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-deps", "<white>Depends: <green>{deps}</green></white>")
                .replace("{deps}", depsStr)
        ));
        player.sendMessage(mm.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.info-softdeps", "<white>SoftDepends: <green>{soft}</green></white>")
                .replace("{soft}", softStr)
        ));
        player.sendMessage(mm.deserialize(
            plugin.getConfigManager().getValue("config.yml", "plugin-manager.deps-reverse", "<white>Dependents: <green>{dependents}</green></white>")
                .replace("{dependents}", dependentsStr)
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
