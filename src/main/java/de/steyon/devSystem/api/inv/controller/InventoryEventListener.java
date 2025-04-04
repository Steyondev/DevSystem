package de.steyon.devSystem.api.inv.controller;

import de.steyon.devSystem.api.inv.SteyOnInv;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InventoryEventListener implements Listener {

    private final List<SteyOnInv> registeredInventories = new ArrayList<>();

    public void registerInventory(SteyOnInv inventory) {
        registeredInventories.add(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        for (SteyOnInv inventory : registeredInventories) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(inventory.getInventory())) {
                inventory.handleClick(event);
                break;
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {

        for (SteyOnInv inventory : registeredInventories) {
            if (event.getInventory().equals(inventory.getInventory())) {
                if (inventory.openAction != null) {
                    inventory.openAction.accept(event);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        for (SteyOnInv inventory : registeredInventories) {
            if (event.getInventory().equals(inventory.getInventory())) {
                if (inventory.closeAction != null) {
                    inventory.closeAction.accept(event);
                    break;
                }
            }
        }
    }
}
