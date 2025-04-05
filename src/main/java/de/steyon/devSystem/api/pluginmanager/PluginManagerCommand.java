package de.steyon.devSystem.api.pluginmanager;

import de.steyon.devSystem.DevSystem;
import de.steyon.devSystem.api.pluginmanager.commands.EnableCommand;
import de.steyon.devSystem.api.pluginmanager.commands.DisableCommand;
import de.steyon.devSystem.api.pluginmanager.commands.InfoCommand;
import de.steyon.devSystem.api.pluginmanager.commands.ListCommand;
import de.steyon.devSystem.api.pluginmanager.commands.ReloadCommand;
import de.steyon.devSystem.api.pluginmanager.commands.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class PluginManagerCommand implements CommandExecutor, TabCompleter {

    private final DevSystem plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final PluginManagerService pluginManagerService;

    public PluginManagerCommand(DevSystem plugin, PluginManagerService pluginManagerService) {
        this.plugin = plugin;
        this.pluginManagerService = pluginManagerService;
        
        registerSubCommands();
    }
    
    private void registerSubCommands() {
        registerSubCommand(new ListCommand(plugin, pluginManagerService));
        registerSubCommand(new InfoCommand(plugin, pluginManagerService));
        registerSubCommand(new EnableCommand(plugin, pluginManagerService));
        registerSubCommand(new DisableCommand(plugin, pluginManagerService));
        registerSubCommand(new ReloadCommand(plugin, pluginManagerService));
    }
    
    private void registerSubCommand(SubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
        for (String alias : command.getAliases()) {
            subCommands.put(alias.toLowerCase(), command);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage(
                plugin.getConfigManager().getValue("config.yml", "messages.command.player-only", 
                "<prefix><red>This command can only be executed by a player!</red>")
            ));
            return true;
        }

        if (!player.hasPermission("devsystem.pluginmanager")) {
            sender.sendMessage(plugin.getConfigManager().getMessage(
                plugin.getConfigManager().getValue("config.yml", "messages.no-permission", 
                "<prefix><red>You don't have permission to use this command!</red>")
            ));
            return true;
        }
        
        if (args.length == 0) {
            pluginManagerService.openPluginManagerGUI(player);
            return true;
        }
        
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        
        if (subCommand == null) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                plugin.getConfigManager().getValue("config.yml", "messages.command.unknown", 
                "<prefix><red>Unknown command. Type /help for help.</red>")
            ));
            sendHelp(player);
            return true;
        }
        
        if (!player.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(plugin.getConfigManager().getMessage(
                plugin.getConfigManager().getValue("config.yml", "messages.no-permission", 
                "<prefix><red>You don't have permission to use this command!</red>")
            ));
            return true;
        }
        
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(player, subArgs);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player player)) {
            return completions;
        }
        
        if (!player.hasPermission("devsystem.pluginmanager")) {
            return completions;
        }
        
        if (args.length == 1) {
            for (SubCommand subCommand : new ArrayList<>(new HashSet<>(subCommands.values()))) {
                if (player.hasPermission(subCommand.getPermission()) && 
                    !completions.contains(subCommand.getName())) {
                    completions.add(subCommand.getName());
                }
            }
            return filterCompletions(completions, args[0]);
        }
        
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        
        if (subCommand != null && player.hasPermission(subCommand.getPermission())) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            List<String> subCompletions = subCommand.tabComplete(player, subArgs);
            if (subCompletions != null) {
                return filterCompletions(subCompletions, args[args.length - 1]);
            }
        }
        
        return completions;
    }
    
    private List<String> filterCompletions(List<String> completions, String partialArg) {
        String lowerPartialArg = partialArg.toLowerCase();
        List<String> filteredCompletions = new ArrayList<>();
        
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(lowerPartialArg)) {
                filteredCompletions.add(completion);
            }
        }
        
        return filteredCompletions;
    }
    
    private void sendHelp(Player player) {
        MiniMessage miniMessage = plugin.getMiniMessage();
        
        String helpTitle = plugin.getConfigManager().getValue("config.yml", "plugin-manager.help-title", "<gradient:green:blue>Plugin Manager Help</gradient>");
        player.sendMessage(miniMessage.deserialize(helpTitle));
        player.sendMessage(Component.empty());
        
        for (SubCommand subCommand : new ArrayList<>(new HashSet<>(subCommands.values()))) {
            if (player.hasPermission(subCommand.getPermission())) {
                player.sendMessage(miniMessage.deserialize("<green>/plugmanager " + subCommand.getName() + "</green> - " + subCommand.getDescription()));
            }
        }
    }
} 