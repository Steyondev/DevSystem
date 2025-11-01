package de.steyon.devSystem.pluginmanager.commands;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public interface SubCommand {
    
    String getName();
    
    String getDescription();
    
    String getPermission();
    
    List<String> getAliases();
    
    void execute(Player player, String[] args);
    
    default List<String> tabComplete(Player player, String[] args) {
        return new ArrayList<>();
    }
} 