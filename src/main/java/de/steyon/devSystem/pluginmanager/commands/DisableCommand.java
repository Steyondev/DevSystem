package de.steyon.devSystem.pluginmanager.commands;

import de.steyon.devSystem.DevSystem;
import de.steyon.devSystem.pluginmanager.PluginManagerService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DisableCommand implements SubCommand {

    private final DevSystem plugin;
    private final PluginManagerService service;
    private final MiniMessage miniMessage;
    
    public DisableCommand(DevSystem plugin, PluginManagerService service) {
        this.plugin = plugin;
        this.service = service;
        this.miniMessage = plugin.getMiniMessage();
    }

    @Override
    public String getName() {
        return "disable";
    }

    @Override
    public String getDescription() {
        return plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-help.disable", "Disables a plugin");
    }

    @Override
    public String getPermission() {
        return "devsystem.pluginmanager.disable";
    }

    @Override
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("off");
        return aliases;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            String usage = plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-usage.disable", "/plugmanager disable <plugin>");
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
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-already-disabled", "<prefix><red>Plugin is already disabled: {plugin}</red>")
                    .replace("{plugin}", pluginName)
            ));
            return;
        }
        
        if (target.equals(plugin)) {
            player.sendMessage(miniMessage.deserialize(
                plugin.getConfigManager().getValue("config.yml", "plugin-manager.cannot-disable-self", 
                "<gradient:green:blue>DevSystem</gradient> <dark_gray>»</dark_gray> <red>You cannot disable this plugin!</red>")
            ));
            return;
        }
        
        service.disablePlugin(target, player);
    }
    
    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            return service.getPlugins().stream()
                .filter(Plugin::isEnabled)
                .filter(p -> !p.equals(plugin)) // Don't suggest disabling DevSystem
                .map(Plugin::getName)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
} 