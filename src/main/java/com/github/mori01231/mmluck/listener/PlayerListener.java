package com.github.mori01231.mmluck.listener;

import com.github.mori01231.lifecore.util.ItemUtil;
import com.github.mori01231.mmluck.MMLuck;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // remove cached value
        MMLuck.getInstance().boostHolder.silentMode.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // cache value
        new Thread(() -> MMLuck.getInstance().boostHolder.isSilentMode(e.getPlayer().getUniqueId())).start();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (e.getPlayer().getGameMode() != GameMode.ADVENTURE && e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        ItemStack stack = e.getItem();
        if (stack == null || !stack.hasItemMeta()) {
            return;
        }
        String mythicType = ItemUtil.getMythicType(stack);
        long durationMinutes = 0;
        long percentage = 0;
        if (mythicType != null && mythicType.startsWith("Donation_LuckScroll_")) {
            durationMinutes = Long.parseLong(mythicType.split("_")[2].replace("m", ""));
            percentage = Long.parseLong(mythicType.split("_")[3].replace("p", ""));
        }
        ItemMeta meta = Objects.requireNonNull(stack.getItemMeta());
        Long tempDuration = meta.getPersistentDataContainer().get(new NamespacedKey(MMLuck.getInstance(), "duration"), PersistentDataType.LONG);
        if (tempDuration != null) {
            durationMinutes = tempDuration;
        }
        Long tempPercentage = meta.getPersistentDataContainer().get(new NamespacedKey(MMLuck.getInstance(), "percentage"), PersistentDataType.LONG);
        if (tempPercentage != null) {
            percentage = tempPercentage;
        }
        Integer originalAmount = meta.getPersistentDataContainer().get(new NamespacedKey(MMLuck.getInstance(), "original_amount"), PersistentDataType.INTEGER);
        if (originalAmount != null && originalAmount < stack.getAmount()) {
            String itemId = meta.getPersistentDataContainer().get(new NamespacedKey(MMLuck.getInstance(), "item_id"), PersistentDataType.STRING);
            MMLuck.getInstance().getLogger().warning("Player " + e.getPlayer().getName() + " tried to use a duplicated item with id " + itemId);
            MMLuck.getInstance().getLogger().warning("original amount: " + originalAmount + ", current amount: " + stack.getAmount());
        }
        if (durationMinutes == 0 || percentage == 0) {
            return;
        }
        long boostPercentage = MMLuck.getInstance().boostHolder.refreshAndGetPercentage();
        if ((boostPercentage + percentage) > 200) { // cap at +200%
            e.getPlayer().sendMessage(ChatColor.RED + "使用するとブースト倍率が3倍を超えるため、このブーストは使用できません。");
            return;
        }

        // add boost to boostHolder
        MMLuck.getInstance().boostHolder.addBoost(System.currentTimeMillis(), durationMinutes * 60, percentage);
        e.getItem().subtract(1);

        String sp = Long.toString(percentage);
        if (percentage >= 0) {
            sp = "+" + sp;
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "[ブースト] " + ChatColor.WHITE + ChatColor.BOLD + e.getPlayer().getName() + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "が" +
                ChatColor.WHITE + ChatColor.BOLD + sp + "%" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "ブーストを使用しました！" + ChatColor.GRAY + "(" + durationMinutes + "分間有効)");

        float boostMulti = (percentage + boostPercentage + 100)/100.0f;

        Bukkit.broadcastMessage(ChatColor.GOLD + "[ブースト] " + ChatColor.LIGHT_PURPLE + "現在のブースト倍率は" + ChatColor.WHITE + ChatColor.BOLD + boostMulti + ChatColor.LIGHT_PURPLE + "倍です！");
    }
}
