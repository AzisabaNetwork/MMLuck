package com.github.mori01231.mmluck.commands

import com.github.mori01231.mmluck.MMLuck
import com.github.mori01231.mmluck.i18n.Messages
import com.github.mori01231.mmluck.utils.DropCalculation
import com.github.mori01231.mmluck.utils.ItemDelivery
import net.azisaba.rarity.api.Rarity
import net.azisaba.rarity.api.RarityAPIProvider
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.math.BigDecimal
import java.math.RoundingMode

/** Arguments shared by /mlg and /mlgo: `<player> <item> <amount> <chance> [minimum rare message rarity]` */
private class GiveArguments(
    val mmItemName: String,
    val player: Player,
    val mmItemNumber: Int,
    val minimumRareMessageRarity: Rarity,
    val calculation: DropCalculation,
    boostPercentage: Int,
) {
    /** Global boost multiplier, e.g. "1.5". */
    val boostMultiplier: String = BigDecimal.valueOf(boostPercentage + 100L).movePointLeft(2).stripTrailingZeros().toPlainString()

    /** Chance after luck and boost are applied (1 = 100%, may exceed 1). */
    val chance: BigDecimal get() = calculation.boostedChanceDecimal

    /** Chance in percent, as a plain string. */
    val chancePercent: String get() = chance.movePointRight(2).stripTrailingZeros().toPlainString()

    /** Bonus from luck and boost in percent (e.g. 50.0 for x1.5). */
    val bonusPercent: Double get() = calculation.multiplierDecimal.subtract(BigDecimal.ONE).movePointRight(2).toDouble()

    fun giveItems(amount: Int) {
        val silent = MMLuck.instance.boostHolder.isSilentMode(player.uniqueId)
        ItemDelivery.giveItems(player, mmItemName, amount, silent, minimumRareMessageRarity, bonusPercent)
    }

    fun sendVerboseMessage(
        droppedAmount: Int,
        multiDrop: Boolean,
    ) {
        if (!MMLuck.instance.boostHolder.isVerboseMode(player.uniqueId)) return
        calculation.sendVerboseMessage(player, ItemDelivery.getItemName(player, mmItemName), droppedAmount, multiDrop)
    }

    fun log(result: String) {
        MMLuck.instance.logger.info("Player: ${player.name}, Item: $mmItemName, Chance: $chancePercent%, amount: $mmItemNumber, $result")
    }

    companion object {
        /** Parses the arguments, or sends an error message to the sender and returns null. */
        fun parse(
            sender: CommandSender,
            args: Array<out String>,
        ): GiveArguments? {
            if (args.size < 4) {
                Messages.send(sender, "give.invalid_args")
                return null
            }
            val player = Bukkit.getPlayer(args[0])
            if (player == null) {
                Messages.send(sender, "give.player_offline", Messages.text("player", args[0]))
                return null
            }
            val amount = args[2].toIntOrNull()?.takeIf { it >= 0 }
            if (amount == null) {
                Messages.send(sender, "give.invalid_amount", Messages.text("value", args[2]))
                return null
            }
            val chance = args[3].toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
            if (chance == null) {
                Messages.send(sender, "give.invalid_chance", Messages.text("value", args[3]))
                return null
            }
            val rarityId = args.getOrNull(4) ?: "rare"
            val rarity: Rarity? = RarityAPIProvider.get().getRarityById(rarityId)
            if (rarity == null) {
                Messages.send(sender, "give.invalid_rarity", Messages.text("value", rarityId))
                return null
            }
            val boostPercentage =
                Math.toIntExact(MMLuck.instance.boostHolder.refreshAndGetPercentage(force = false, immediate = true).join())
            val calculation = DropCalculation.capture(player, chance, boostPercentage)
            return GiveArguments(args[1], player, amount, rarity, calculation, boostPercentage)
        }
    }
}

private fun randomDecimal(): BigDecimal = BigDecimal.valueOf(Math.random())

/** `/mlg` - gives the item at most once (chances above 100% are treated as 100%). */
class GiveCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        val give = GiveArguments.parse(sender, args) ?: return true
        val player = give.player
        val chance = give.chance
        val odds =
            if (chance.signum() > 0) {
                // 1 / chance, at least "1 in 1"
                val oneIn = BigDecimal.ONE.divide(chance, 0, RoundingMode.HALF_EVEN).max(BigDecimal.ONE).toBigInteger()
                Messages.get(player, "odds", Messages.text("value", Messages.friendlyNumber(player, oneIn)))
            } else {
                Component.text("-")
            }
        Messages.actionBar(
            player,
            "give.chance",
            Messages.text("chance", give.chancePercent),
            Messages.component("odds", odds),
            Messages.text("multiplier", give.boostMultiplier),
        )
        val doDrop = randomDecimal() < chance
        give.log("doDrop: $doDrop")
        if (doDrop) {
            give.giveItems(give.mmItemNumber)
        }
        give.sendVerboseMessage(if (doDrop) give.mmItemNumber else 0, multiDrop = false)
        return true
    }
}

/** `/mlgo` - gives the item once per full 100% of chance, plus one more roll for the remainder. */
class GiveOverflowCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        val give = GiveArguments.parse(sender, args) ?: return true
        Messages.actionBar(
            give.player,
            "give.chance_simple",
            Messages.text("chance", give.chancePercent),
            Messages.text("multiplier", give.boostMultiplier),
        )

        var remaining = give.chance
        var amount = 0
        while (remaining > BigDecimal.ONE) {
            remaining -= BigDecimal.ONE
            amount += give.mmItemNumber
        }
        if (randomDecimal() < remaining) {
            amount += give.mmItemNumber
        }
        give.log("dropped amount: $amount")
        if (amount > 0) {
            give.giveItems(amount)
        }
        give.sendVerboseMessage(amount, multiDrop = true)
        return true
    }
}
