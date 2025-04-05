package de.steyon.devSystem.pluginmanager;

import de.steyon.devSystem.DevSystem;
import de.steyon.devSystem.api.inv.SteyOnInv;
import de.steyon.devSystem.api.inv.controller.PageHandler;
import de.steyon.devSystem.api.inv.item.ActiveItem;
import de.steyon.devSystem.api.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginManagerGUI {

    private final DevSystem plugin;
    private final PluginManagerService service;
    private final MiniMessage miniMessage;
    private final Map<SteyOnInv, PageHandler> pageHandlers = new HashMap<>();

    public PluginManagerGUI(DevSystem plugin, PluginManagerService service) {
        this.plugin = plugin;
        this.service = service;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void openMainGUI(Player player) {
        String title = plugin.getConfigManager().getValue("config.yml", "plugin-manager.main-gui-title", "<gradient:green:blue>Plugin Manager</gradient>");
        Component titleComponent = miniMessage.deserialize(title);
        
        String[] pattern = {
            "XXXXXXXXX",
            "XPPPPPPPX",
            "XPPPPPPPX",
            "XPPPPPPPX",
            "XPPPPPPPX",
            "XXXXXXXXX",
        };
        
        List<ActiveItem> pluginItems = createPluginItems(player);
        
        int itemsPerPage = 28;
        int totalPages = (int) Math.ceil((double) pluginItems.size() / itemsPerPage);
        
        if (totalPages > 1) {
            pattern[5] = "XXX<X>XXX";
        }
        
        SteyOnInv gui = new SteyOnInv(plugin, 9 * 6, titleComponent, player, pattern);
        
        ItemBuilder borderItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(Component.empty())
            .clearAllAttributes();
            
        ActiveItem border = new ActiveItem(borderItem.build())
            .click(e -> e.setCancelled(true));
            
        HashMap<Character, ActiveItem> staticItems = new HashMap<>();
        staticItems.put('X', border);
        
        if (totalPages > 1) {
            String prevPageText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.prev-page", "<blue>Previous Page</blue>");
            String nextPageText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.next-page", "<blue>Next Page</blue>");
            
            ItemBuilder prevPageItem = new ItemBuilder(Material.ARROW)
                .name(miniMessage.deserialize(prevPageText))
                .clearAllAttributes();
                
            ItemBuilder nextPageItem = new ItemBuilder(Material.ARROW)
                .name(miniMessage.deserialize(nextPageText))
                .clearAllAttributes();
                
            ActiveItem prevPage = new ActiveItem(prevPageItem.build()).click(this::handlePrevPageClick);
            ActiveItem nextPage = new ActiveItem(nextPageItem.build()).click(this::handleNextPageClick);
            
            staticItems.put('<', prevPage);
            staticItems.put('>', nextPage);
        }
        
        gui.setUnActiveItems(staticItems);
        
        PageHandler pageHandler = new PageHandler(gui, itemsPerPage, pluginItems, 'P');
        pageHandlers.put(gui, pageHandler);
        
        gui.onClose(event -> {
            pageHandlers.remove(gui);
        });
        
        gui.open(player);
    }
    
    private void handlePrevPageClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getInventory().getHolder() == null) {
            SteyOnInv inv = findInventoryByClickEvent(event);
            if (inv != null) {
                PageHandler pageHandler = pageHandlers.get(inv);
                if (pageHandler != null && pageHandler.hasPreviousPage()) {
                    pageHandler.previousPage();
                }
            }
        }
    }
    
    private void handleNextPageClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getInventory().getHolder() == null) {
            SteyOnInv inv = findInventoryByClickEvent(event);
            if (inv != null) {
                PageHandler pageHandler = pageHandlers.get(inv);
                if (pageHandler != null && pageHandler.hasNextPage()) {
                    pageHandler.nextPage();
                }
            }
        }
    }
    
    private SteyOnInv findInventoryByClickEvent(InventoryClickEvent event) {
        for (SteyOnInv inv : pageHandlers.keySet()) {
            if (inv.getInventory().equals(event.getInventory())) {
                return inv;
            }
        }
        return null;
    }
    
    private List<ActiveItem> createPluginItems(Player player) {
        List<ActiveItem> items = new ArrayList<>();
        
        for (Plugin plugin : service.getPlugins()) {
            String nameText = this.plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-name", "<gradient:green:blue>{plugin}</gradient>")
                .replace("{plugin}", plugin.getName());
                
            String versionText = this.plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-version", "<gray>Version: <green>{version}</green>")
                .replace("{version}", plugin.getDescription().getVersion());
                
            String enabledText = this.plugin.getConfigManager().getValue("config.yml", "plugin-manager.status-enabled", "<green>Enabled</green>");
            String disabledText = this.plugin.getConfigManager().getValue("config.yml", "plugin-manager.status-disabled", "<red>Disabled</red>");
            
            String statusText = this.plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-status", "<gray>Status: {status}")
                .replace("{status}", plugin.isEnabled() ? enabledText : disabledText);
                
            String clickText = this.plugin.getConfigManager().getValue("config.yml", "plugin-manager.click-to-manage", "<dark_gray>Click to manage this plugin</dark_gray>");
            
            ItemBuilder builder = new ItemBuilder(getPluginMaterial(plugin))
                .name(miniMessage.deserialize(nameText))
                .lore(
                    miniMessage.deserialize(versionText),
                    miniMessage.deserialize(statusText),
                    miniMessage.deserialize(clickText)
                )
                .clearAllAttributes();
                
            ActiveItem item = new ActiveItem(builder.build())
                .click(e -> {
                    e.setCancelled(true);
                    openPluginDetailsGUI(player, plugin);
                });
                
            items.add(item);
        }
        
        return items;
    }
    
    private Material getPluginMaterial(Plugin plugin) {
        if (!plugin.isEnabled()) {
            return Material.RED_WOOL;
        }
        return Material.GREEN_WOOL;
    }
    
    public void openPluginDetailsGUI(Player player, Plugin targetPlugin) {
        String title = plugin.getConfigManager().getValue("config.yml", "plugin-manager.details-gui-title", "<gradient:green:blue>Plugin: {plugin}</gradient>")
            .replace("{plugin}", targetPlugin.getName());
        Component titleComponent = miniMessage.deserialize(title);
        
        String[] pattern = {
            "XXXXXXXXX",
            "XXX I XXX",
            "XXRXDXEXX",
            "XXXXXXXXB",
        };
        
        SteyOnInv gui = new SteyOnInv(plugin, 9 * 4, titleComponent, player, pattern);
        
        String pluginNameText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-name", "<gradient:green:blue>{plugin}</gradient>")
            .replace("{plugin}", targetPlugin.getName());
            
        String versionText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-version", "<gray>Version: <green>{version}</green>")
            .replace("{version}", targetPlugin.getDescription().getVersion());
            
        String authorName = "Unknown";
        if (targetPlugin.getDescription().getAuthors() != null && !targetPlugin.getDescription().getAuthors().isEmpty()) {
            authorName = String.join(", ", targetPlugin.getDescription().getAuthors());
        }
        
        String authorText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-author", "<gray>Author: <green>{author}</green>")
            .replace("{author}", authorName);
        
        String desc = "No description available";
        if (targetPlugin.getDescription().getDescription() != null) {
            desc = targetPlugin.getDescription().getDescription();
        }
        
        String descriptionText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-description", "<gray>Description: <green>{description}</green>")
            .replace("{description}", desc);
        
        String enabledText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.status-enabled", "<green>Enabled</green>");
        String disabledText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.status-disabled", "<red>Disabled</red>");
        
        String statusText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-status", "<gray>Status: {status}")
            .replace("{status}", targetPlugin.isEnabled() ? enabledText : disabledText);
        
        String commandsFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-commands", "<gray>Commands: <green>{commands}</green>");
        StringBuilder commandsBuilder = new StringBuilder();
        Map<String, Map<String, Object>> commands = targetPlugin.getDescription().getCommands();
        if (commands != null && !commands.isEmpty()) {
            for (String commandName : commands.keySet()) {
                if (commandsBuilder.length() > 0) {
                    commandsBuilder.append(", ");
                }
                commandsBuilder.append("/").append(commandName);
            }
        }
        
        String commandsText = commandsFormat.replace("{commands}", 
            commandsBuilder.length() > 0 ? commandsBuilder.toString() : "None");
            
        String dependenciesFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-dependencies", 
            "<gray>Dependencies: <green>{dependencies}</green>");
        StringBuilder dependenciesBuilder = new StringBuilder();
        List<String> dependencies = targetPlugin.getDescription().getDepend();
        if (dependencies != null && !dependencies.isEmpty()) {
            dependenciesBuilder.append(String.join(", ", dependencies));
        }
        
        String dependenciesText = dependenciesFormat.replace("{dependencies}", 
            dependenciesBuilder.length() > 0 ? dependenciesBuilder.toString() : "None");
            
        String softDependenciesFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-soft-dependencies", 
            "<gray>Soft Dependencies: <green>{softdependencies}</green>");
        StringBuilder softDependenciesBuilder = new StringBuilder();
        List<String> softDependencies = targetPlugin.getDescription().getSoftDepend();
        if (softDependencies != null && !softDependencies.isEmpty()) {
            softDependenciesBuilder.append(String.join(", ", softDependencies));
        }
        
        String softDependenciesText = softDependenciesFormat.replace("{softdependencies}", 
            softDependenciesBuilder.length() > 0 ? softDependenciesBuilder.toString() : "None");
            
        String websiteFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-website", 
            "<gray>Website: <green>{website}</green>");
        String website = targetPlugin.getDescription().getWebsite();
        String websiteText = websiteFormat.replace("{website}", 
            website != null && !website.isEmpty() ? website : "None");
            
        String mainClassFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-main-class", 
            "<gray>Main Class: <green>{mainclass}</green>");
        String mainClassText = mainClassFormat.replace("{mainclass}", targetPlugin.getDescription().getMain());
        
        ItemBuilder infoBuilder = new ItemBuilder(Material.BOOK)
            .name(miniMessage.deserialize(pluginNameText))
            .lore(
                miniMessage.deserialize(versionText),
                miniMessage.deserialize(authorText),
                miniMessage.deserialize(descriptionText),
                miniMessage.deserialize(statusText),
                miniMessage.deserialize(commandsText),
                miniMessage.deserialize(dependenciesText),
                miniMessage.deserialize(softDependenciesText),
                miniMessage.deserialize(websiteText),
                miniMessage.deserialize(mainClassText)
            )
            .clearAllAttributes();
            
        ActiveItem infoItem = new ActiveItem(infoBuilder.build())
            .click(e -> e.setCancelled(true));
            
        ItemBuilder borderBuilder = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(Component.empty())
            .clearAllAttributes();
            
        ActiveItem borderItem = new ActiveItem(borderBuilder.build())
            .click(e -> e.setCancelled(true));
            
        String reloadText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.reload-text", "<yellow>Reload Plugin</yellow>");
        String reloadLoreText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.reload-lore", "<dark_gray>Click to reload this plugin</dark_gray>");
        
        ItemBuilder reloadBuilder = new ItemBuilder(Material.CLOCK)
            .name(miniMessage.deserialize(reloadText))
            .lore(miniMessage.deserialize(reloadLoreText))
            .clearAllAttributes();
            
        ActiveItem reloadItem = new ActiveItem(reloadBuilder.build())
            .click(e -> {
                e.setCancelled(true);
                service.reloadPlugin(targetPlugin, player);
                openPluginDetailsGUI(player, targetPlugin);
            });
            
        String enableText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.enable-text", "<green>Enable Plugin</green>");
        String enableLoreText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.enable-lore", "<dark_gray>Click to enable this plugin</dark_gray>");
        
        ItemBuilder enableBuilder = new ItemBuilder(Material.LIME_DYE)
            .name(miniMessage.deserialize(enableText))
            .lore(miniMessage.deserialize(enableLoreText))
            .clearAllAttributes();
            
        ActiveItem enableItem = new ActiveItem(enableBuilder.build())
            .click(e -> {
                e.setCancelled(true);
                service.enablePlugin(targetPlugin, player);
                openPluginDetailsGUI(player, targetPlugin);
            });
            
        String disableText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.disable-text", "<red>Disable Plugin</red>");
        String disableLoreText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.disable-lore", "<dark_gray>Click to disable this plugin</dark_gray>");
        
        ItemBuilder disableBuilder = new ItemBuilder(Material.RED_DYE)
            .name(miniMessage.deserialize(disableText))
            .lore(miniMessage.deserialize(disableLoreText))
            .clearAllAttributes();
            
        ActiveItem disableItem = new ActiveItem(disableBuilder.build())
            .click(e -> {
                e.setCancelled(true);
                service.disablePlugin(targetPlugin, player);
                openPluginDetailsGUI(player, targetPlugin);
            });
            
        String backText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.back-text", "<blue>Back to Plugin List</blue>");
        
        ItemBuilder backBuilder = new ItemBuilder(Material.ARROW)
            .name(miniMessage.deserialize(backText))
            .clearAllAttributes();
            
        ActiveItem backItem = new ActiveItem(backBuilder.build())
            .click(e -> {
                e.setCancelled(true);
                openMainGUI(player);
            });
            
        HashMap<Character, ActiveItem> items = new HashMap<>();
        items.put('X', borderItem);
        items.put('I', infoItem);
        items.put('R', reloadItem);
        items.put('B', backItem);
        
        if (targetPlugin.isEnabled()) {
            items.put('D', disableItem);
            items.put('E', new ActiveItem(new ItemBuilder(Material.BARRIER).name(Component.empty()).build()).click(e -> e.setCancelled(true)));
        } else {
            items.put('E', enableItem);
            items.put('D', new ActiveItem(new ItemBuilder(Material.BARRIER).name(Component.empty()).build()).click(e -> e.setCancelled(true)));
        }
        
        gui.setUnActiveItems(items);
        gui.open(player);
    }
} 