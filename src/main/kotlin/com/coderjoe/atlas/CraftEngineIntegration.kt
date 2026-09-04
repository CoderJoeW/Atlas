package com.coderjoe.atlas

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URI
import java.util.jar.JarFile

class CraftEngineIntegration(private val plugin: JavaPlugin) {
    companion object {
        /** Records what Atlas deployed, so a later run can tell its own files from everyone else's. */
        const val MANIFEST_NAME = ".atlas-deployed"
        const val TEXTURES_PATH = "resourcepack/assets/minecraft/textures/block/custom"
        const val MODELS_PATH = "resourcepack/assets/minecraft/models/block/custom"
        const val ITEM_TEXTURES_PATH = "resourcepack/assets/minecraft/textures/item/custom"
        const val ITEM_MODELS_PATH = "resourcepack/assets/minecraft/models/item/custom"
    }

    private val craftEngineFolder: File
        get() = File(plugin.dataFolder.parentFile, "CraftEngine/resources/atlas")

    /**
     * Relative paths written on this run, used to prune what a previous build left behind.
     */
    private val deployed = mutableSetOf<String>()

    fun initialize() {
        copyPackYml()
        copyConfigurations()
        copyAssets(TEXTURES_PATH, ".png")
        // An animated texture is a strip of frames plus a .mcmeta naming the frame rate. Without
        // the .mcmeta the client has no reason to think the file is animated and draws the whole
        // strip squashed onto one face, so it has to ship alongside the png.
        copyAssets(TEXTURES_PATH, ".png.mcmeta")
        copyAssets(MODELS_PATH, ".json")
        copyAssets(ITEM_TEXTURES_PATH, ".png")
        copyAssets(ITEM_TEXTURES_PATH, ".png.mcmeta")
        copyAssets(ITEM_MODELS_PATH, ".json")
        pruneStaleFiles()
        writeManifest()
        plugin.logger.atlasInfo("Atlas CraftEngine integration initialized")
    }

    /**
     * Deletes files a previous build deployed that this one no longer ships.
     *
     * Without this, a retired block's configuration lives on in CraftEngine's resources folder
     * forever, and keeps claiming the vanilla block states its appearances were allocated - which
     * is enough to push a later block over a state group's capacity and refuse to load.
     *
     * Only paths recorded in the manifest are considered. Anything else in the folder was put
     * there by someone else and is left strictly alone.
     */
    private fun pruneStaleFiles() {
        val manifest = File(craftEngineFolder, MANIFEST_NAME)
        if (!manifest.exists()) return

        val previous =
            try {
                manifest.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            } catch (e: Throwable) {
                plugin.logger.warning("Could not read Atlas deployment manifest: ${e.message}")
                return
            }

        for (path in previous.subtract(deployed)) {
            val stale = File(craftEngineFolder, path)
            if (stale.exists() && stale.delete()) {
                plugin.logger.atlasInfo("Removed retired resource $path from CraftEngine")
            }
        }
    }

    private fun writeManifest() {
        try {
            File(craftEngineFolder, MANIFEST_NAME).writeText(deployed.sorted().joinToString("\n"))
        } catch (e: Throwable) {
            plugin.logger.warning("Could not write Atlas deployment manifest: ${e.message}")
        }
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
                deployed.add("configuration/$fileName")
            }
        }
    }

    /**
     * Copies every [suffix] file the jar ships under [assetPath] into CraftEngine's resources.
     *
     * [assetPath] is relative to both the plugin's `atlas/` resource root and the CraftEngine
     * folder, so the same value names the source and the destination.
     */
    private fun copyAssets(
        assetPath: String,
        suffix: String,
    ) {
        val targetFolder = File(craftEngineFolder, assetPath)
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }

        val prefix = "atlas/$assetPath/"

        for (resourcePath in discoverResources(prefix, suffix)) {
            val fileName = resourcePath.substringAfterLast("/")
            val targetFile = File(targetFolder, fileName)
            plugin.saveResource(resourcePath, true)
            val sourceFile = File(plugin.dataFolder, resourcePath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                deployed.add("$assetPath/$fileName")
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
