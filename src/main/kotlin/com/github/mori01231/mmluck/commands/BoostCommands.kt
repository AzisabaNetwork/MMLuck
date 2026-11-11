package com.github.mori01231.mmluck.commands

import com.github.mori01231.mmluck.MMLuck
import com.github.mori01231.mmluck.i18n.Messages
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.math.BigDecimal
import java.math.RoundingMode

private fun currentBoostPercentage(): Long = MMLuck.instance.boostHolder.refreshAndGetPercentage(force = false, immediate = true).join()

/** Formats a boost percentage as a multiplier, e.g. 50 -> "1.5". */
internal fun multiplierOf(percentage: Long): String = BigDecimal.valueOf(percentage + 100).movePointLeft(2).stripTrailingZeros().toPlainString()

/** `/addboosttime <duration in seconds> <percentage>` */
class AddBoostTimeCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (sender !is Player) {
            Messages.send(sender, "common.console_not_allowed")
            return true
        }
        val durationSeconds = args.getOrNull(0)?.toLongOrNull()
        val percentage = args.getOrNull(1)?.toLongOrNull()
        if (durationSeconds == null || percentage == null) {
            Messages.send(sender, "boost.usage")
            return true
        }
        val boostPercentage = currentBoostPercentage()
        MMLuck.instance.boostHolder.addBoost(System.currentTimeMillis(), durationSeconds, percentage)

        val signedPercentage = if (percentage >= 0) "+$percentage" else percentage.toString()
        Messages.broadcast(
            "boost.used",
            Messages.text("player", sender.name),
            Messages.text("percentage", signedPercentage),
            Messages.text("minutes", BigDecimal.valueOf(durationSeconds).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()),
        )
        Messages.broadcast("boost.current", Messages.text("multiplier", multiplierOf(percentage + boostPercentage)))
        return true
    }
}

/** `/broadcastboost` - announces the current boost if there is one. */
class BroadcastBoostCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        val boostPercentage = currentBoostPercentage()
        if (boostPercentage != 0L) {
            Messages.broadcast("boost.status", Messages.text("multiplier", multiplierOf(boostPercentage)))
        }
        return true
    }
}

/** `/checkboost` - shows the current boost and the remaining time of each boost. */
class CheckBoostCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        Messages.send(sender, "boost.status", Messages.text("multiplier", multiplierOf(currentBoostPercentage())))
        val now = System.currentTimeMillis()
        // expired boosts may remain until the next cleanup; don't show negative remaining time
        for (data in MMLuck.instance.boostHolder.getBoostData().filterNot { it.isExpired(now) }) {
            val remaining = data.remainingSeconds(now)
            Messages.send(
                sender,
                "boost.remaining",
                Messages.text("minutes", remaining / 60),
                Messages.text("seconds", remaining % 60),
                Messages.text("percentage", data.percentage),
            )
        }
        return true
    }
}
