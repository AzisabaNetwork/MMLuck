package com.github.mori01231.mmluck;

import com.github.mori01231.mmluck.utils.ListStore;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MMLuck extends JavaPlugin {

    private int boostPercentage;
    private Long lastBoostPercentageCalculated;

    public ListStore BoostTimes;

    private static MMLuck instance;

    public MMLuck (){
        instance = this;
    }

    public static MMLuck getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("MythicLuck has been enabled.");
        this.getCommand("mythicluckgive").setExecutor(new GiveCommandExecutor());
        this.getCommand("mythicluckgiveoverflow").setExecutor(new GiveOverflowCommandExecutor());
        this.getCommand("addboosttime").setExecutor(new AddBoostTimeCommandExecutor());

        String pluginFolder = this.getDataFolder().getAbsolutePath();
        (new File(pluginFolder)).mkdirs();

        this.BoostTimes = new ListStore(new File(pluginFolder + File.separator + "HalloweenVoteSum.txt"));
        this.BoostTimes.Load();
        RemoveExpiredBoost();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("MythicLuck has been disabled.");
    }

    public void RemoveExpiredBoost(){
        // loop through boost information
        for(int i = 0; i < BoostTimes.getValues().size(); i++){

            // get and reformat a boost value
            String value = BoostTimes.getValues().get(i);
            String[] splitValues = value.split(":" ,3);

            // check if enough time has elapsed
            if( System.currentTimeMillis() > Long.parseLong(splitValues[1]) * 1000 + Long.parseLong(splitValues[0])){
                // remove boost entry
                BoostTimes.Remove(BoostTimes.getValues().get(i));
                BoostTimes.Save();
            }
        }
    }

    public int GetBoostValue(){
        if(System.currentTimeMillis() < lastBoostPercentageCalculated + 1000){
            return boostPercentage;
        }
        boostPercentage = 0;

        // loop through boost information
        for(int i = 0; i < BoostTimes.getValues().size(); i++){

            // get and reformat a boost value
            String value = BoostTimes.getValues().get(i);
            String[] splitValues = value.split(":" ,3);

            // check if enough time has elapsed
            if( System.currentTimeMillis() > Long.parseLong(splitValues[1]) * 1000 + Long.parseLong(splitValues[0])){
                // remove boost entry
                BoostTimes.Remove(BoostTimes.getValues().get(i));
                BoostTimes.Save();
            }else{
                // add boostPercentage
                boostPercentage += Integer.parseInt(splitValues[2]);
            }
        }
        lastBoostPercentageCalculated = System.currentTimeMillis();
        return boostPercentage;
    }
}
