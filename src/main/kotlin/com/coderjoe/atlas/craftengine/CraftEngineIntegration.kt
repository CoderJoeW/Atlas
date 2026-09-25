package com.coderjoe.atlas.craftengine

import com.coderjoe.atlas.util.atlasInfo
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URI
import java.util.jar.JarFile

class CraftEngineIntegration(private val plugin: JavaPlugin) {
    companion object {
        /** Records what Atlas deployed, so a later run can tell its own files from everyone else's. */
        const val MANIFEST_NAME = ".atlas-deployed"

        /**
         * The resource pack, deployed whole with its folder layout intact, so a file added anywhere
         * in it reaches players without touching this class.
         */
        const val RESOURCE_PACK_PATH = "resourcepack"

        /** Finder litter that a macOS checkout can leave in the resource folders. */
        private const val FINDER_METADATA = ".DS_Store"

        /**
         * Fails if two resources in different folders share a file name.
         *
         * Deploying keeps only the file name, so the second copy would overwrite the first and one
         * block's config would silently disappear from CraftEngine.
         */
        internal fun requireUniqueFileNames(resourcePaths: List<String>) {
            val clashes = resourcePaths.groupBy { it.substringAfterLast("/") }.filterValues { it.size > 1 }
            check(clashes.isEmpty()) { "Config file names must be unique across folders: $clashes" }
        }

        /**
         * Lists every resource under [prefix] ending in [suffix], at any depth, as paths relative to
         * the jar root.
         */
        internal fun discoverResources(
            prefix: String,
            suffix: String,
        ): List<String> {
            val url = CraftEngineIntegration::class.java.classLoader.getResource(prefix) ?: return emptyList()

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
                    val root = File(url.toURI())
                    root.walkTopDown()
                        .filter { it.isFile && it.name.endsWith(suffix) }
                        .map { prefix + it.relativeTo(root).invariantSeparatorsPath }
                        .toList()
                }
                else -> emptyList()
            }
        }
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
        copyResourcePack()
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

        requireUniqueFileNames(configPaths)

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
     * Copies every file the jar ships under [RESOURCE_PACK_PATH] into CraftEngine's resources,
     * keeping each file's path relative to Atlas's `atlas/` resource root.
     */
    private fun copyResourcePack() {
        for (resourcePath in discoverResources("atlas/$RESOURCE_PACK_PATH/", "")) {
            if (resourcePath.substringAfterLast("/") == FINDER_METADATA) continue
            val relativePath = resourcePath.removePrefix("atlas/")
            val targetFile = File(craftEngineFolder, relativePath)
            targetFile.parentFile.mkdirs()
            plugin.saveResource(resourcePath, true)
            val sourceFile = File(plugin.dataFolder, resourcePath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                sourceFile.delete()
                deployed.add(relativePath)
            }
        }
    }
}
