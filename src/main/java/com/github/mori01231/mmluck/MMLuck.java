package com.github.mori01231.mmluck;

import com.github.mori01231.mmluck.utils.BoostHolder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;

public final class MMLuck extends JavaPlugin {

    private int boostPercentage;
    private Long lastBoostPercentageCalculated = 0l;

    public BoostHolder boostHolder;

    private static MMLuck instance;

    public MMLuck (){
        instance = this;
    }

    public static MMLuck getInstance() {
        return instance;
    }

    public Connection connection;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("MythicLuck has been enabled.");
        this.getCommand("mythicluckgive").setExecutor(new GiveCommandExecutor());
        this.getCommand("mythicluckgiveoverflow").setExecutor(new GiveOverflowCommandExecutor());
        this.getCommand("addboosttime").setExecutor(new AddBoostTimeCommandExecutor());
        this.getCommand("broadcastboost").setExecutor(new BroadcastBoostCommandExecutor());
        this.getCommand("checkboost").setExecutor(new CheckBoostCommandExecutor());

        this.saveDefaultConfig();

        this.boostHolder = new BoostHolder();
        boostHolder.refreshAndGetPercentage();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                boostHolder.refreshAndGetPercentage();
                //Bukkit.broadcastMessage("Current percentage: " + boostHolder.refreshAndGetPercentage());

            }
        }, 0L, 100L); //0 Tick initial delay, 100 Tick (5 Second) between repeats
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try { //using a try catch to catch connection errors (like wrong sql password...)
            if (connection!=null && !connection.isClosed()){ //checking if connection isn't null to
                //avoid receiving a nullpointer
                connection.close(); //closing the connection field variable.
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        getLogger().info("MythicLuck has been disabled.");
    }
}
