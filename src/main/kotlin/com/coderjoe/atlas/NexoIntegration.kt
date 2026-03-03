package com.coderjoe.atlas

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class NexoIntegration(private val plugin: JavaPlugin) {
    private val nexoFolder: File
        get() = File(plugin.dataFolder.parentFile, "Nexo")

    fun initialize() {
        copyItemConfigurations()
        copyTextures()
        copyRecipes()
        plugin.logger.info("Atlas Nexo integration initialized")
    }

    private fun copyItemConfigurations() {
        val itemsFolder = File(nexoFolder, "items")
        if (!itemsFolder.exists()) {
            plugin.logger.warning("Nexo items folder not found. Make sure Nexo is installed.")
            return
        }

        val targetFile = File(itemsFolder, "atlas_blocks.yml")
        plugin.saveResource("nexo/items/atlas_blocks.yml", true)
        val sourceFile = File(plugin.dataFolder, "nexo/items/atlas_blocks.yml")
        if (sourceFile.exists()) {
            sourceFile.copyTo(targetFile, overwrite = true)
            sourceFile.delete()
            plugin.logger.info("Copied Atlas block configurations to Nexo")
        }
    }

    private fun copyTextures() {
        val texturesFolder = File(nexoFolder, "pack/assets/atlas/textures/block")
        if (!texturesFolder.exists()) {
            texturesFolder.mkdirs()
        }

        // Copy block textures
        val textures = listOf(
            "small_solar_panel",
            "small_solar_panel_full",
            "small_solar_panel_side",
            "small_solar_panel_bottom",
            "power_cable_front_powered",
            "power_cable_back_powered",
            "power_cable_side_up_powered",
            "power_cable_side_down_powered",
            "power_cable_side_left_powered",
            "power_cable_side_right_powered",
            "power_cable_cap_up_powered",
            "power_cable_cap_down_powered",
            "power_cable_cap_left_powered",
            "power_cable_cap_right_powered",
            "small_drill",
            "small_drill_front",
            "small_drill_arrow_up",
            "small_drill_arrow_down",
            "small_drill_arrow_left",
            "small_drill_arrow_right",
            "small_battery",
            "small_battery_low",
            "small_battery_medium",
            "small_battery_full",
            "small_battery_side",
            "small_battery_bottom"
        )
        for (textureName in textures) {
            val textureFile = File(texturesFolder, "$textureName.png")
            plugin.saveResource("nexo/pack/assets/atlas/textures/block/$textureName.png", true)
            val sourceFile = File(plugin.dataFolder, "nexo/pack/assets/atlas/textures/block/$textureName.png")
            if (sourceFile.exists()) {
                sourceFile.copyTo(textureFile, overwrite = true)
                sourceFile.delete()
                plugin.logger.info("Copied $textureName texture to Nexo")
            }
        }
    }

    private fun copyRecipes() {
        val recipesFolder = File(nexoFolder, "recipes/shapeless")
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs()
        }

        val targetFile = File(recipesFolder, "atlas_recipes.yml")
        plugin.saveResource("nexo/recipes/shapeless/atlas_recipes.yml", true)
        val sourceFile = File(plugin.dataFolder, "nexo/recipes/shapeless/atlas_recipes.yml")
        if (sourceFile.exists()) {
            sourceFile.copyTo(targetFile, overwrite = true)
            sourceFile.delete()
            plugin.logger.info("Copied Atlas recipes to Nexo")
        }
    }
}
