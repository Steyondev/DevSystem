package de.steyon.devSystem.api.inv.item;

import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@Getter
public class ActiveItem {

    private final ItemStack itemStack;
    private Consumer<InventoryClickEvent> action;

    public ActiveItem(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ActiveItem click(Consumer<InventoryClickEvent> action) {
        this.action = action;
        return this;
    }

    @EventHandler
    public void handleClick(InventoryClickEvent event) {
        if (action != null) {
            action.accept(event);
        }
    }

}
