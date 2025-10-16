package de.steyon.devSystem.api.item;

import de.steyon.devSystem.DevSystem;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder name(String name) {
        name(LegacyComponentSerializer.legacySection().deserialize(name));
        return this;
    }

    public ItemBuilder name(Component name) {
        Component nonItalic = name.decoration(TextDecoration.ITALIC, false);
        this.itemStack.editMeta(itemMeta -> itemMeta.displayName(nonItalic));
        return this;
    }

    public ItemBuilder setCustomModelData(int customModelData) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        itemMeta.setCustomModelData(customModelData);
        this.itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemStack build() {
        return this.itemStack;
    }

    public ItemBuilder lore(String... lore) {
        List<Component> componentLore = new ArrayList<>();
        for (String s : lore) {
            Component c = LegacyComponentSerializer.legacySection().deserialize(s).decoration(TextDecoration.ITALIC, false);
            componentLore.add(c);
        }
        lore(componentLore.toArray(Component[]::new));
        return this;
    }

    public ItemBuilder lore(Component... lore) {
        List<Component> nonItalicLore = new ArrayList<>();
        for (Component c : lore) {
            nonItalicLore.add(c.decoration(TextDecoration.ITALIC, false));
        }
        this.itemStack.editMeta(itemMeta -> itemMeta.lore(nonItalicLore));
        return this;
    }

    public ItemBuilder lore(List<Component> components){
        List<Component> nonItalicLore = new ArrayList<>();
        for (Component c : components) {
            nonItalicLore.add(c.decoration(TextDecoration.ITALIC, false));
        }
        this.itemStack.editMeta(itemMeta -> itemMeta.lore(nonItalicLore));
        return this;
    }


    public ItemBuilder setSkullTexture(String texture, Plugin plugin) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        SkullMeta skullMeta = (SkullMeta) itemMeta;
        PlayerProfile profile = plugin.getServer().createProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();
        try {
            textures.setSkin(new URI(texture).toURL());
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        profile.setTextures(textures);
        skullMeta.setPlayerProfile(profile);
        this.itemStack.setItemMeta(skullMeta);
        return this;
    }

    public List<ItemBuilder> setSkullTexture(String texture, DevSystem devSystem, int amount) {
        List<ItemBuilder> itemBuilders = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            ItemStack clone = this.itemStack.clone();
            ItemMeta itemMeta = clone.getItemMeta();
            SkullMeta skullMeta = (SkullMeta) itemMeta;
            com.destroystokyo.paper.profile.PlayerProfile profile = devSystem.getServer().createProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            try {
                textures.setSkin(new URL(texture));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            profile.setTextures(textures);
            skullMeta.setPlayerProfile(profile);
            clone.setItemMeta(skullMeta);
            itemBuilders.add(new ItemBuilder(clone));
        }
        return itemBuilders;
    }

    public ItemBuilder setCustomSkullWithValue(String data) throws IllegalArgumentException {
        if (!this.itemStack.getType().equals(Material.PLAYER_HEAD) || !(this.itemStack.getItemMeta() instanceof SkullMeta headMeta)) {
            throw new IllegalArgumentException("Head needs PlayerHead as Material");
        }


        com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.getServer().createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", data));

        headMeta.setPlayerProfile(profile);

        this.itemStack.setItemMeta(headMeta);
        return this;
    }

    public ItemBuilder setFireWorkColor(Color color) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        itemMeta.setCustomModelData(100);
        itemStack.setItemMeta(itemMeta);
        itemMeta = itemStack.getItemMeta();
        FireworkEffectMeta fireworkEffectMeta = (FireworkEffectMeta) itemMeta;
        fireworkEffectMeta.setEffect(FireworkEffect.builder().withColor(color).build());
        this.itemStack.setItemMeta(fireworkEffectMeta);
        return this;
    }

    public ItemBuilder setSkullOwner(String name, DevSystem devSystem) {
        OfflinePlayer offlinePlayer = devSystem.getServer().getOfflinePlayer(name);
        if (offlinePlayer.hasPlayedBefore()) {
            this.itemStack.editMeta(itemMeta -> {
                SkullMeta skullMeta = (SkullMeta) itemMeta;
                skullMeta.setOwningPlayer(offlinePlayer);
            });
        }
        return this;
    }


    public ItemBuilder clearAllAttributes() {
        this.itemStack.editMeta(itemMeta -> {
            itemMeta.addItemFlags(ItemFlag.values());
        });
        return this;
    }

    public ItemBuilder amount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder loreWrap(@NotNull Component deserialize) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<Component> lore = itemMeta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.add(deserialize.decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(lore);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

   // public ItemBuilder glow(){
   //     itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
   //     ItemMeta itemMeta = itemStack.getItemMeta();
   //     itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
   //     itemStack.setItemMeta(itemMeta);
   //
   //     return this;
   // }

   // public ItemBuilder potion(PotionType potionType){
   //     ItemMeta itemMeta = itemStack.getItemMeta();
   //     PotionMeta potionMeta = (PotionMeta) itemMeta;
   //     potionMeta.setBasePotionType(potionType);
   //     itemMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
   //     itemStack.setItemMeta(potionMeta);
   //     return this;
   // }

}
