package com.github.mori01231.mmluck;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
public class GiveBoostItemCommandExecutor implements TabExecutor {
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /giveboostitem <player> <duration in minutes> <percentage> [amount] [player]");
            return true;
        }
        Player player = Bukkit.getPlayerExact(args[0]);
        if (player == null) {
            sender.sendMessage("Player not found");
            return true;
        }
        long durationMinutes = Long.parseLong(args[1]);
        long percentage = Long.parseLong(args[2]);
        int amount = 1;
        if (args.length >= 4) {
            amount = Integer.parseInt(args[3]);
        }
        String playerName = player.getName();
        UUID playerUUID = player.getUniqueId();
        if (args.length >= 5) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[4]);
            if (offlinePlayer.getUniqueId().version() == 4 && offlinePlayer.getName() != null) {
                playerName = offlinePlayer.getName();
                playerUUID = offlinePlayer.getUniqueId();
            }
        }
        double multi = 1 + (percentage / 100.0);
        ItemStack stack = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = Objects.requireNonNull(stack.getItemMeta());
        meta.setCustomModelData(5);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName("§3ドロップ率上昇スクロール §f" + durationMinutes + "分" + multi + "倍");
        meta.setLore(Arrays.asList(
                "§6 - 使い方 -",
                "§3空中で左クリックでドロップ率ブーストを発動",
                "§3効果時間： §f§l" + durationMinutes + "分",
                "§3ドロップ上昇率： §f§l" + multi + "倍 §7(+" + percentage + "%)",
                "§f",
                "§d" + playerName + " 専用",
                "§c§l【 取引禁止 】",
                "§f",
                "§7このアイテムは別で購入したブーストとスタックできません。",
                "§8[Soulbound: " + playerUUID + "]"
        ));
        meta.getPersistentDataContainer().set(new NamespacedKey(MMLuck.getInstance(), "duration"), PersistentDataType.LONG, durationMinutes);
        meta.getPersistentDataContainer().set(new NamespacedKey(MMLuck.getInstance(), "percentage"), PersistentDataType.LONG, percentage);
        meta.getPersistentDataContainer().set(new NamespacedKey(MMLuck.getInstance(), "player_name"), PersistentDataType.STRING, playerName);
        meta.getPersistentDataContainer().set(new NamespacedKey(MMLuck.getInstance(), "player_uuid"), PersistentDataType.STRING, playerUUID.toString());
        meta.getPersistentDataContainer().set(new NamespacedKey(MMLuck.getInstance(), "original_amount"), PersistentDataType.INTEGER, amount);
        meta.getPersistentDataContainer().set(new NamespacedKey(MMLuck.getInstance(), "item_id"), PersistentDataType.STRING, UUID.randomUUID().toString());
        stack.setItemMeta(meta);
        player.getInventory().addItem(stack);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
