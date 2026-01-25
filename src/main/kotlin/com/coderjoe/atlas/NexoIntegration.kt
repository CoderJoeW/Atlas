package com.coderjoe.atlas

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class NexoIntegration(private val plugin: JavaPlugin) {

    companion object {
        const val TEST_BLOCK_ID = "test_block"
    }

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
        if (!targetFile.exists()) {
            plugin.saveResource("nexo/items/atlas_blocks.yml", false)
            // Move from our data folder to Nexo's folder
            val sourceFile = File(plugin.dataFolder, "nexo/items/atlas_blocks.yml")
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                plugin.logger.info("Copied Atlas block configurations to Nexo")
            }
        }
    }

    private fun copyTextures() {
        val texturesFolder = File(nexoFolder, "pack/assets/atlas/textures/block")
        if (!texturesFolder.exists()) {
            texturesFolder.mkdirs()
        }

        val targetFile = File(texturesFolder, "test_block.png")
        if (!targetFile.exists()) {
            plugin.saveResource("nexo/pack/assets/atlas/textures/block/test_block.png", false)
            val sourceFile = File(plugin.dataFolder, "nexo/pack/assets/atlas/textures/block/test_block.png")
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                plugin.logger.info("Copied Atlas textures to Nexo")
            }
        }
    }

    private fun copyRecipes() {
        val recipesFolder = File(nexoFolder, "recipes/shapeless")
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs()
        }

        val targetFile = File(recipesFolder, "atlas_recipes.yml")
        if (!targetFile.exists()) {
            plugin.saveResource("nexo/recipes/shapeless/atlas_recipes.yml", false)
            val sourceFile = File(plugin.dataFolder, "nexo/recipes/shapeless/atlas_recipes.yml")
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                plugin.logger.info("Copied Atlas recipes to Nexo")
            }
        }
    }

    /**
     * Check if a block at the given location is a TestBlock
     */
    fun isTestBlock(block: Block): Boolean {
        val mechanic = NexoBlocks.customBlockMechanic(block.location) ?: return false
        return mechanic.itemID == TEST_BLOCK_ID
    }

    /**
     * Place a TestBlock at the specified location
     */
    fun placeTestBlock(location: Location): Boolean {
        if (!NexoItems.exists(TEST_BLOCK_ID)) {
            plugin.logger.warning("TestBlock is not registered in Nexo")
            return false
        }
        NexoBlocks.place(TEST_BLOCK_ID, location)
        return true
    }

    /**
     * Check if Nexo has loaded the TestBlock
     */
    fun isTestBlockRegistered(): Boolean {
        return NexoItems.exists(TEST_BLOCK_ID)
    }
}
