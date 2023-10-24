package com.github.mori01231.mmluck;

import com.github.mori01231.mmluck.utils.Expr;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MMLuckEvalCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;
        String src = String.join(" ", args);
        try {
            sender.sendMessage(String.valueOf(Expr.eval(player, src)));
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        try {
            return Expr.getSuggestions(sender, String.join(" ", args)).collect(Collectors.toList());
        } catch (Exception e) {
            String message = e.getMessage();
            return Collections.singletonList(ChatColor.RED + message.substring(0, Math.min(150, message.length())));
        }
    }
}
