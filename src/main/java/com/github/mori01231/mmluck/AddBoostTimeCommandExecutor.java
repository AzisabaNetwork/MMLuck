package com.github.mori01231.mmluck;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.*;

import static org.bukkit.Bukkit.getLogger;

public class AddBoostTimeCommandExecutor implements CommandExecutor {

    private Boolean isConsole;
    Player player;


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if ((sender instanceof Player)) {
            player = (Player) sender;
            isConsole = false;
        } else{
            isConsole = true;
        }

        // declare variables
        Long currentTime = System.currentTimeMillis();
        Long duration;
        Long percentage;

        // get values of variables from the command arguments
        try{
            duration = Long.valueOf(args[0]);
            percentage = Long.valueOf(args[1]);
        }catch(Exception e){
            sender.sendMessage("Invalid arguments");
            return true;
        }

        // add boost to boostHolder
        MMLuck.getInstance().boostHolder.addBoost(currentTime,duration,percentage);

        FeedBack("&3Successfully added boost!");

        return true;
    }

    public void FeedBack(String message){
        if(isConsole){
            getLogger().info(ChatColor.translateAlternateColorCodes('&',message));
        }else{
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',message));
        }
    }
}
