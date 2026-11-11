package com.github.mori01231.mmluck.commands

import com.github.mori01231.mmluck.MMLuck
import com.github.mori01231.mmluck.i18n.Messages
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.util.Locale
import java.util.UUID

/**
 * Base class for per-player on/off commands.
 *
 * `/<command> true|false` sets the value, `/<command>` toggles it.
 */
abstract class ToggleCommand(
    private val enabledKey: String,
    private val disabledKey: String,
) : TabExecutor {
    protected abstract fun get(uuid: UUID): Boolean

    protected abstract fun set(
        uuid: UUID,
        value: Boolean,
    )

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (sender !is Player) {
            Messages.send(sender, "common.player_only")
            return true
        }
        val uuid = sender.uniqueId
        val plugin = MMLuck.instance
        Bukkit.getScheduler().runTaskAsynchronously(
            plugin,
            Runnable {
                val value =
                    when (args.firstOrNull()?.lowercase(Locale.ROOT)) {
                        "true" -> true
                        "false" -> false
                        else -> !get(uuid)
                    }
                set(uuid, value)
                Bukkit.getScheduler().runTask(plugin, Runnable { Messages.send(sender, if (value) enabledKey else disabledKey) })
            },
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size != 1) return emptyList()
        val input = args[0].lowercase(Locale.ROOT)
        return SUGGESTIONS.filter { it.startsWith(input) }
    }

    private companion object {
        val SUGGESTIONS = listOf("true", "false")
    }
}

class SilentCommand : ToggleCommand("silent.enabled", "silent.disabled") {
    override fun get(uuid: UUID) = MMLuck.instance.boostHolder.isSilentMode(uuid)

    override fun set(
        uuid: UUID,
        value: Boolean,
    ) = MMLuck.instance.boostHolder.setSilentMode(uuid, value)
}

class VerboseCommand : ToggleCommand("verbose.enabled", "verbose.disabled") {
    override fun get(uuid: UUID) = MMLuck.instance.boostHolder.isVerboseMode(uuid)

    override fun set(
        uuid: UUID,
        value: Boolean,
    ) = MMLuck.instance.boostHolder.setVerboseMode(uuid, value)
}

class StashCommand : ToggleCommand("stash.mode_enabled", "stash.mode_disabled") {
    override fun get(uuid: UUID) = MMLuck.instance.boostHolder.isAlwaysStash(uuid)

    override fun set(
        uuid: UUID,
        value: Boolean,
    ) = MMLuck.instance.boostHolder.setAlwaysStash(uuid, value)
}
