package de.steyon.devSystem;

import de.steyon.devSystem.api.inv.controller.InventoryManager;
import de.steyon.devSystem.config.Config;
import de.steyon.devSystem.pluginmanager.PluginManagerCommand;
import de.steyon.devSystem.pluginmanager.PluginManagerService;
import lombok.Getter;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public final class DevSystem extends JavaPlugin {

    @Getter
    private InventoryManager inventoryManager;
    
    @Getter
    private Config configManager;
    @Getter
    private MiniMessage miniMessage;
    
    @Getter
    private PluginManagerService pluginManagerService;

    @Override
    public void onEnable() {
        this.configManager = new Config(this);
        
        getServer().getConsoleSender().sendMessage(this.configManager.getPluginMessage("starting"));
        getServer().getConsoleSender().sendMessage(this.configManager.getPluginMessage("config-loaded"));
        
        this.inventoryManager = new InventoryManager(this, new HashMap<>());
        this.miniMessage = MiniMessage.builder().postProcessor(postProcessor -> postProcessor.decoration(TextDecoration.ITALIC, false)).build();
        
        this.pluginManagerService = new PluginManagerService(this);

        registerCommands();
        checkForUpdates();
        
        getServer().getConsoleSender().sendMessage(this.configManager.getPluginMessage("enabled"));
    }
    
    private void registerCommands() {
        PluginManagerCommand pluginManagerCommand = new PluginManagerCommand(this, this.pluginManagerService);
        getCommand("plugmanager").setExecutor(pluginManagerCommand);
        getCommand("plugmanager").setTabCompleter(pluginManagerCommand);
    }
    
    private void checkForUpdates() {
        getServer().getConsoleSender().sendMessage(this.configManager.getPluginMessage("checking-updates"));
    }
    

    @Override
    public void onDisable() {
        if (this.configManager != null) {
            getServer().getConsoleSender().sendMessage(this.configManager.getPluginMessage("disabled"));
        }
    }
}
