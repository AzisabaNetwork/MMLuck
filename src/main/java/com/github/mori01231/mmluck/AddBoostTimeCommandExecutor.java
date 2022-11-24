package com.github.mori01231.mmluck;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static org.bukkit.Bukkit.getLogger;

public class AddBoostTimeCommandExecutor implements CommandExecutor {

    // /addboosttime [duration] [percentage]
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = null;
        boolean isConsole;
        if ((sender instanceof Player)) {
            player = (Player) sender;
            isConsole = false;
        } else{
            isConsole = true;
        }

        // declare variables
        long currentTime = System.currentTimeMillis();
        long durationSeconds;
        long percentage;

        // get values of variables from the command arguments
        try{
            durationSeconds = Long.parseLong(args[0]);
            percentage = Long.parseLong(args[1]);
        }catch(Exception e){
            sender.sendMessage("Invalid arguments");
            return true;
        }

        long boostPercentage = MMLuck.getInstance().boostHolder.refreshAndGetPercentage();
        if (boostPercentage >= 125) { // luck boosted by +125% or more (effectively capped at +200%)
            if (player != null) {
                String mmId = getMMId(durationSeconds, percentage);
                String cmd = "mm i give " + player.getName() + " " + mmId;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                MMLuck.getInstance().getLogger().info("Executed " + cmd);
                player.sendMessage(ChatColor.RED + "すでにブースト倍率が2.25倍以上のため、これ以上ブーストを追加できません。");
            } else {
                MMLuck.getInstance().getLogger().warning("Tried to refund the booster item but the sender is not a player");
                MMLuck.getInstance().getLogger().warning("Duration: " + durationSeconds + ", Percentage: " + percentage);
            }
            return true;
        }

        // add boost to boostHolder
        MMLuck.getInstance().boostHolder.addBoost(currentTime,durationSeconds,percentage);

        FeedBack(player, isConsole, "&3Successfully added boost!");

        float minutes = durationSeconds / 60.0f;

        Bukkit.broadcastMessage(ChatColor.GOLD + "[ブースト] " + ChatColor.WHITE + ChatColor.BOLD + sender.getName() + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "が" +
                ChatColor.WHITE + ChatColor.BOLD + "+" + percentage + "%" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "ブーストを使用しました！" + ChatColor.GRAY + "(" + minutes + "分間有効)");

        float boostMulti = (boostPercentage + 100)/100.0f;

        Bukkit.broadcastMessage(ChatColor.GOLD + "[ブースト] " + ChatColor.LIGHT_PURPLE + "現在のブースト倍率は" + ChatColor.WHITE + ChatColor.BOLD + boostMulti + ChatColor.LIGHT_PURPLE + "倍です！");

        return true;
    }

    private String getMMId(long durationInSeconds, long percentage) {
        long durationInMinutes = durationInSeconds / 60;
        return "Donation_LuckScroll_" + durationInMinutes + "m_" + percentage + "p";
    }

    public void FeedBack(Player player, boolean isConsole, String message){
        if(isConsole){
            getLogger().info(ChatColor.translateAlternateColorCodes('&',message));
        }else{
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',message));
        }
    }
}
