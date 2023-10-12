package com.github.mori01231.mmluck;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BroadcastBoostCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Long boostPercentage = MMLuck.getInstance().boostHolder.refreshAndGetPercentage(false, true).join();

        if(boostPercentage == 0l)
            return true;

        float boostMulti = (boostPercentage + 100)/100.0f;

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',"&3現在ドロップブースト倍率が &f&l" + boostMulti + "倍&3になっています！"));


        return true;
    }
}
