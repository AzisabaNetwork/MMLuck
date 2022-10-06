package com.github.mori01231.mmluck;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// /silent true/false - sets the silent mode
// /silent - toggles the silent mode
public class SilentCommand implements TabExecutor {
    private static final List<String> SUGGESTIONS = Arrays.asList("true", "false");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }
        Player p = (Player) sender;
        Bukkit.getScheduler().runTaskAsynchronously(MMLuck.getInstance(), () -> {
            Boolean force = null;
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("true")) {
                    force = true;
                } else if (args[0].equalsIgnoreCase("false")) {
                    force = false;
                }
            }
            if (force == null) {
                force = !MMLuck.getInstance().boostHolder.isSilentMode(p.getUniqueId());
            }
            MMLuck.getInstance().boostHolder.setSilentMode(p.getUniqueId(), force);
            if (force) {
                p.sendMessage(ChatColor.RED + "アイテム獲得ログを非表示にしました。");
            } else {
                p.sendMessage(ChatColor.GREEN + "アイテム獲得ログを表示するようにしました。");
            }
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUGGESTIONS.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
