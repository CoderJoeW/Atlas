package com.coderjoe.atlas

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URI
import java.util.jar.JarFile

class NexoIntegration(private val plugin: JavaPlugin) {
    private val nexoFolder: File
        get() = File(plugin.dataFolder.parentFile, "Nexo")

    fun initialize() {
        copyItemConfigurations()
        copyTextures()
        copyModels()
        copyRecipes()
        plugin.logger.atlasInfo("Atlas Nexo integration initialized")
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
            plugin.logger.atlasInfo("Copied Atlas block configurations to Nexo")
        }
    }

    private fun copyTextures() {
        val texturesFolder = File(nexoFolder, "pack/assets/atlas/textures/block")
        if (!texturesFolder.exists()) {
            texturesFolder.mkdirs()
        }

        val prefix = "nexo/pack/assets/atlas/textures/block/"
        val texturePaths = discoverResources(prefix, ".png")

        for (resourcePath in texturePaths) {
            val fileName = resourcePath.substringAfterLast("/")
            val textureFile = File(texturesFolder, fileName)
            plugin.saveResource(resourcePath, true)
            val sourceFile = File(plugin.dataFolder, resourcePath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(textureFile, overwrite = true)
                sourceFile.delete()
                plugin.logger.atlasInfo("Copied ${fileName.removeSuffix(".png")} texture to Nexo")
            }
        }
    }

    private fun discoverResources(prefix: String, suffix: String): List<String> {
        val url = javaClass.classLoader.getResource(prefix) ?: return emptyList()

        return when (url.protocol) {
            "jar" -> {
                val jarPath = url.toURI().schemeSpecificPart.substringBefore("!")
                JarFile(File(URI(jarPath))).use { jar ->
                    jar.entries().asSequence()
                        .filter { it.name.startsWith(prefix) && it.name.endsWith(suffix) && !it.isDirectory }
                        .map { it.name }
                        .toList()
                }
            }
            "file" -> {
                File(url.toURI()).listFiles()
                    ?.filter { it.name.endsWith(suffix) }
                    ?.map { prefix + it.name }
                    ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun copyModels() {
        val modelsFolder = File(nexoFolder, "pack/assets/atlas/models/block")
        if (!modelsFolder.exists()) {
            modelsFolder.mkdirs()
        }

        val prefix = "nexo/pack/assets/atlas/models/block/"
        val modelPaths = discoverResources(prefix, ".json")

        for (resourcePath in modelPaths) {
            val fileName = resourcePath.substringAfterLast("/")
            val modelFile = File(modelsFolder, fileName)
            plugin.saveResource(resourcePath, true)
            val sourceFile = File(plugin.dataFolder, resourcePath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(modelFile, overwrite = true)
                sourceFile.delete()
                plugin.logger.atlasInfo("Copied ${fileName.removeSuffix(".json")} model to Nexo")
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
            plugin.logger.atlasInfo("Copied Atlas recipes to Nexo")
        }
    }
}
