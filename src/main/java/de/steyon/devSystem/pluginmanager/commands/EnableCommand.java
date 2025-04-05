package de.steyon.devSystem.pluginmanager.commands;

import de.steyon.devSystem.DevSystem;
import de.steyon.devSystem.pluginmanager.PluginManagerService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnableCommand implements SubCommand {

    private final DevSystem plugin;
    private final PluginManagerService service;
    private final MiniMessage miniMessage;
    
    public EnableCommand(DevSystem plugin, PluginManagerService service) {
        this.plugin = plugin;
        this.service = service;
        this.miniMessage = plugin.getMiniMessage();
    }

    @Override
    public String getName() {
        return "enable";
    }

    @Override
    public String getDescription() {
        return plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-help.enable", "Enables a plugin");
    }

    @Override
    public String getPermission() {
        return "devsystem.pluginmanager.enable";
    }

    @Override
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("on");
        return aliases;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            String usage = plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-usage.enable", "/plugmanager enable <plugin>");
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
        
        if (target.isEnabled()) {
            player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-already-enabled", "<prefix><red>Plugin is already enabled: {plugin}</red>")
                    .replace("{plugin}", pluginName)
            ));
            return;
        }
        
        service.enablePlugin(target, player);
    }
    
    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return service.getPlugins().stream()
                .filter(p -> !p.isEnabled())
                .map(Plugin::getName)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
} 