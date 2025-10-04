package de.steyon.devSystem.api.inv.controller;

import de.steyon.devSystem.api.inv.SteyOnInv;
import de.steyon.devSystem.api.inv.SteyOnInv;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.UUID;

public class InventoryManager {

    private final Plugin mainClass;
    private final HashMap<UUID, SteyOnInv> cache;

    public InventoryManager(Plugin mainClass, HashMap<UUID, SteyOnInv> cache) {
        this.mainClass = mainClass;
        this.cache = cache;
    }

    public void open(UUID uuid, SteyOnInv SteyOnInv) {
        this.cache.put(uuid, SteyOnInv);
    }

    public void add(UUID uuid, SteyOnInv SteyOnInv) {
        cache.put(uuid, SteyOnInv);
    }

    public void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public void clear() {
        cache.clear();
    }

}