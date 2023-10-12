package com.github.mori01231.mmluck;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddBoostTimeCommandExecutor implements CommandExecutor {

    // /addboosttime [duration] [percentage]
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "コンソールからは実行できません。");
            return true;
        }

        Player player = (Player) sender;

        // declare variables
        long currentTime = System.currentTimeMillis();
        long durationSeconds;
        long percentage;

        // get values of variables from the command arguments
        try{
            durationSeconds = Long.parseLong(args[0]);
            percentage = Long.parseLong(args[1]);
        }catch(Exception e){
            sender.sendMessage("/abt [duration in seconds] [percentage]");
            return true;
        }

        long boostPercentage = MMLuck.getInstance().boostHolder.refreshAndGetPercentage(false, true).join();
        if (false && (boostPercentage + percentage) >= 200) { // cap at +200%
            player.sendMessage(ChatColor.RED + "すでにブースト倍率が2.25倍以上のため、これ以上ブーストを追加できません。");
            return true;
        }

        // add boost to boostHolder
        MMLuck.getInstance().boostHolder.addBoost(currentTime,durationSeconds,percentage);

        float minutes = durationSeconds / 60.0f;

        String sp = Long.toString(percentage);
        if (percentage >= 0) {
            sp = "+" + sp;
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "[ブースト] " + ChatColor.WHITE + ChatColor.BOLD + sender.getName() + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "が" +
                ChatColor.WHITE + ChatColor.BOLD + sp + "%" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "ブーストを使用しました！" + ChatColor.GRAY + "(" + minutes + "分間有効)");

        float boostMulti = (percentage + boostPercentage + 100)/100.0f;

        Bukkit.broadcastMessage(ChatColor.GOLD + "[ブースト] " + ChatColor.LIGHT_PURPLE + "現在のブースト倍率は" + ChatColor.WHITE + ChatColor.BOLD + boostMulti + ChatColor.LIGHT_PURPLE + "倍です！");

        return true;
    }
}
