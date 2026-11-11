package com.github.mori01231.mmluck

import com.github.mori01231.mmluck.commands.AddBoostTimeCommand
import com.github.mori01231.mmluck.commands.BroadcastBoostCommand
import com.github.mori01231.mmluck.commands.CheckBoostCommand
import com.github.mori01231.mmluck.commands.GiveBoostItemCommand
import com.github.mori01231.mmluck.commands.GiveCommand
import com.github.mori01231.mmluck.commands.GiveOverflowCommand
import com.github.mori01231.mmluck.commands.MMLuckEvalCommand
import com.github.mori01231.mmluck.commands.MinimumStashRarityCommand
import com.github.mori01231.mmluck.commands.SilentCommand
import com.github.mori01231.mmluck.commands.StashCommand
import com.github.mori01231.mmluck.commands.VerboseCommand
import com.github.mori01231.mmluck.gui.MinimumStashRarityGui
import com.github.mori01231.mmluck.i18n.Messages
import com.github.mori01231.mmluck.listener.PlayerListener
import com.github.mori01231.mmluck.utils.BoostHolder
import org.bukkit.command.CommandExecutor
import org.bukkit.plugin.java.JavaPlugin

class MMLuck : JavaPlugin() {
    lateinit var boostHolder: BoostHolder
        private set

    init {
        instance = this
    }

    override fun onEnable() {
        saveDefaultConfig()
        Messages.load(this)

        boostHolder = BoostHolder()
        boostHolder.refreshAndGetPercentage(force = false, immediate = true)

        register("mythicluckgive", GiveCommand())
        register("mythicluckgiveoverflow", GiveOverflowCommand())
        register("addboosttime", AddBoostTimeCommand())
        register("broadcastboost", BroadcastBoostCommand())
        register("checkboost", CheckBoostCommand())
        register("mythiclucksilent", SilentCommand())
        register("mythicluckverbose", VerboseCommand())
        register("mythicluckstash", StashCommand())
        register("giveboostitem", GiveBoostItemCommand())
        register("mmluckeval", MMLuckEvalCommand())
        register("minimumstashrarity", MinimumStashRarityCommand())

        server.pluginManager.registerEvents(PlayerListener(), this)
        server.pluginManager.registerEvents(MinimumStashRarityGui.EventListener(), this)

        // refresh the boost percentage every 5 seconds
        server.scheduler.runTaskTimer(this, Runnable { boostHolder.refreshAndGetPercentage(force = false, immediate = false) }, 100L, 100L)

        logger.info("MythicLuck has been enabled.")
    }

    override fun onDisable() {
        if (::boostHolder.isInitialized) {
            try {
                boostHolder.close()
            } catch (e: Exception) {
                slF4JLogger.warn("Failed to close the database connection", e)
            }
        }
        logger.info("MythicLuck has been disabled.")
    }

    private fun register(
        name: String,
        executor: CommandExecutor,
    ) {
        val command = getCommand(name) ?: error("Command $name is not defined in plugin.yml")
        command.setExecutor(executor)
    }

    companion object {
        lateinit var instance: MMLuck
            private set
    }
}
