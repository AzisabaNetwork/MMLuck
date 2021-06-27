package com.github.mori01231.mmluck;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BroadcastBoostCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Long boostPercentage = MMLuck.getInstance().boostHolder.refreshAndGetPercentage();

        if(boostPercentage == 0l)
            return true;

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',"&3現在ドロップ率が &f&l+" + boostPercentage + "%&3になっています！"));


        return true;
    }
}
