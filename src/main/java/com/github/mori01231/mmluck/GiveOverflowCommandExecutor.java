package com.github.mori01231.mmluck;

import com.github.mori01231.mmluck.utils.Expr;
import io.lumine.xikage.mythicmobs.MythicMobs;
import net.azisaba.itemstash.ItemStash;
import net.azisaba.loreeditor.api.item.CraftItemStack;
import net.azisaba.rarity.api.Rarity;
import net.azisaba.rarity.api.RarityAPIProvider;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getPlayer;

public class GiveOverflowCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Usage /mlg プレイヤー名 アイテム名 確率 個数
        // 確率は少数で指定

        // declare those variables!
        int boostPercentage = Math.toIntExact(MMLuck.getInstance().boostHolder.refreshAndGetPercentage(false, true).join());
        float boostMulti = (boostPercentage + 100)/100.0f;

        String playerName;
        String mmItemName;
        try{
            playerName = args[0];
            mmItemName = args[1];
        }catch(Exception e){
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c正常な引数を渡してください。"));
            return true;
        }

        final double mmItemChance;
        int mmItemNumber;
        Player player = getPlayer(playerName);
        if (player == null) {
            sender.sendMessage("§c" + playerName + "は現在ログインしていません。");
            return true;
        }

        // Acquire the luck value of the player
        double luckNumber;
        try{
            luckNumber = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_LUCK)).getValue();
        }catch(Exception e){
            luckNumber = 0.0;
        }


        // Only for debug to determine if the correct luck value is read by plugin
        //sender.sendMessage(ChatColor.translateAlternateColorCodes('&', String.valueOf(luckNumber)));

        // Set mmItemNumber to the third argument if it is a positive integer
        try{
            mmItemNumber = Integer.parseInt(args[2]);
            if(mmItemNumber < 0){
                sendMessage(sender, player, "&c渡す確率は正の少数で指定してください。");
                return true;
            }
        }catch(Exception e){
            sendMessage(sender, player, "&c渡す確率は正の少数で指定してください。");
            return true;
        }

        // Set mmItemChance to the fourth argument if it is a positive double
        try{
            mmItemChance = Double.parseDouble(args[3]);
            if (mmItemChance < 0){
                sendMessage(sender, player, "&c渡す確率は正の少数で指定してください。");
                return true;
            }
        }catch(Exception e){
            sendMessage(sender, player, "&c渡す確率は正の少数で指定してください。");
            return true;
        }


        // Calculate the odds the player will be getting the item
        int giveMultiplier = (int) Math.round((100 + luckNumber) * boostMulti); // multiplier in 0-100%
        int giveOdds = (int)Math.round(giveMultiplier * mmItemChance * 100); // odds in 0-100% * 10

        sendMessage(sender, player, "&3アイテムドロップ確率 ： " + giveOdds / 100.0 + "%     ブースト倍率 : &f&l+" + boostMulti +"倍");
        //sender.sendMessage("アイテムが渡される確率（1を超えている場合は実際は1扱いされます）：" + String.valueOf(giveOdds/100.0));

        // Check silent mode
        boolean silent = MMLuck.getInstance().boostHolder.isSilentMode(player.getUniqueId());
        int amount = 0;
        while (giveOdds > 10000) {
            giveOdds -= 10000;
            amount += mmItemNumber;
        }

        // Generate random number
        Random rand = new Random();
        int rand_int1 = rand.nextInt(10000);

        // If the random number is lower than the chance of getting item, give item.
        if (rand_int1 < giveOdds) {
            amount += mmItemNumber;
        }
        if (amount > 0) {
            giveItems(player, mmItemName, amount, silent);
        }
        MMLuck.getInstance().getLogger().info("Player: " + playerName + ", Item: " + mmItemName + ", Chance: " +  giveOdds / 100.0 + "%, dropped amount: " + amount);
        return true;
    }

    static void giveItems(Player player, String mmItemId, int amount, boolean silent) {
        ItemStack stack = MythicMobs.inst().getItemManager().getItemStack(mmItemId);
        if (stack == null) {
            player.sendActionBar(ChatColor.RED + "アイテムが見つかりません:" + mmItemId);
            return;
        }
        // we need to do this because the item stack is bugged at the moment
        stack = CraftItemStack.STATIC.asCraftMirror(Objects.requireNonNull(CraftItemStack.STATIC.asNMSCopy(stack)));
        // rewrite lore if lore has script pattern
        if (stack.hasItemMeta()) {
            ItemMeta meta = Objects.requireNonNull(stack.getItemMeta());
            AtomicBoolean modified = new AtomicBoolean();
            if (meta.hasLore()) {
                meta.setLore(Objects.requireNonNull(meta.getLore()).stream().map(line -> {
                    String replaced = Expr.evalAndReplace(player, line, mmItemId + " (lore)");
                    if (replaced != null) {
                        modified.set(true);
                        return replaced;
                    }
                    return line;
                }).collect(Collectors.toList()));
            }
            if (meta.hasDisplayName()) {
                String originalName = meta.getDisplayName();
                String replaced = Expr.evalAndReplace(player, originalName, mmItemId + " (name)");
                if (replaced != null) {
                    modified.set(true);
                    meta.setDisplayName(replaced);
                }
            }
            if (modified.get()) {
                stack.setItemMeta(meta);
            }
        }
        String itemName;
        if (stack.hasItemMeta() && Objects.requireNonNull(stack.getItemMeta()).hasDisplayName()) {
            itemName = stack.getItemMeta().getDisplayName();
        } else {
            itemName = mmItemId;
        }
        stack.setAmount(amount);
        boolean doStash = MMLuck.getInstance().boostHolder.isAlwaysStash(player.getUniqueId());
        try {
            Rarity rarity = RarityAPIProvider.get().getRarityByItemStack(stack);
            if (rarity == null || (
                    rarity.getId().equals("rare") ||
                            rarity.getId().equals("epic") ||
                            rarity.getId().equals("legendary") ||
                            rarity.getId().equals("mythic") ||
                            rarity.getId().equals("special"))) {
                doStash = true;
            }
        } catch (Exception e) {
            doStash = true;
            MMLuck.getInstance().getLogger().warning("Failed to get rarity of " + mmItemId + ", assuming alwaysStash");
        }
        Collection<ItemStack> items =
                MMLuck.getInstance().boostHolder.isAlwaysStash(player.getUniqueId())
                        ? Collections.singleton(stack)
                        : player.getInventory().addItem(stack).values();
        int droppedAmount = items.stream().map(ItemStack::getAmount).reduce(Integer::sum).orElse(0);
        int actualAmount = amount;
        actualAmount -= droppedAmount;
        if (!silent) {
            TextComponent nameComponent = new TextComponent(itemName);
            nameComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(mmItemId)));
            TextComponent component;
            if (actualAmount > 0 || MMLuck.getInstance().boostHolder.isAlwaysStash(player.getUniqueId())) {
                component = new TextComponent("§6[MMLuck] §e");
                component.addExtra(nameComponent);
                if (MMLuck.getInstance().boostHolder.isAlwaysStash(player.getUniqueId())) {
                    component.addExtra("§r§7 (x" + amount + ")§aを獲得しました。");
                } else {
                    component.addExtra("§r§7 (x" + actualAmount + ")§aを獲得しました。");
                }
            } else {
                component = new TextComponent("§7[MMLuck] ");
                component.addExtra(nameComponent);
                component.addExtra("§r§8 (x" + actualAmount + ")§7を獲得しました。");
            }
            player.spigot().sendMessage(component);
        }
        boolean finalDoStash = doStash;
        Bukkit.getScheduler().runTaskAsynchronously(MMLuck.getInstance(), () -> {
            if (finalDoStash && !items.isEmpty() && items.stream().allMatch(item -> addToStashIfEnabled(player.getUniqueId(), item))) {
                if (!silent && !MMLuck.getInstance().boostHolder.isAlwaysStash(player.getUniqueId())) {
                    player.sendMessage("§e" + droppedAmount + "§c個のアイテム§7(" + itemName + "§r§7)§cがStashに入りました。");
                    player.sendMessage("§b/pickupstash§cで回収できます。");
                }
            }
        });
    }

    public static boolean addToStashIfEnabled(UUID uuid, ItemStack item) {
        try {
            Class.forName("net.azisaba.itemstash.ItemStash");
            ItemStash.getInstance().addItemToStash(uuid, item);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    // function to make sending messages a lot shorter
    public void sendMessage(CommandSender sender, Player player, String message){
        if(sender instanceof Player){
            player.sendActionBar('&',message);
        }

        if(!player.equals(sender)){
            player.sendActionBar('&',message);
        }
    }
}
