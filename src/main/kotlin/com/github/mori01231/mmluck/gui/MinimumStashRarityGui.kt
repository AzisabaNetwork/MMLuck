package com.github.mori01231.mmluck.gui

import com.github.mori01231.mmluck.MMLuck
import com.github.mori01231.mmluck.i18n.Messages
import com.github.mori01231.mmluck.utils.Text
import net.azisaba.rarity.api.RarityAPIProvider
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class MinimumStashRarityGui(
    private val player: Player,
) : InventoryHolder {
    private val inventory: Inventory = Bukkit.createInventory(this, 9, Messages.get(player, "gui.min_stash.title"))
    private val slotToRarityWeight = HashMap<Int, Int>()

    init {
        reset()
    }

    fun reset() {
        val minimumStashRarityWeight = MMLuck.instance.boostHolder.getMinimumStashRarity(player.uniqueId)
        val closeButton =
            ItemStack.of(Material.BARRIER).apply {
                editMeta { it.displayName(Text.item(Messages.get(player, "gui.close"))) }
            }
        inventory.setItem(0, closeButton)
        inventory.setItem(8, closeButton.clone())

        RARITY_SLOTS.forEachIndexed { index, (rarityId, material) ->
            val slot = index + 1
            val rarity = RarityAPIProvider.get().getRarityById(rarityId)
            slotToRarityWeight[slot] = rarity.weight
            val stateKey = if (rarity.weight >= minimumStashRarityWeight) "common.enabled" else "common.disabled"
            inventory.setItem(
                slot,
                ItemStack.of(material).apply {
                    editMeta { meta ->
                        meta.displayName(Text.item(Text.fromAmpersand(rarity.getDisplayName(player))))
                        meta.lore(listOf(Text.item(Messages.get(player, stateKey))))
                    }
                },
            )
        }
    }

    override fun getInventory(): Inventory = inventory

    class EventListener : Listener {
        @EventHandler
        fun onInventoryClick(e: InventoryClickEvent) {
            val player = e.whoClicked as? Player ?: return
            if (e.inventory.getHolder(false) !is MinimumStashRarityGui) return
            e.isCancelled = true
            val gui = e.clickedInventory?.getHolder(false) as? MinimumStashRarityGui ?: return
            if (e.slot == 0 || e.slot == 8) {
                player.closeInventory()
                return
            }
            val weight = gui.slotToRarityWeight[e.slot] ?: return
            MMLuck.instance.boostHolder.setMinimumStashRarity(player.uniqueId, weight)
            gui.reset()
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_HAT, 10f, 1f)
        }

        @EventHandler
        fun onInventoryDrag(e: InventoryDragEvent) {
            if (e.inventory.getHolder(false) is MinimumStashRarityGui) {
                e.isCancelled = true
            }
        }
    }

    private companion object {
        val RARITY_SLOTS =
            listOf(
                "common" to Material.COBBLESTONE,
                "uncommon" to Material.IRON_BLOCK,
                "rare" to Material.GOLD_BLOCK,
                "epic" to Material.EMERALD_BLOCK,
                "legendary" to Material.DIAMOND_BLOCK,
                "mythic" to Material.ENCHANTED_GOLDEN_APPLE,
                "special" to Material.NETHER_STAR,
            )
    }
}
