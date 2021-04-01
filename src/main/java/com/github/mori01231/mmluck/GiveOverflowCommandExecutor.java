package com.github.mori01231.mmluck;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

import static org.bukkit.Bukkit.getPlayer;
import static org.bukkit.Bukkit.getServer;

public class GiveOverflowCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Usage /mlg プレイヤー名 アイテム名 確率 個数
        // 確率は少数で指定

        // declare those variables!
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
        }catch(Exception e){
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&cこの名前のプレイヤーは現在ログインしていません。"));
            return true;
        }

        // Acquire the luck value of the player
        double luckNumber;
        try{
            luckNumber = player.getAttribute(Attribute.GENERIC_LUCK).getValue();
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
        int giveMultiplier = 100 + (int)Math.round(luckNumber * 1); // multiplier in 0-100%
        int giveOdds = (int)Math.round(giveMultiplier * mmItemChance * 100); // odds in 0-100% * 10

        sendMessage(sender, player, "&3アイテムドロップ確率 ： " + giveOdds / 100.0 + "%");
        //sender.sendMessage("アイテムが渡される確率（1を超えている場合は実際は1扱いされます）：" + String.valueOf(giveOdds/100.0));

        String mmGiveString;
        int firstGiveNumber = 0;
        while(giveOdds > 10000){
            giveOdds -= 10000;
            firstGiveNumber += 1;
        }
        if(firstGiveNumber > 0){
            mmGiveString = "mm i give " + playerName + " " + mmItemName + " " + firstGiveNumber;
            sendCommand(mmGiveString);
        }


        // Generate random number
        Random rand = new Random();
        int rand_int1 = rand.nextInt(10000);

        // If the random number is lower than than the chance of getting item, give item.
        if (rand_int1 < giveOdds){
            mmGiveString = "mm i give " + playerName + " " + mmItemName + " " + mmItemNumber;
            sendCommand(mmGiveString);

            // Used for debug only
            // sender.sendMessage("アイテムが渡されました。実行されたコマンド：" + mmGiveString);
            // sendMessage(sender, player, "&3アイテムが渡されました。実行されたコマンド ： " + mmGiveString);
        }
        return true;
    }

    // function to make sending commands a lot shorter
    public void sendCommand(String command){
        getServer().dispatchCommand(getServer().getConsoleSender(), command);
    }

    // function to make sending messages a lot shorter
    public void sendMessage(CommandSender sender, Player player, String message){
        if(sender instanceof Player){
            player.sendActionBar('&',message);
        }else{
            sender.sendMessage(message);
        }

        if(!player.equals(sender)){
            player.sendActionBar('&',message);
        }
    }
}
