package com.github.mori01231.mmluck.listener;

import com.github.mori01231.mmluck.MMLuck;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MMCommandListener implements Listener {
    private static final Set<String> COMMANDS = new HashSet<>(
            Arrays.asList("mythicmobs:mythicmobs", "mythicmobs:mm", "mythicmobs", "mm",
                    "/mythicmobs:mythicmobs", "/mythicmobs:mm", "/mythicmobs", "/mm")
    );

    @EventHandler
    public void onCommand(ServerCommandEvent e) {
        List<String> cmdAndArgs = new ArrayList<>(Arrays.asList(e.getCommand().split(" ")));
        // /mm i give [-s] <name> <item>
        if (cmdAndArgs.size() != 5) {
            return;
        }
        if (!COMMANDS.contains(cmdAndArgs.get(0))) {
            return;
        }
        if (!cmdAndArgs.get(1).equals("i") && !cmdAndArgs.get(1).equals("items")) {
            return;
        }
        if (!cmdAndArgs.get(2).equals("give")) {
            return;
        }
        String playerName = cmdAndArgs.get(3);
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (MMLuck.getInstance().boostHolder.isSilentMode(uuid)) {
            cmdAndArgs.add(3, "-s");
            e.setCommand(String.join(" ", cmdAndArgs));
        }
    }
}
