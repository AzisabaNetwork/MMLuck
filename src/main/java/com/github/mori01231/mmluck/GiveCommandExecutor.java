package com.github.mori01231.mmluck;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Random;

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


        // Calculate the odds the player will be getting the item
        int giveMultiplier = (int) Math.round((100 + luckNumber) * boostMulti); // multiplier in 0-100%
        int giveOdds = (int)Math.round(giveMultiplier * mmItemChance * 100); // odds in 0-100% * 10

        if(giveOdds > 10000){
            giveOdds = 10000;
        }

        sendMessage(sender, player, "&3アイテムドロップ確率 ： " + giveOdds / 100.0 + "%     ブースト倍率 : &f&l+" + boostMulti +"倍");
        //sender.sendMessage("アイテムが渡される確率（1を超えている場合は実際は1扱いされます）：" + String.valueOf(giveOdds/100.0));

        // Generate random number
        Random rand = new Random();
        int rand_int1 = rand.nextInt(10000);

        // Check silent mode
        boolean silent = MMLuck.getInstance().boostHolder.isSilentMode(player.getUniqueId());
        // If the random number is lower than the chance of getting item, give item.
        boolean doDrop = rand_int1 < giveOdds;
        MMLuck.getInstance().getLogger().info("Player: " + playerName + ", Item: " + mmItemName + ", Chance: " +  giveOdds / 100.0 + "%, amount: " + mmItemNumber + ", doDrop: " + doDrop);
        if (doDrop) {
            GiveOverflowCommandExecutor.giveItems(player, mmItemName, mmItemNumber, silent);
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
}
