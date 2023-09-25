package com.github.mori01231.mmluck;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CheckBoostCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Long boostPercentage = MMLuck.getInstance().boostHolder.refreshAndGetPercentage();

        float boostMulti = (boostPercentage + 100)/100.0f;

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&3現在ドロップブースト倍率が &f&l" + boostMulti + "倍&3になっています！"));
        for (BoostData data : MMLuck.getInstance().boostHolder.getBoostData()) {
            long remaining = ((data.startTime + data.duration * 1000) - System.currentTimeMillis()) / 1000;
            long minutes = remaining / 60;
            long seconds = remaining % 60;
            sender.sendMessage(ChatColor.GOLD + "あと" + ChatColor.RED + minutes + "分" + seconds + "秒" + ChatColor.GOLD + ": " + ChatColor.GREEN + ChatColor.BOLD + "+" + data.percentage + "%");
        }


        return true;
    }
}
