package com.coderjoe.atlas.item

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Equippable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

/**
 * The headgear that shows Atlas machines' readouts.
 *
 * Worn in the helmet slot, it floats a holographic panel beside whichever Atlas block the wearer
 * is looking at. Nothing needs clicking, so a whole factory can be read by walking through it,
 * and taking the goggles off - or wearing real armour instead - clears the view entirely.
 *
 * The icon is borrowed from the spyglass until the goggles have art of their own.
 */
object AtlasGoggles {
    const val ITEM_NAME = "Atlas Goggles"
    private const val TAG = "atlas_goggles"
    private val PLACEHOLDER_MODEL = Key.key("minecraft", "spyglass")

    private fun key(plugin: JavaPlugin) = NamespacedKey(plugin, TAG)

    fun create(plugin: JavaPlugin): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta
        meta.displayName(
            Component.text(ITEM_NAME)
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false),
        )
        meta.lore(listOf(lore("Wear to see a readout of the Atlas block you look at")))
        meta.persistentDataContainer.set(key(plugin), PersistentDataType.BYTE, 1)
        item.itemMeta = meta

        item.setData(DataComponentTypes.EQUIPPABLE, Equippable.equippable(EquipmentSlot.HEAD).build())
        item.setData(DataComponentTypes.ITEM_MODEL, PLACEHOLDER_MODEL)
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1)
        return item
    }

    private fun lore(text: String): Component =
        Component.text(text)
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)

    fun isGoggles(
        item: ItemStack?,
        plugin: JavaPlugin,
    ): Boolean {
        val meta = item?.itemMeta ?: return false
        return meta.persistentDataContainer.has(key(plugin), PersistentDataType.BYTE)
    }

    /** Only the helmet slot counts: goggles held in a hand or carried in the inventory show nothing. */
    fun isWearing(
        player: Player,
        plugin: JavaPlugin,
    ): Boolean = isGoggles(player.inventory.helmet, plugin)

    /** Copper frame, redstone bridge, glass lenses: all within reach before the first machine. */
    fun createRecipe(plugin: JavaPlugin): ShapedRecipe {
        val recipe = ShapedRecipe(key(plugin), create(plugin))
        recipe.shape("CRC", "G G")
        recipe.setIngredient('C', Material.COPPER_INGOT)
        recipe.setIngredient('R', Material.REDSTONE)
        recipe.setIngredient('G', Material.GLASS_PANE)
        return recipe
    }
}
