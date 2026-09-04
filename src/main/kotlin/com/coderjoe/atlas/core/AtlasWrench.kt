package com.coderjoe.atlas.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

/**
 * The tool that opens Atlas block dialogs.
 *
 * Without it, right-clicking a machine does nothing special, so Atlas blocks behave like ordinary
 * blocks and never interrupt building. Inspecting a block is then a deliberate act: hold the
 * wrench and right-click. Sneak-right-clicking a power block reads its network instead.
 */
object AtlasWrench {
    const val ITEM_NAME = "Atlas Wrench"
    private const val TAG = "atlas_wrench"

    private fun key(plugin: JavaPlugin) = NamespacedKey(plugin, TAG)

    fun create(plugin: JavaPlugin): ItemStack {
        val item = ItemStack(Material.BRUSH)
        val meta = item.itemMeta
        meta.displayName(
            Component.text(ITEM_NAME)
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false),
        )
        meta.lore(
            listOf(
                lore("Right-click an Atlas block to inspect it"),
                lore("Sneak + right-click a power block to read its network"),
            ),
        )
        meta.persistentDataContainer.set(key(plugin), PersistentDataType.BYTE, 1)
        item.itemMeta = meta
        return item
    }

    private fun lore(text: String): Component =
        Component.text(text)
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)

    fun isWrench(
        item: ItemStack?,
        plugin: JavaPlugin,
    ): Boolean {
        val meta = item?.itemMeta ?: return false
        return meta.persistentDataContainer.has(key(plugin), PersistentDataType.BYTE)
    }

    fun createRecipe(plugin: JavaPlugin): ShapelessRecipe {
        val recipe = ShapelessRecipe(key(plugin), create(plugin))
        recipe.addIngredient(Material.IRON_INGOT)
        recipe.addIngredient(Material.COPPER_INGOT)
        recipe.addIngredient(Material.REDSTONE)
        return recipe
    }
}
