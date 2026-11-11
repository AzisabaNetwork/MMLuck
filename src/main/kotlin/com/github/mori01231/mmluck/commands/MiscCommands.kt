package com.github.mori01231.mmluck.commands

import com.github.mori01231.mmluck.MMLuck
import com.github.mori01231.mmluck.gui.MinimumStashRarityGui
import com.github.mori01231.mmluck.i18n.Messages
import com.github.mori01231.mmluck.utils.Expr
import com.github.mori01231.mmluck.utils.Text
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.math.BigDecimal
import java.util.UUID

/** `/giveboostitem <player> <duration in minutes> <percentage> [amount] [owner]` */
class GiveBoostItemCommand : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (args.size < 3) {
            Messages.send(sender, "boost_item.usage")
            return true
        }
        val player = Bukkit.getPlayerExact(args[0])
        if (player == null) {
            Messages.send(sender, "boost_item.player_not_found")
            return true
        }
        val durationMinutes = args[1].toLongOrNull()
        val percentage = args[2].toLongOrNull()
        val amount = args.getOrNull(3)?.toIntOrNull() ?: 1
        if (durationMinutes == null || percentage == null) {
            Messages.send(sender, "boost_item.usage")
            return true
        }
        var playerName = player.name
        var playerUUID = player.uniqueId
        args.getOrNull(4)?.let { Bukkit.getOfflinePlayerIfCached(it) }?.let { owner ->
            val ownerName = owner.name
            if (owner.uniqueId.version() == 4 && ownerName != null) {
                playerName = ownerName
                playerUUID = owner.uniqueId
            }
        }
        player.inventory.addItem(createItem(player, durationMinutes, percentage, amount, playerName, playerUUID))
        return true
    }

    private fun createItem(
        player: Player,
        durationMinutes: Long,
        percentage: Long,
        amount: Int,
        ownerName: String,
        ownerUUID: UUID,
    ): ItemStack {
        val multi = BigDecimal.valueOf(percentage + 100).movePointLeft(2).stripTrailingZeros().toPlainString()
        val resolvers =
            arrayOf(
                Messages.text("minutes", durationMinutes),
                Messages.text("multiplier", multi),
                Messages.text("percentage", percentage),
                Messages.text("player", ownerName),
                Messages.text("uuid", ownerUUID),
            )
        val plugin = MMLuck.instance
        val stack = ItemStack.of(Material.PAPER, amount)
        stack.editMeta { meta ->
            // legacy integer custom model data 5 == floats[0] = 5 (1.21.4+)
            meta.setCustomModelDataComponent(meta.customModelDataComponent.apply { floats = listOf(5f) })
            meta.addEnchant(Enchantment.MENDING, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            meta.displayName(Text.item(Messages.get(player, "boost_item.name", *resolvers)))
            meta.lore(Messages.lines(player, "boost_item.lore", *resolvers).map(Text::item))
            val pdc = meta.persistentDataContainer
            pdc.set(NamespacedKey(plugin, "duration"), PersistentDataType.LONG, durationMinutes)
            pdc.set(NamespacedKey(plugin, "percentage"), PersistentDataType.LONG, percentage)
            pdc.set(NamespacedKey(plugin, "player_name"), PersistentDataType.STRING, ownerName)
            pdc.set(NamespacedKey(plugin, "player_uuid"), PersistentDataType.STRING, ownerUUID.toString())
            pdc.set(NamespacedKey(plugin, "original_amount"), PersistentDataType.INTEGER, amount)
            pdc.set(NamespacedKey(plugin, "item_id"), PersistentDataType.STRING, UUID.randomUUID().toString())
        }
        return stack
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()
}

/** `/mmluckeval <expression>` - evaluates an expression as the player (for debugging). */
class MMLuckEvalCommand : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (sender !is Player) return true
        try {
            sender.sendPlainMessage(Expr.eval(sender, args.joinToString(" ")).toString())
        } catch (e: Exception) {
            Messages.send(sender, "eval.error", Messages.text("error", "${e.javaClass.name}: ${e.message}"))
            MMLuck.instance.slF4JLogger.warn("Failed to evaluate expression", e)
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (sender !is Player) return emptyList()
        return try {
            Expr.getSuggestions(sender, args.joinToString(" "))
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            listOf(message.take(150))
        }
    }
}

/** `/minimumstashrarity` - opens the GUI to choose the minimum rarity that goes to the stash. */
class MinimumStashRarityCommand : TabExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (sender is Player) {
            sender.openInventory(MinimumStashRarityGui(sender).inventory)
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> = emptyList()
}
