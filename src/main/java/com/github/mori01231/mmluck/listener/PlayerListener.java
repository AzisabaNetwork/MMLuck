package com.github.mori01231.mmluck.listener;

import com.github.mori01231.mmluck.MMLuck;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // remove cached value
        MMLuck.getInstance().boostHolder.silentMode.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // cache value
        MMLuck.getInstance().boostHolder.isSilentMode(e.getPlayer().getUniqueId());
    }
}
