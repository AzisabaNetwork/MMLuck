package com.github.mori01231.mmluck.utils

import com.github.mori01231.mmluck.i18n.Messages
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import java.math.BigDecimal

/**
 * Snapshot of a drop chance calculation, used to show the verbose drop log (/mlv).
 */
class DropCalculation private constructor(
    private val originalChance: BigDecimal,
    private val multiplier: BigDecimal,
    private val breakdown: LuckBreakdown,
) {
    private val boostedChance: BigDecimal = originalChance.multiply(multiplier)

    /** Chance after luck and boost are applied (1 = 100%, may exceed 1). */
    val boostedChanceDecimal: BigDecimal get() = boostedChance

    /** Combined luck and boost multiplier (1 = no bonus). */
    val multiplierDecimal: BigDecimal get() = multiplier

    /** The chance actually used by /mlg (clamped to 0-100%). */
    private val mlgEffectiveChance: BigDecimal get() = boostedChance.max(BigDecimal.ZERO).min(BigDecimal.ONE)

    fun sendVerboseMessage(
        player: Player,
        itemName: Component,
        droppedAmount: Int,
        multiDrop: Boolean,
    ) {
        val chance = if (multiDrop) boostedChance else mlgEffectiveChance
        val hover =
            Messages.get(
                player,
                "verbose.hover",
                Messages.text("original_chance", formatPercent(originalChance)),
                Messages.component("multi_drop", Messages.get(player, if (multiDrop) "common.enabled" else "common.disabled")),
                Messages.text("total_luck", formatSigned(breakdown.totalLuck)),
                Messages.text("equipment", formatSigned(breakdown.equipment)),
                Messages.text("main_hand", formatSigned(breakdown.mainHand)),
                Messages.text("off_hand", formatSigned(breakdown.offHand)),
                Messages.text("potion", formatSigned(breakdown.potion)),
                Messages.text("other", formatSigned(breakdown.other)),
                Messages.text("boost_multiplier", format(breakdown.globalBoostMultiplier)),
                Messages.text("multiplier", format(multiplier)),
                Messages.text("boosted_chance", formatPercent(boostedChance)),
            )
        val message =
            Messages.get(
                player,
                "verbose.line",
                Messages.component("item", itemName),
                Messages.text("amount", droppedAmount),
                Messages.text("chance", formatPercent(chance)),
            )
        player.sendMessage(message.hoverEvent(HoverEvent.showText(hover)))
    }

    private class LuckBreakdown(
        val equipment: BigDecimal,
        val mainHand: BigDecimal,
        val offHand: BigDecimal,
        val potion: BigDecimal,
        val other: BigDecimal,
        val totalLuck: BigDecimal,
        globalBoost: Int,
    ) {
        val globalBoostMultiplier: BigDecimal = BigDecimal.valueOf(globalBoost + 100L).movePointLeft(2)

        companion object {
            fun capture(
                player: Player,
                globalBoost: Int,
            ): LuckBreakdown {
                val attribute = player.getAttribute(Attribute.LUCK)
                val totalLuck = BigDecimal.valueOf(attribute?.value ?: 0.0)
                // modifiers currently applied to the player; only these are counted from items
                val activeModifiers: Set<Key> = attribute?.modifiers?.mapTo(HashSet()) { it.key() }.orEmpty()
                val counted = HashSet<Key>()
                val inventory = player.inventory
                val equipment =
                    listOf(inventory.helmet, inventory.chestplate, inventory.leggings, inventory.boots)
                        .fold(BigDecimal.ZERO) { sum, item -> sum.add(sumItemLuck(item, activeModifiers, counted)) }
                val mainHand = sumItemLuck(inventory.itemInMainHand, activeModifiers, counted)
                val offHand = sumItemLuck(inventory.itemInOffHand, activeModifiers, counted)
                val potion = getPotionLuck(player)
                val other = totalLuck.subtract(equipment).subtract(mainHand).subtract(offHand).subtract(potion)
                return LuckBreakdown(equipment, mainHand, offHand, potion, other, totalLuck, globalBoost)
            }

            private fun sumItemLuck(
                stack: ItemStack?,
                activeModifiers: Set<Key>,
                counted: MutableSet<Key>,
            ): BigDecimal {
                if (stack == null || !stack.hasItemMeta()) return BigDecimal.ZERO
                val modifiers = stack.itemMeta.getAttributeModifiers(Attribute.LUCK) ?: return BigDecimal.ZERO
                return modifiers
                    .filter { it.key() in activeModifiers && counted.add(it.key()) && it.operation == AttributeModifier.Operation.ADD_NUMBER }
                    .fold(BigDecimal.ZERO) { sum, modifier -> sum.add(BigDecimal.valueOf(modifier.amount)) }
            }

            private fun getPotionLuck(player: Player): BigDecimal {
                var luck = 0L
                for (effect in player.activePotionEffects) {
                    when (effect.type) {
                        PotionEffectType.LUCK -> luck += effect.amplifier + 1
                        PotionEffectType.UNLUCK -> luck -= effect.amplifier + 1
                    }
                }
                return BigDecimal.valueOf(luck)
            }
        }
    }

    companion object {
        fun capture(
            player: Player,
            originalChance: BigDecimal,
            globalBoostPercentage: Int,
        ): DropCalculation {
            val breakdown = LuckBreakdown.capture(player, globalBoostPercentage)
            val boostMultiplier = BigDecimal.valueOf(globalBoostPercentage + 100L).movePointLeft(2)
            val multiplier = breakdown.totalLuck.add(BigDecimal.valueOf(100L)).movePointLeft(2).multiply(boostMultiplier)
            return DropCalculation(originalChance, multiplier, breakdown)
        }

        private fun formatSigned(value: BigDecimal): String = if (value.signum() >= 0) "+" + format(value) else format(value)

        private fun formatPercent(value: BigDecimal): String = format(value.movePointRight(2))

        private fun format(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
    }
}
