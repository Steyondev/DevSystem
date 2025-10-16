package de.steyon.devSystem.api.inv;

import de.steyon.devSystem.api.inv.controller.InventoryEventListener;
import de.steyon.devSystem.api.inv.item.ActiveItem;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class SteyOnInv {

    @Getter
    private final Inventory inventory;
    @Getter
    private final ArrayList<ActiveItem> activeItems;
    private final Plugin mainClass;
    private final Player player;
    private final int size;
    @Getter
    private final Component title;
    public Consumer<InventoryOpenEvent> openAction;
    private static InventoryEventListener listener;
    @Getter
    public Consumer<InventoryCloseEvent> closeAction;
    @Getter
    private String[] pattern;

    public SteyOnInv(Plugin mainClass, int size, Component title, Player player, String[] pattern) {
        this.mainClass = mainClass;
        this.size = size;
        this.title = title;
        this.player = player;
        this.pattern = pattern;
        this.activeItems = new ArrayList<>();
        this.inventory = this.mainClass.getServer().createInventory(player, size, title);
        if (listener == null) {
            listener = new InventoryEventListener();
            this.mainClass.getServer().getPluginManager().registerEvents(listener, mainClass);
        }
        listener.registerInventory(this);
    }

    public SteyOnInv addItem(ActiveItem activeItem) {
        inventory.addItem(activeItem.getItemStack());
        activeItems.add(activeItem);
        return this;
    }

    public SteyOnInv onOpen(Consumer<InventoryOpenEvent> action) {
        this.openAction = action;
        return this;
    }

    public SteyOnInv onClose(Consumer<InventoryCloseEvent> action) {
        this.closeAction = action;
        return this;
    }

    public void removeItem(int slot) {
        activeItems.removeIf(item -> item.getItemStack().equals(inventory.getItem(slot)));
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void setItem(int slot, ActiveItem activeItem) {
        inventory.setItem(slot, activeItem.getItemStack());
        activeItems.add(activeItem);
    }


    public void handleClick(InventoryClickEvent event) {
        for (ActiveItem activeItem : activeItems) {
            if (event.getCurrentItem() != null && event.getCurrentItem().isSimilar(activeItem.getItemStack()) && activeItem.getAction() != null) {
                activeItem.getAction().accept(event);
                break;
            }
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        if (closeAction != null) {
            closeAction.accept(event);
        }
    }

    public void setUnActiveItems(HashMap<Character, ActiveItem> activeItems) {

        int startIndex = 0;

        for (String row : pattern) {

            if (startIndex >= size) {
                break;
            }

            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (activeItems.containsKey(c)) {
                    setItem(startIndex + i, activeItems.get(c));
                }
            }

            startIndex += 9;
        }

    }

    /**
     * @param activeItems
     * @apiNote This method is used to set the active items in the inventory
     */
    public void setActiveItems(HashMap<Character, List<ActiveItem>> activeItems) {
        int startIndex = 0;

        for (String row : pattern) {

            if (startIndex >= size) {
                break;
            }

            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (activeItems.containsKey(c)) {
                    List<ActiveItem> items = activeItems.get(c);
                    for (ActiveItem item : items) {
                        if (inventory.contains(item.getItemStack())) continue;
                        for (ItemStack itemStack : inventory.getContents()){
                            if (itemStack != null) continue;
                            setItem(startIndex + i, item);
                        }
                    }
                }
            }

            startIndex += 9;
        }
    }
}