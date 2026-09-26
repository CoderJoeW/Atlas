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
        deploy("atlas/pack.yml", "pack.yml")
    }

    private fun copyConfigurations() {
        val configPaths = discoverResources("atlas/configuration/", ".yml")

        requireUniqueFileNames(configPaths)

        for (resourcePath in configPaths) {
            val relativePath = "configuration/${resourcePath.substringAfterLast("/")}"
            if (deploy(resourcePath, relativePath)) deployed.add(relativePath)
        }
    }

    /**
     * Copies every file the jar ships under [RESOURCE_PACK_PATH] into CraftEngine's resources,
     * keeping each file's path relative to Atlas's `atlas/` resource root.
     */
    private fun copyResourcePack() {
        for (resourcePath in discoverResources("atlas/$RESOURCE_PACK_PATH/", "")) {
            val relativePath = resourcePath.removePrefix("atlas/")
            if (deploy(resourcePath, relativePath)) deployed.add(relativePath)
        }
    }

    /**
     * Streams the jar resource [resourcePath] to [relativePath] under CraftEngine's folder, and
     * reports whether the jar had it.
     */
    private fun deploy(
        resourcePath: String,
        relativePath: String,
    ): Boolean {
        val source = plugin.getResource(resourcePath) ?: return false
        val target = File(craftEngineFolder, relativePath)
        target.parentFile.mkdirs()
        source.use { input -> target.outputStream().use { input.copyTo(it) } }
        return true
    }
}
