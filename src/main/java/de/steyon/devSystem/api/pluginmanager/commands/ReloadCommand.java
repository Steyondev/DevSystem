package de.steyon.devSystem.api.pluginmanager.commands;

import de.steyon.devSystem.DevSystem;
import de.steyon.devSystem.api.pluginmanager.PluginManagerService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReloadCommand implements SubCommand {

    private final DevSystem plugin;
    private final PluginManagerService service;
    private final MiniMessage miniMessage;
    
    public ReloadCommand(DevSystem plugin, PluginManagerService service) {
        this.plugin = plugin;
        this.service = service;
        this.miniMessage = plugin.getMiniMessage();
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-help.reload", "Reloads a plugin");
    }

    @Override
    public String getPermission() {
        return "devsystem.pluginmanager.reload";
    }

    @Override
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("rl");
        aliases.add("restart");
        return aliases;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            String usage = plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-usage.reload", "/plugmanager reload <plugin>");
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
        
        if (!target.isEnabled()) {
            player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-not-enabled", 
                "<prefix><red>Plugin is not enabled!</red>")
            ));
            return;
        }
        
        service.reloadPlugin(target, player);
    }
    
    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return service.getPlugins().stream()
                .filter(Plugin::isEnabled)
                .map(Plugin::getName)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
} 