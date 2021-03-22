package com.github.mori01231.mmluck;

import org.bukkit.plugin.java.JavaPlugin;

public final class MMLuck extends JavaPlugin {

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
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("MythicLuck has been disabled.");
    }
}
