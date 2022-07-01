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
        long duration;
        long percentage;

        // get values of variables from the command arguments
        try{
            duration = Long.parseLong(args[0]);
            percentage = Long.parseLong(args[1]);
        }catch(Exception e){
            sender.sendMessage("Invalid arguments");
            return true;
        }

        if (MMLuck.getInstance().boostHolder.refreshAndGetPercentage() >= 200) { // drop boosted by +200% or more (so player has 300%+ luck by boost)
            if (player != null) {
                String mmId = getMMId(duration, percentage);
                String cmd = "mm i give " + player.getName() + " " + mmId;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                MMLuck.getInstance().getLogger().info("Executed " + cmd);
                player.sendMessage(ChatColor.RED + "すでにブースト倍率が3倍以上のため、これ以上ブーストを追加できません。");
            } else {
                MMLuck.getInstance().getLogger().warning("Tried to refund the booster item but the sender is not a player");
                MMLuck.getInstance().getLogger().warning("Duration: " + duration + ", Percentage: " + percentage);
            }
        }

        // add boost to boostHolder
        MMLuck.getInstance().boostHolder.addBoost(currentTime,duration,percentage);

        FeedBack(player, isConsole, "&3Successfully added boost!");

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
