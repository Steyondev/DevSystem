package de.steyon.devSystem.pluginmanager.commands;

import de.steyon.devSystem.DevSystem;
import de.steyon.devSystem.pluginmanager.PluginManagerService;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ListCommand implements SubCommand {

    private final DevSystem plugin;
    private final PluginManagerService service;
    
    public ListCommand(DevSystem plugin, PluginManagerService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return plugin.getConfigManager().getValue("config.yml", "plugin-manager.command-help.list", "Lists all plugins on the server");
    }

    @Override
    public String getPermission() {
        return "devsystem.pluginmanager.list";
    }

    @Override
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("ls");
        return aliases;
    }

    @Override
    public void execute(Player player, String[] args) {
        service.listPlugins(player);
    }
} 