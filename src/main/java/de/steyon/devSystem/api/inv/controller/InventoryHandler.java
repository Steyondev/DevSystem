package de.steyon.devSystem.api.inv.controller;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

public abstract class InventoryHandler implements Listener {

    @EventHandler
    public abstract void onInventoryClick(InventoryClickEvent event);

    @EventHandler
    public abstract void onInventoryClose(InventoryCloseEvent event);

    @EventHandler
    public abstract void onInventoryOpen(InventoryOpenEvent event);

    @EventHandler
    public abstract void onInventoryDrag(InventoryDragEvent event);

    @EventHandler
    public abstract void onInventoryMove(InventoryMoveItemEvent event);

    @EventHandler
    public abstract void onInventoryPickup(InventoryPickupItemEvent event);

    @EventHandler
    public abstract void onInventoryInteract();

    @EventHandler
    public abstract void onInventoryScroll();

    @EventHandler
    public abstract void onInventoryUpdate();

    @EventHandler
    public abstract void onInventorySwap();

    @EventHandler
    public abstract void onInventoryInsert();

    @EventHandler
    public abstract void onInventoryRemove();

    @EventHandler
    public abstract void onInventoryAdd();

    @EventHandler
    public abstract void onInventoryRemoveAll();

    @EventHandler
    public abstract void onInventoryClear();

    @EventHandler
    public abstract void onInventoryUpdateAll();

    @EventHandler
    public abstract void onInventoryCloseAll();

    @EventHandler
    public abstract void onInventoryOpenAll();

    @EventHandler
    public abstract void onInventoryClickAll();

    @EventHandler
    public abstract void onInventoryDragAll();

    @EventHandler
    public abstract void onInventoryMoveAll();

    @EventHandler
    public abstract void onInventoryPickupAll();

    @EventHandler
    public abstract void onInventoryDropAll();

    @EventHandler
    public abstract void onInventoryInteractAll();

    @EventHandler
    public abstract void onInventoryScrollAll();

    @EventHandler
    public abstract void onInventorySwapAll();

    @EventHandler
    public abstract void onInventoryInsertAll();

    @EventHandler
    public abstract void onInventoryRemoveAllAll();

    @EventHandler
    public abstract void onInventoryAddAll();

    @EventHandler
    public abstract void onInventoryClearAll();

}
