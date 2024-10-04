package com.github.mori01231.mmluck.gui;

import com.github.mori01231.lifecore.util.ItemUtil;
import com.github.mori01231.mmluck.MMLuck;
import net.azisaba.rarity.api.Rarity;
import net.azisaba.rarity.api.RarityAPIProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MinimumStashRarityGui implements InventoryHolder {
    private final Inventory inventory = Bukkit.createInventory(this, 9, "Minimum stash rarity gui");
    private final Map<Integer, Integer> slotToRarityWeight = new HashMap<>();
    private final Player player;

    public MinimumStashRarityGui(Player player) {
        this.player = player;
        reset();
    }

    public void reset() {
        int minimumStashRarityWeight = MMLuck.getInstance().boostHolder.getMinimumStashRarity(player.getUniqueId());
        Rarity commonRarity = RarityAPIProvider.get().getRarityById("common");
        String commonName = commonRarity.getDisplayName(player);
        String commonLore = commonRarity.getWeight() >= minimumStashRarityWeight ? ChatColor.GREEN + "有効" : ChatColor.RED + "無効";
        Rarity uncommonRarity = RarityAPIProvider.get().getRarityById("uncommon");
        String uncommonName = uncommonRarity.getDisplayName(player);
        String uncommonLore = uncommonRarity.getWeight() >= minimumStashRarityWeight ? ChatColor.GREEN + "有効" : ChatColor.RED + "無効";
        Rarity rareRarity = RarityAPIProvider.get().getRarityById("rare");
        String rareName = rareRarity.getDisplayName(player);
        String rareLore = rareRarity.getWeight() >= minimumStashRarityWeight ? ChatColor.GREEN + "有効" : ChatColor.RED + "無効";
        Rarity epicRarity = RarityAPIProvider.get().getRarityById("epic");
        String epicName = epicRarity.getDisplayName(player);
        String epicLore = epicRarity.getWeight() >= minimumStashRarityWeight ? ChatColor.GREEN + "有効" : ChatColor.RED + "無効";
        Rarity legendaryRarity = RarityAPIProvider.get().getRarityById("legendary");
        String legendaryName = legendaryRarity.getDisplayName(player);
        String legendaryLore = legendaryRarity.getWeight() >= minimumStashRarityWeight ? ChatColor.GREEN + "有効" : ChatColor.RED + "無効";
        Rarity mythicRarity = RarityAPIProvider.get().getRarityById("mythic");
        String mythicName = mythicRarity.getDisplayName(player);
        String mythicLore = mythicRarity.getWeight() >= minimumStashRarityWeight ? ChatColor.GREEN + "有効" : ChatColor.RED + "無効";
        Rarity specialRarity = RarityAPIProvider.get().getRarityById("special");
        String specialName = specialRarity.getDisplayName(player);
        String specialLore = specialRarity.getWeight() >= minimumStashRarityWeight ? ChatColor.GREEN + "有効" : ChatColor.RED + "無効";
        inventory.setItem(0, ItemUtil.createItemStack(Material.BARRIER, 1, item -> {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "閉じる");
                item.setItemMeta(meta);
            }
        }));

        slotToRarityWeight.put(1, commonRarity.getWeight());
        inventory.setItem(1, ItemUtil.createItemStack(Material.COBBLESTONE, 1, item -> {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', commonName));
                meta.setLore(Collections.singletonList(commonLore));
                item.setItemMeta(meta);
            }
        }));
        slotToRarityWeight.put(2, uncommonRarity.getWeight());
        inventory.setItem(2, ItemUtil.createItemStack(Material.IRON_BLOCK, 1, item -> {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', uncommonName));
                meta.setLore(Collections.singletonList(uncommonLore));
                item.setItemMeta(meta);
            }
        }));
        slotToRarityWeight.put(3, rareRarity.getWeight());
        inventory.setItem(3, ItemUtil.createItemStack(Material.GOLD_BLOCK, 1, item -> {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', rareName));
                meta.setLore(Collections.singletonList(rareLore));
                item.setItemMeta(meta);
            }
        }));
        slotToRarityWeight.put(4, epicRarity.getWeight());
        inventory.setItem(4, ItemUtil.createItemStack(Material.EMERALD_BLOCK, 1, item -> {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', epicName));
                meta.setLore(Collections.singletonList(epicLore));
                item.setItemMeta(meta);
            }
        }));
        slotToRarityWeight.put(5, legendaryRarity.getWeight());
        inventory.setItem(5, ItemUtil.createItemStack(Material.DIAMOND_BLOCK, 1, item -> {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', legendaryName));
                meta.setLore(Collections.singletonList(legendaryLore));
                item.setItemMeta(meta);
            }
        }));
        slotToRarityWeight.put(6, mythicRarity.getWeight());
        inventory.setItem(6, ItemUtil.createItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1, item -> {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', mythicName));
                meta.setLore(Collections.singletonList(mythicLore));
                item.setItemMeta(meta);
            }
        }));
        slotToRarityWeight.put(7, specialRarity.getWeight());
        inventory.setItem(7, ItemUtil.createItemStack(Material.NETHER_STAR, 1, item -> {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', specialName));
                meta.setLore(Collections.singletonList(specialLore));
                item.setItemMeta(meta);
            }
        }));
        inventory.setItem(8, ItemUtil.createItemStack(Material.BARRIER, 1, item -> {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "閉じる");
                item.setItemMeta(meta);
            }
        }));
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static class EventListener implements Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            if (!(e.getInventory().getHolder() instanceof MinimumStashRarityGui)) return;
            e.setCancelled(true);
            if (e.getClickedInventory() == null) return;
            if (!(e.getClickedInventory().getHolder() instanceof MinimumStashRarityGui)) return;
            MinimumStashRarityGui gui = (MinimumStashRarityGui) e.getClickedInventory().getHolder();
            if (e.getSlot() == 0 || e.getSlot() == 8) {
                p.closeInventory();
                return;
            }
            if (gui.slotToRarityWeight.containsKey(e.getSlot())) {
                int weight = gui.slotToRarityWeight.get(e.getSlot());
                MMLuck.getInstance().boostHolder.setMinimumStashRarity(p.getUniqueId(), weight);
                gui.reset();
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 10f, 1f);
                p.updateInventory();
            }
        }

        @EventHandler
        public void onInventoryDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder() instanceof MinimumStashRarityGui) {
                e.setCancelled(true);
            }
        }
    }
}
