package com.github.mori01231.mmluck.utils

import com.github.mori01231.mmluck.MMLuck
import com.github.mori01231.mmluck.i18n.Messages
import io.lumine.mythic.bukkit.MythicBukkit
import net.azisaba.itemstash.ItemStash
import net.azisaba.loreeditor.api.item.CraftItemStack
import net.azisaba.rarity.api.Rarity
import net.azisaba.rarity.api.RarityAPIProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.acrylicstyle.storageBox.utils.StorageBoxUtils

/**
 * Gives MythicMobs items to players, handling StorageBox, ItemStash and drop messages.
 */
object ItemDelivery {
    private val NORMAL_RARE_IDS = setOf("common", "uncommon", "rare")

    /** Returns the display name of the MythicMobs item, with expressions evaluated. */
    fun getItemName(
        player: Player,
        mmItemId: String,
    ): Component {
        val stack = MythicBukkit.inst().itemManager.getItemStack(mmItemId) ?: return Component.text(mmItemId)
        if (!stack.hasItemMeta()) return Component.text(mmItemId)
        val meta = stack.itemMeta
        val name = meta.displayName()
        if (!meta.hasDisplayName() || name == null) return Component.text(mmItemId)
        return Expr.evalAndReplace(player, Text.toLegacy(name), "$mmItemId (name)")?.let(Text::fromLegacy) ?: name
    }

    fun giveItems(
        player: Player,
        mmItemId: String,
        amount: Int,
        silent: Boolean,
        minimumRareMessageRarity: Rarity,
        boostedChance: Double,
    ) {
        val original = MythicBukkit.inst().itemManager.getItemStack(mmItemId)
        if (original == null) {
            Messages.actionBar(player, "item.not_found", Messages.text("id", mmItemId))
            return
        }
        // we need to do this because the item stack is bugged at the moment
        val stack = CraftItemStack.STATIC.asCraftMirror(requireNotNull(CraftItemStack.STATIC.asNMSCopy(original)))
        evaluateExpressions(player, mmItemId, stack)

        val name = stack.itemMeta?.takeIf { stack.hasItemMeta() && it.hasDisplayName() }?.displayName() ?: Component.text(mmItemId)
        val nameComponent = name.hoverEvent(HoverEvent.showText(Component.text(mmItemId)))
        stack.amount = amount

        val holder = MMLuck.instance.boostHolder
        val alwaysStash = holder.isAlwaysStash(player.uniqueId)
        var doStash = alwaysStash
        try {
            val rarity = RarityAPIProvider.get().getRarityByItemStack(stack)
            if (rarity == null) {
                doStash = true
            } else {
                if (rarity.weight >= holder.getMinimumStashRarity(player.uniqueId)) {
                    doStash = true
                }
                if (rarity.weight >= minimumRareMessageRarity.weight) {
                    doStash = true
                    Messages.send(
                        player,
                        if (rarity.id in NORMAL_RARE_IDS) "drop.rare" else "drop.super_rare",
                        Messages.style("rarity_color", rarityColor(rarity, player)),
                        Messages.component("item", nameComponent),
                        Messages.text("amount", stack.amount),
                        Messages.text("boost", Math.round(boostedChance)),
                    )
                }
            }
        } catch (e: Exception) {
            doStash = true
            MMLuck.instance.slF4JLogger.warn("Failed to get rarity of {}, assuming alwaysStash", mmItemId, e)
        }

        val entry = StorageBoxUtils.getStorageBoxForType(player.inventory, stack)
        val items: Collection<ItemStack> =
            when {
                entry != null -> {
                    val storageBox = entry.value
                    storageBox.amount = storageBox.amount + stack.amount
                    player.inventory.setItem(entry.key, storageBox.itemStack)
                    emptyList()
                }
                alwaysStash -> listOf(stack)
                else -> player.inventory.addItem(stack).values
            }
        val droppedAmount = items.sumOf { it.amount }
        val actualAmount = amount - droppedAmount
        if (!silent) {
            if (actualAmount > 0 || alwaysStash) {
                Messages.send(
                    player,
                    "drop.obtained",
                    Messages.component("item", nameComponent),
                    Messages.text("amount", if (alwaysStash) amount else actualAmount),
                )
            } else {
                Messages.send(player, "drop.obtained_none", Messages.component("item", nameComponent), Messages.text("amount", actualAmount))
            }
        }

        val stashItems = doStash
        Bukkit.getScheduler().runTaskAsynchronously(
            MMLuck.instance,
            Runnable {
                if (stashItems && items.isNotEmpty() && items.all { addToStash(player, it) } && !silent && !alwaysStash) {
                    Messages.send(player, "stash.added", Messages.text("amount", droppedAmount), Messages.component("item", name))
                    Messages.send(player, "stash.pickup_hint")
                }
            },
        )
    }

    /** Evaluates `${...}` expressions in the display name and lore of the item. */
    private fun evaluateExpressions(
        player: Player,
        mmItemId: String,
        stack: ItemStack,
    ) {
        if (!stack.hasItemMeta()) return
        val meta = stack.itemMeta
        var modified = false
        meta.lore()?.let { lore ->
            meta.lore(
                lore.map { line ->
                    Expr.evalAndReplace(player, Text.toLegacy(line), "$mmItemId (lore)")?.let {
                        modified = true
                        Text.fromLegacy(it)
                    } ?: line
                },
            )
        }
        val name = meta.displayName()
        if (meta.hasDisplayName() && name != null) {
            Expr.evalAndReplace(player, Text.toLegacy(name), "$mmItemId (name)")?.let {
                modified = true
                meta.displayName(Text.fromLegacy(it))
            }
        }
        if (modified) {
            stack.itemMeta = meta
        }
    }

    private fun rarityColor(
        rarity: Rarity,
        player: Player,
    ): TextColor {
        val name = Text.translateAmpersand(rarity.getDisplayName(player))
        if (name.length < 2 || name[0] != LegacyComponentSerializer.SECTION_CHAR) return NamedTextColor.WHITE
        return LegacyComponentSerializer.parseChar(name[1])?.color() ?: NamedTextColor.WHITE
    }

    /**
     * Adds the item to the stash if ItemStash is installed.
     *
     * @return true if the item was added to the stash
     */
    fun addToStash(
        player: Player,
        item: ItemStack,
    ): Boolean {
        if (Bukkit.getPluginManager().getPlugin("ItemStash") == null) return false
        return try {
            ItemStash.getInstance().addItemToStash(player.uniqueId, item)
            true
        } catch (e: Exception) {
            MMLuck.instance.slF4JLogger.error("Failed to add item to stash: {}; falling back to addItem -> drop", item, e)
            Bukkit.getScheduler().runTask(
                MMLuck.instance,
                Runnable {
                    for (value in player.inventory.addItem(item).values) {
                        player.world.dropItem(player.location, value)
                    }
                },
            )
            false
        }
    }
}
