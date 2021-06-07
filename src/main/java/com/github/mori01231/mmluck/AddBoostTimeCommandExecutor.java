package com.github.mori01231.mmluck;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AddBoostTimeCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // send plugin message to other servers to save

        // declare variables
        String currentTime = Long.toString(System.currentTimeMillis());
        String duration;
        String percentage;

        // get values of variables from the command arguments
        try{
            duration = args[0];
            percentage = args[1];
        }catch(Exception e){
            sender.sendMessage("Invalid arguments");
            return true;
        }

        // add boost information to ListStore
        MMLuck.getInstance().BoostTimes.Add( currentTime + ":" + duration + ":" + percentage);
        MMLuck.getInstance().BoostTimes.Save();

        // loop through boost information and remove expired boosts
        MMLuck.getInstance().RemoveExpiredBoost();

        return true;
    }
}
