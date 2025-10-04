package de.steyon.devSystem.api.inv.controller;


import de.steyon.devSystem.api.inv.SteyOnInv;
import de.steyon.devSystem.api.inv.item.ActiveItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PageHandler {

    private final char attach;
    private final SteyOnInv SteyOnInv;
    private final int itemsPerPage;
    private final List<ActiveItem> activeItems;
    private int page;
    private int endIndex;
    private int startIndex;

    /**
     * @param SteyOnInv
     * @param itemsPerPage
     * @param activeItems
     * @param attach
     * @apiNote This constructor is used to set the start page of the inventory to 1
     */
    public PageHandler(SteyOnInv SteyOnInv, int itemsPerPage, List<ActiveItem> activeItems, char attach) {
        this.SteyOnInv = SteyOnInv;
        this.itemsPerPage = itemsPerPage;
        this.page = 1;
        this.startIndex = 0;
        this.activeItems = activeItems;
        this.attach = attach;
        this.endIndex = calculateEndIndex();
        inject();
    }

    /**
     * @param SteyOnInv
     * @param itemsPerPage
     * @param activeItems
     * @param attach
     * @param startPage
     * @apiNote This constructor is used to set the start page of the inventory
     */
    public PageHandler(SteyOnInv SteyOnInv, int itemsPerPage, List<ActiveItem> activeItems, char attach, int startPage) {
        this.SteyOnInv = SteyOnInv;
        this.itemsPerPage = itemsPerPage;
        this.page = Math.max(startPage, 1);
        if (startPage == 1) this.startIndex = 0;
        else this.startIndex = (startPage - 1) * itemsPerPage;
        this.activeItems = activeItems;
        this.endIndex = calculateEndIndex();
        this.attach = attach;
        inject();
    }

    private void inject() {
        List<ActiveItem> items = getItems();
        HashMap<Character, List<ActiveItem>> itemMap = new HashMap<>();
        itemMap.put(attach, items);
        SteyOnInv.setActiveItems(itemMap);
    }


    public List<ActiveItem> getItems() {
        return activeItems.subList(startIndex, endIndex);
    }

    public void nextPage() {
        if (page * itemsPerPage < activeItems.size()) {
            page++;
            startIndex = (page - 1) * itemsPerPage;

            updateIndexes();
        }
    }

    public void previousPage() {
        if (page > 1) {
            page--;
            startIndex = (page - 1) * itemsPerPage;

            updateIndexes();
        }
    }

    public boolean hasNextPage() {
        return page * itemsPerPage < activeItems.size();
    }

    public boolean hasPreviousPage() {
        return page > 1;
    }

    public int calculateEndIndex() {
        int endIndex = page * itemsPerPage;
        if (activeItems == null)
            return 0;
        if (activeItems.isEmpty())
            return 0;

        if (endIndex > activeItems.size()) {
            endIndex = activeItems.size();
        }

        return endIndex;
    }

    public void updateIndexes() {
        endIndex = calculateEndIndex();
        inject();
    }


}
