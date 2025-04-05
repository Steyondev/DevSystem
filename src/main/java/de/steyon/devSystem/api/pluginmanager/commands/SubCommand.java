package de.steyon.devSystem.api.pluginmanager.commands;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public interface SubCommand {
    
    /**
     * Gets the name of the subcommand
     * @return The subcommand name
     */
    String getName();
    
    /**
     * Gets the description of the subcommand
     * @return The subcommand description
     */
    String getDescription();
    
    /**
     * Gets the permission needed to execute this subcommand
     * @return The permission string
     */
    String getPermission();
    
    /**
     * Gets the aliases for this subcommand
     * @return List of aliases
     */
    List<String> getAliases();
    
    /**
     * Executes the subcommand
     * @param player The player executing the command
     * @param args The arguments passed to the subcommand
     */
    void execute(Player player, String[] args);
    
    /**
     * Provides tab completions for this subcommand
     * @param player The player requesting tab completions
     * @param args The arguments passed to the subcommand
     * @return List of tab completions
     */
    default List<String> tabComplete(Player player, String[] args) {
        return new ArrayList<>();
    }
} 