package com.github.mori01231.mmluck.listener

import com.github.mori01231.lifecore.util.ItemUtil
import com.github.mori01231.mmluck.MMLuck
import com.github.mori01231.mmluck.commands.multiplierOf
import com.github.mori01231.mmluck.i18n.Messages
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType

class PlayerListener : Listener {
    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        // remove cached values
        val holder = MMLuck.instance.boostHolder
        val uuid = e.player.uniqueId
        holder.silentMode.remove(uuid)
        holder.verboseMode.remove(uuid)
        holder.alwaysStash.remove(uuid)
        holder.minimumStashRarity.remove(uuid)
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        // cache values
        val uuid = e.player.uniqueId
        Bukkit.getScheduler().runTaskAsynchronously(
            MMLuck.instance,
            Runnable {
                val holder = MMLuck.instance.boostHolder
                holder.isSilentMode(uuid)
                holder.isVerboseMode(uuid)
                holder.isAlwaysStash(uuid)
                holder.getMinimumStashRarity(uuid)
            },
        )
    }

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.action != Action.LEFT_CLICK_AIR && e.action != Action.LEFT_CLICK_BLOCK) return
        if (e.player.gameMode != GameMode.ADVENTURE && e.player.gameMode != GameMode.SURVIVAL) return
        val stack = e.item ?: return
        if (!stack.hasItemMeta()) return

        var durationMinutes = 0L
        var percentage = 0L
        val mythicType = ItemUtil.getMythicType(stack)
        if (mythicType != null && mythicType.startsWith("Donation_LuckScroll_")) {
            val parts = mythicType.split("_")
            durationMinutes = parts[2].replace("m", "").toLong()
            percentage = parts[3].replace("p", "").toLong()
        }
        val plugin = MMLuck.instance
        val pdc = stack.itemMeta.persistentDataContainer
        pdc.get(NamespacedKey(plugin, "duration"), PersistentDataType.LONG)?.let { durationMinutes = it }
        pdc.get(NamespacedKey(plugin, "percentage"), PersistentDataType.LONG)?.let { percentage = it }
        val originalAmount = pdc.get(NamespacedKey(plugin, "original_amount"), PersistentDataType.INTEGER)
        if (originalAmount != null && originalAmount < stack.amount) {
            val itemId = pdc.get(NamespacedKey(plugin, "item_id"), PersistentDataType.STRING)
            plugin.logger.warning("Player ${e.player.name} tried to use a duplicated item with id $itemId")
            plugin.logger.warning("original amount: $originalAmount, current amount: ${stack.amount}")
        }
        if (durationMinutes == 0L || percentage == 0L) return

        val boostPercentage = plugin.boostHolder.refreshAndGetPercentage(force = true, immediate = false).join()
        if (boostPercentage + percentage > 201) { // cap at +201% (allow 1% for debugging)
            Messages.send(e.player, "boost.cap")
            return
        }

        // add boost to boostHolder
        plugin.boostHolder.addBoost(System.currentTimeMillis(), durationMinutes * 60, percentage).join()
        stack.subtract(1)

        val signedPercentage = if (percentage >= 0) "+$percentage" else percentage.toString()
        Messages.broadcast(
            "boost.used",
            Messages.text("player", e.player.name),
            Messages.text("percentage", signedPercentage),
            Messages.text("minutes", durationMinutes),
        )
        Messages.broadcast("boost.current", Messages.text("multiplier", multiplierOf(percentage + boostPercentage)))
    }
}
