package de.steyon.devSystem.api.pluginmanager.commands;

import de.steyon.devSystem.DevSystem;
import de.steyon.devSystem.api.pluginmanager.PluginManagerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InfoCommand implements SubCommand {

    private final DevSystem plugin;
    private final PluginManagerService service;
    private final MiniMessage miniMessage;
    
    public InfoCommand(DevSystem plugin, PluginManagerService service) {
        this.plugin = plugin;
        this.service = service;
        this.miniMessage = plugin.getMiniMessage();
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-help.info", "Shows information about a plugin");
    }

    @Override
    public String getPermission() {
        return "devsystem.pluginmanager.info";
    }

    @Override
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("i");
        aliases.add("show");
        return aliases;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            String usage = plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-usage.info", "/plugmanager info <plugin>");
            player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getValue("config.yml", "messages.command.usage", "<prefix><red>Usage: {usage}</red>")
                    .replace("{usage}", usage)
            ));
            return;
        }
        
        String pluginName = args[0];
        Plugin target = service.getPlugin(pluginName);
        
        if (target == null) {
            player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-not-found", "<prefix><red>Plugin not found: {plugin}</red>")
                    .replace("{plugin}", pluginName)
            ));
            return;
        }
        
        service.showPluginInfo(target, player);
    }
    
    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return service.getPlugins().stream()
                .map(Plugin::getName)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
} 