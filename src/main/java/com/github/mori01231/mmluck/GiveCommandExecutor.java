package com.github.mori01231.mmluck;

import net.azisaba.rarity.api.Rarity;
import net.azisaba.rarity.api.RarityAPIProvider;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.bukkit.Bukkit.getPlayer;

public class GiveCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Usage /mlg プレイヤー名 アイテム名 個数 確率
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

        double mmItemChance;
        int mmItemNumber;
        Player player;
        try{
            player = getPlayer(playerName);
            if(player == null){
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&cこの名前のプレイヤーは現在ログインしていません。"));
                return true;
            }
        }catch(Exception e){
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&cこの名前のプレイヤーは現在ログインしていません。"));
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

        Rarity minimumRareMessageRarity = RarityAPIProvider.get().getRarityById(args.length == 4 ? "rare" : args[4]);

        // Calculate the odds the player will be getting the item
        BigDecimal luckNumberD = new BigDecimal(luckNumber);
        BigDecimal boostMultiD = new BigDecimal(boostMulti);
        double giveMultiplier = (100 + luckNumber) * boostMulti / 100; // multiplier in 0-100%
        // If the random number is lower than the chance of getting item, give item.
        boolean doDrop;
        try {
            BigDecimal giveMultiplierD = luckNumberD.add(new BigDecimal(100)).multiply(boostMultiD).divide(new BigDecimal(100), RoundingMode.HALF_EVEN);
            BigDecimal giveOdds = giveMultiplierD.multiply(BigDecimal.valueOf(mmItemChance)); // odds in 0-100%
            String formattedValue = giveOdds.multiply(new BigDecimal(100)).stripTrailingZeros().toPlainString();
            String japanese = toFriendlyString(new BigDecimal(1).divide(giveOdds, RoundingMode.HALF_EVEN).toBigInteger()) + "分の1";
            sendMessage(sender, player, "&3アイテムドロップ確率 ： " + formattedValue + "% (" + giveOdds.multiply(new BigDecimal(100)).doubleValue() + "% " + japanese + ")     ブースト倍率 : &f&l+" + boostMulti + "倍");
            doDrop = giveOdds.compareTo(BigDecimal.valueOf(Math.random())) >= 0;
            MMLuck.getInstance().getLogger().info("Player: " + playerName + ", Item: " + mmItemName + ", Chance: " +  giveOdds.multiply(new BigDecimal(100)) + "%, amount: " + mmItemNumber + ", doDrop: " + doDrop);
        } catch (ArithmeticException e) {
            double giveOdds = giveMultiplier * mmItemChance; // odds in 0-100%
            sendMessage(sender, player, "&3アイテムドロップ確率 ： " + (giveOdds * 100) + "%     ブースト倍率 : &f&l+" + boostMulti + "倍");
            doDrop = giveOdds >= 1 || Math.random() < giveOdds;
            MMLuck.getInstance().getLogger().info("Player: " + playerName + ", Item: " + mmItemName + ", Chance: " +  (giveOdds * 100.0) + "%, amount: " + mmItemNumber + ", doDrop: " + doDrop);
        }
        // Check silent mode
        boolean silent = MMLuck.getInstance().boostHolder.isSilentMode(player.getUniqueId());
        if (doDrop) {
            GiveOverflowCommandExecutor.giveItems(player, mmItemName, mmItemNumber, silent, minimumRareMessageRarity, giveMultiplier - 100);
        }
        return true;
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

    public static String toFriendlyString(BigInteger number) {
        List<String> suffixes = Arrays.asList("", "", "万", "億", "兆", "京", "垓", "\uD855\uDF71", "穣", "溝", "澗", "正", "載", "極", "恒河沙", "阿僧祇", "那由他", "不可思議", "無量大数");
        int suffixNum = (int) Math.ceil(number.toString(10).length() / 4.0);
        String value = new DecimalFormat("0.00").format(new BigDecimal(number).divide(BigDecimal.valueOf(Math.pow(10000, suffixNum - 1)), RoundingMode.HALF_EVEN));
        String suffix = suffixes.get((int) suffixNum);
        return value + suffix;
    }
}
