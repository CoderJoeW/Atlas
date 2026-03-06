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
            "small_battery_bottom",
            "fluid_pump_top",
            "fluid_pump_side",
            "fluid_pump_bottom",
            "fluid_pump_top_active",
            "fluid_pump_side_active",
            "fluid_pump_bottom_active",
            "fluid_pipe_front",
            "fluid_pipe_back",
            "fluid_pipe_side_up",
            "fluid_pipe_side_down",
            "fluid_pipe_side_left",
            "fluid_pipe_side_right",
            "fluid_pipe_front_filled",
            "fluid_pipe_back_filled",
            "fluid_pipe_side_filled_up",
            "fluid_pipe_side_filled_down",
            "fluid_pipe_side_filled_left",
            "fluid_pipe_side_filled_right",
            "fluid_pump_top_active_lava",
            "fluid_pump_side_active_lava",
            "fluid_pump_bottom_active_lava",
            "fluid_pipe_front_filled_lava",
            "fluid_pipe_back_filled_lava",
            "fluid_pipe_side_filled_lava_up",
            "fluid_pipe_side_filled_lava_down",
            "fluid_pipe_side_filled_lava_left",
            "fluid_pipe_side_filled_lava_right",
            "fluid_container_front",
            "fluid_container_back",
            "fluid_container_side",
            "fluid_container_top",
            "fluid_container_front_water_low",
            "fluid_container_back_water_low",
            "fluid_container_side_water_low",
            "fluid_container_top_water_low",
            "fluid_container_front_water_medium",
            "fluid_container_back_water_medium",
            "fluid_container_side_water_medium",
            "fluid_container_top_water_medium",
            "fluid_container_front_water_full",
            "fluid_container_back_water_full",
            "fluid_container_side_water_full",
            "fluid_container_top_water_full",
            "fluid_container_front_lava_low",
            "fluid_container_back_lava_low",
            "fluid_container_side_lava_low",
            "fluid_container_top_lava_low",
            "fluid_container_front_lava_medium",
            "fluid_container_back_lava_medium",
            "fluid_container_side_lava_medium",
            "fluid_container_top_lava_medium",
            "fluid_container_front_lava_full",
            "fluid_container_back_lava_full",
            "fluid_container_side_lava_full",
            "fluid_container_top_lava_full"
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
