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
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;

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
        
        boolean showLoadButton = plugin.getConfigManager().getValue("config.yml", "plugin-manager.settings.load-gui-button-enabled", true);
        if (totalPages > 1) {
            pattern[5] = showLoadButton ? "XXX<X>XXL" : "XXX<X>XXX";
        } else {
            if (showLoadButton) pattern[5] = "XXXXXXXXL";
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

        if (showLoadButton) {
            String loadText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.load-button-text", "<yellow>Load Plugin</yellow>");
            String loadLore = plugin.getConfigManager().getValue("config.yml", "plugin-manager.load-button-lore", "<dark_gray>» <gray>Click to see command usage");

            ItemBuilder loadBuilder = new ItemBuilder(Material.CHEST)
                .name(miniMessage.deserialize(loadText))
                .lore(miniMessage.deserialize(loadLore))
                .clearAllAttributes();

            ActiveItem loadItem = new ActiveItem(loadBuilder.build())
                .click(e -> {
                    e.setCancelled(true);
                    if (!((Player) e.getWhoClicked()).hasPermission("devsystem.pluginmanager.load")) {
                        ((Player) e.getWhoClicked()).sendMessage(
                            plugin.getConfigManager().getMessage(
                                plugin.getConfigManager().getValue("config.yml", "messages.no-permission", "<prefix><red>You don't have permission to use this command!</red>")
                            )
                        );
                        return;
                    }
                    String loadTitle = plugin.getConfigManager().getValue("config.yml", "plugin-manager.load-help-title", "<aqua><bold>Load Plugin</bold>");
                    String usage = plugin.getConfigManager().getValue("config.yml", "plugin-manager.load-help-usage", "<dark_gray>» <gray>/plugmanager load <blue><path-or-url>");
                    ((Player) e.getWhoClicked()).sendMessage(miniMessage.deserialize(loadTitle));
                    ((Player) e.getWhoClicked()).sendMessage(miniMessage.deserialize(usage));
                });

            staticItems.put('L', loadItem);
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
                    Component.empty(),
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
            "XXXXIXXXX",
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
        
        String statusText = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-status", "<gray>Status: {status}")
            .replace("{status}", targetPlugin.isEnabled() ? "<green>Enabled</green>" : "<red>Disabled</red>");
        
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
        
        String eventsFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-events", 
            "<gray>Events Listener: <green>{events}</green>");
        
        StringBuilder eventsBuilder = new StringBuilder();
        
        try {
            List<String> listenerClasses = new ArrayList<>();
            
            for (RegisteredListener listener : HandlerList.getRegisteredListeners(targetPlugin)) {
                String listenerClass = listener.getListener().getClass().getSimpleName();
                if (!listenerClass.isEmpty() && !listenerClasses.contains(listenerClass)) {
                    listenerClasses.add(listenerClass);
                }
            }
            
            if (!listenerClasses.isEmpty()) {
                eventsBuilder.append(String.join(", ", listenerClasses));
            }
        } catch (Exception e) {
        }
        
        String eventsText = eventsFormat.replace("{events}", 
            eventsBuilder.length() > 0 ? eventsBuilder.toString() : "None");
        
        String apiFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-api", 
            "<gray>API: <green>{api}</green>");
        String apiText = apiFormat.replace("{api}", service.getApiVersion(targetPlugin));

        String loadFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-load", 
            "<gray>Load: <green>{load}</green>");
        String loadText = loadFormat.replace("{load}", service.getLoadPhase(targetPlugin));

        String providesFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-provides", 
            "<gray>Provides: <green>{provides}</green>");
        String providesJoined = String.join(", ", service.getProvides(targetPlugin));
        String providesText = providesFormat.replace("{provides}", providesJoined.isEmpty() ? "None" : providesJoined);

        String jarFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-jar", 
            "<gray>JAR: <green>{path}</green> <gray>({size} bytes)</gray>");
        String jarText = jarFormat.replace("{path}", service.getJarPath(targetPlugin))
            .replace("{size}", String.valueOf(Math.max(service.getJarSizeBytes(targetPlugin), 0)));

        String dataFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-datafolder", 
            "<gray>DataFolder: <green>{path}</green> <gray>({files} files, {size} bytes)</gray>");
        String dataText = dataFormat
            .replace("{path}", targetPlugin.getDataFolder() != null ? targetPlugin.getDataFolder().getAbsolutePath() : "-")
            .replace("{files}", String.valueOf(service.getDataFolderFileCount(targetPlugin)))
            .replace("{size}", String.valueOf(Math.max(service.getDataFolderSizeBytes(targetPlugin), 0)));

        String listenersCountFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-listeners-count", 
            "<gray>Listeners: <green>{count}</green>");
        String listenersCountText = listenersCountFormat.replace("{count}", String.valueOf(service.getListenerCount(targetPlugin)));

        String tasksCountFormat = plugin.getConfigManager().getValue("config.yml", "plugin-manager.plugin-tasks-count", 
            "<gray>Tasks: <green>{count}</green>");
        String tasksCountText = tasksCountFormat.replace("{count}", String.valueOf(service.getTaskCount(targetPlugin)));
        
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
                miniMessage.deserialize(mainClassText),
                miniMessage.deserialize(eventsText),
                miniMessage.deserialize(apiText),
                miniMessage.deserialize(loadText),
                miniMessage.deserialize(providesText),
                miniMessage.deserialize(jarText),
                miniMessage.deserialize(dataText),
                miniMessage.deserialize(listenersCountText),
                miniMessage.deserialize(tasksCountText)
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
                if (player.hasPermission("devsystem.pluginmanager.reload")) {
                    // Prevent reloading the core DevSystem plugin to avoid breaking the system
                    if (targetPlugin.getName().equalsIgnoreCase(plugin.getName())) {
                        player.sendMessage(miniMessage.deserialize(
                            plugin.getConfigManager().getValue("config.yml", "plugin-manager.cannot-reload-core",
                            "<prefix><red>Cannot reload core system plugin</red><dark_gray>: </dark_gray><aqua>{plugin}</aqua>")
                                .replace("{plugin}", targetPlugin.getName())
                        ));
                        return;
                    }
                    openConfirmGUI(player,
                        plugin.getConfigManager().getValue("config.yml", "plugin-manager.confirm-reload", "<yellow>Reload {plugin}?</yellow>").replace("{plugin}", targetPlugin.getName()),
                        () -> {
                            service.reloadPlugin(targetPlugin, player);
                            openPluginDetailsGUI(player, targetPlugin);
                        },
                        () -> openPluginDetailsGUI(player, targetPlugin)
                    );
                }
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
                if (player.hasPermission("devsystem.pluginmanager.enable")) {
                    openConfirmGUI(player,
                        plugin.getConfigManager().getValue("config.yml", "plugin-manager.confirm-enable", "<green>Enable {plugin}?</green>").replace("{plugin}", targetPlugin.getName()),
                        () -> {
                            service.enablePlugin(targetPlugin, player);
                            openPluginDetailsGUI(player, targetPlugin);
                        },
                        () -> openPluginDetailsGUI(player, targetPlugin)
                    );
                }
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
                if (player.hasPermission("devsystem.pluginmanager.disable")) {
                    openConfirmGUI(player,
                        plugin.getConfigManager().getValue("config.yml", "plugin-manager.confirm-disable", "<red>Disable {plugin}?</red>").replace("{plugin}", targetPlugin.getName()),
                        () -> {
                            service.disablePlugin(targetPlugin, player);
                            openPluginDetailsGUI(player, targetPlugin);
                        },
                        () -> openPluginDetailsGUI(player, targetPlugin)
                    );
                }
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
        boolean canReload = player.hasPermission("devsystem.pluginmanager.reload");
        boolean canEnable = player.hasPermission("devsystem.pluginmanager.enable");
        boolean canDisable = player.hasPermission("devsystem.pluginmanager.disable");

        items.put('R', canReload ? reloadItem : borderItem);
        items.put('B', backItem);

        if (targetPlugin.isEnabled()) {
            items.put('D', canDisable ? disableItem : borderItem);
            items.put('E', borderItem);
        } else {
            items.put('E', canEnable ? enableItem : borderItem);
            items.put('D', borderItem);
        }

        gui.setUnActiveItems(items);
        gui.open(player);
    }

    private void openConfirmGUI(Player player, String titleMiniMsg, Runnable onConfirm, Runnable onCancel) {
        Component title = miniMessage.deserialize(titleMiniMsg);
        String[] pattern = {
            "XXXXXXXXX",
            "XXYXXXNXX",
            "XXXXXXXXX",
        };

        SteyOnInv gui = new SteyOnInv(plugin, 9 * 3, title, player, pattern);

        ItemBuilder borderBuilder = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(Component.empty())
            .clearAllAttributes();
        ActiveItem border = new ActiveItem(borderBuilder.build()).click(e -> e.setCancelled(true));

        ItemBuilder yesBuilder = new ItemBuilder(Material.LIME_WOOL)
            .name(miniMessage.deserialize(plugin.getConfigManager().getValue("config.yml", "plugin-manager.confirm-yes", "<green>Yes</green>")))
            .clearAllAttributes();
        ActiveItem yes = new ActiveItem(yesBuilder.build()).click(e -> {
            e.setCancelled(true);
            if (onConfirm != null) onConfirm.run();
        });

        ItemBuilder noBuilder = new ItemBuilder(Material.RED_WOOL)
            .name(miniMessage.deserialize(plugin.getConfigManager().getValue("config.yml", "plugin-manager.confirm-no", "<red>No</red>")))
            .clearAllAttributes();
        ActiveItem no = new ActiveItem(noBuilder.build()).click(e -> {
            e.setCancelled(true);
            if (onCancel != null) onCancel.run();
        });

        HashMap<Character, ActiveItem> items = new HashMap<>();
        items.put('X', border);
        items.put('Y', yes);
        items.put('N', no);

        gui.setUnActiveItems(items);
        gui.open(player);
    }
}