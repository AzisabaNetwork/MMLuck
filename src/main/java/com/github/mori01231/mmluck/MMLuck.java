package com.github.mori01231.mmluck;

import com.github.mori01231.mmluck.listener.PlayerListener;
import com.github.mori01231.mmluck.utils.BoostHolder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.util.Objects;

public final class MMLuck extends JavaPlugin {

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
        Objects.requireNonNull(this.getCommand("mythicluckgive")).setExecutor(new GiveCommandExecutor());
        Objects.requireNonNull(this.getCommand("mythicluckgiveoverflow")).setExecutor(new GiveOverflowCommandExecutor());
        Objects.requireNonNull(this.getCommand("addboosttime")).setExecutor(new AddBoostTimeCommandExecutor());
        Objects.requireNonNull(this.getCommand("broadcastboost")).setExecutor(new BroadcastBoostCommandExecutor());
        Objects.requireNonNull(this.getCommand("checkboost")).setExecutor(new CheckBoostCommandExecutor());
        Objects.requireNonNull(this.getCommand("mythiclucksilent")).setExecutor(new SilentCommand());
        Objects.requireNonNull(this.getCommand("giveboostitem")).setExecutor(new GiveBoostItemCommandExecutor());

        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);

        this.saveDefaultConfig();

        this.boostHolder = new BoostHolder();
        boostHolder.refreshAndGetPercentage();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            boostHolder.refreshAndGetPercentage();
            //Bukkit.broadcastMessage("Current percentage: " + boostHolder.refreshAndGetPercentage());
        }, 100L, 100L); // 100 Ticks initial delay, 100 Tick (5 Second) between repeats
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
