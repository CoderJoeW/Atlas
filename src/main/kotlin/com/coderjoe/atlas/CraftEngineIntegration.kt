package com.coderjoe.atlas

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URI
import java.util.jar.JarFile

class CraftEngineIntegration(private val plugin: JavaPlugin) {
    private val craftEngineFolder: File
        get() = File(plugin.dataFolder.parentFile, "CraftEngine/resources/atlas")

    fun initialize() {
        copyPackYml()
        copyConfigurations()
        copyTextures()
        copyModels()
        plugin.logger.atlasInfo("Atlas CraftEngine integration initialized")
    }

    private fun copyPackYml() {
        val targetFile = File(craftEngineFolder, "pack.yml")
        if (!targetFile.parentFile.exists()) {
            targetFile.parentFile.mkdirs()
        }
        plugin.saveResource("atlas/pack.yml", true)
        val sourceFile = File(plugin.dataFolder, "atlas/pack.yml")
        if (sourceFile.exists()) {
            sourceFile.copyTo(targetFile, overwrite = true)
            sourceFile.delete()
        }
    }

    private fun copyConfigurations() {
        val configFolder = File(craftEngineFolder, "configuration")
        if (!configFolder.exists()) {
            configFolder.mkdirs()
        }

        val prefix = "atlas/configuration/"
        val configPaths = discoverResources(prefix, ".yml")

        for (resourcePath in configPaths) {
            val fileName = resourcePath.substringAfterLast("/")
            val targetFile = File(configFolder, fileName)
            plugin.saveResource(resourcePath, true)
            val sourceFile = File(plugin.dataFolder, resourcePath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                plugin.logger.atlasInfo("Copied ${fileName.removeSuffix(".yml")} configuration to CraftEngine")
            }
        }
    }

    private fun copyTextures() {
        val texturesFolder = File(craftEngineFolder, "resourcepack/assets/minecraft/textures/block/custom")
        if (!texturesFolder.exists()) {
            texturesFolder.mkdirs()
        }

        val prefix = "atlas/resourcepack/assets/minecraft/textures/block/custom/"
        val texturePaths = discoverResources(prefix, ".png")

        for (resourcePath in texturePaths) {
            val fileName = resourcePath.substringAfterLast("/")
            val targetFile = File(texturesFolder, fileName)
            plugin.saveResource(resourcePath, true)
            val sourceFile = File(plugin.dataFolder, resourcePath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                plugin.logger.atlasInfo("Copied ${fileName.removeSuffix(".png")} texture to CraftEngine")
            }
        }
    }

    private fun copyModels() {
        val modelsFolder = File(craftEngineFolder, "resourcepack/assets/minecraft/models/block/custom")
        if (!modelsFolder.exists()) {
            modelsFolder.mkdirs()
        }

        val prefix = "atlas/resourcepack/assets/minecraft/models/block/custom/"
        val modelPaths = discoverResources(prefix, ".json")

        for (resourcePath in modelPaths) {
            val fileName = resourcePath.substringAfterLast("/")
            val targetFile = File(modelsFolder, fileName)
            plugin.saveResource(resourcePath, true)
            val sourceFile = File(plugin.dataFolder, resourcePath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                plugin.logger.atlasInfo("Copied ${fileName.removeSuffix(".json")} model to CraftEngine")
            }
        }
    }

    private fun discoverResources(
        prefix: String,
        suffix: String,
    ): List<String> {
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
}
