package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockCatalog
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.util.atlasInfo
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Every Atlas block, whatever its family, in one [FILE_NAME] stamped with a schema [VERSION].
 *
 * Servers that ran Atlas before the families shared a file have one file per family instead. The
 * first load with no [FILE_NAME] reads those, writes their blocks to [FILE_NAME], and only then
 * renames each to `<name>.migrated`, so a failed write leaves them in place to try again next start.
 */
class BlockPersistence(
    private val plugin: JavaPlugin,
    private val catalog: BlockCatalog,
) {
    companion object {
        const val FILE_NAME = "blocks.yml"
        const val VERSION = 1
        const val MIGRATED_SUFFIX = ".migrated"

        /** The per-family save files, by file name, with the key each kept its block list under. */
        val LEGACY_FILES =
            mapOf(
                "power_blocks.yml" to "power_blocks",
                "fluid_blocks.yml" to "fluid_blocks",
                "transport_blocks.yml" to "transport_blocks",
            )

        private const val VERSION_KEY = "version"
        private const val BLOCKS_KEY = "blocks"
    }

    private data class Spot(val world: String, val x: Int, val y: Int, val z: Int) {
        override fun toString() = "$world $x,$y,$z"
    }

    private val dataFile = File(plugin.dataFolder, FILE_NAME)

    /**
     * Saved entries that could not become blocks yet, because their world is not loaded or the catalog
     * does not know their id. [save] writes them back unchanged so they are never lost.
     */
    private val held = LinkedHashMap<Spot, Map<*, *>>()

    /**
     * Writes every block in [registry] to [FILE_NAME], along with the held entries no live block has
     * replaced, returning whether the file was written.
     */
    fun save(registry: BlockRegistry): Boolean {
        val blocksWithIds = registry.getAllBlocksWithIds()
        val liveDataList =
            blocksWithIds.map { (block, blockId) ->
                val map =
                    mutableMapOf<String, Any>(
                        "blockId" to blockId,
                        "world" to (block.location.world?.name ?: "world"),
                        "x" to block.location.blockX,
                        "y" to block.location.blockY,
                        "z" to block.location.blockZ,
                    )
                val facing = block.facing
                if (facing != BlockFace.SELF) {
                    map["facing"] = facing.name
                }
                block.writeSaveData(map)
                map
            }
        held.keys.removeAll(liveDataList.mapNotNull(::spotOf).toSet())
        val blockDataList = liveDataList + held.values
        plugin.logger.atlasInfo("Saving ${liveDataList.size} blocks and ${held.size} held entries to $FILE_NAME...")

        val config = YamlConfiguration()
        config.set(VERSION_KEY, VERSION)
        config.set(BLOCKS_KEY, blockDataList)

        val partial = File(plugin.dataFolder, "$FILE_NAME.tmp")
        return try {
            config.save(partial)
            replace(partial, dataFile)
            plugin.logger.atlasInfo("Successfully saved ${blockDataList.size} blocks to $FILE_NAME")
            true
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save blocks to $FILE_NAME: ${e.message}")
            e.printStackTrace()
            partial.delete()
            false
        }
    }

    private fun replace(
        source: File,
        target: File,
    ) {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun load(registry: BlockRegistry) {
        loadFiles(registry)
        logHeld()
    }

    /** Restores the held entries in [world], now that it has loaded, into [registry]. */
    fun restoreWorld(
        registry: BlockRegistry,
        world: World,
    ) {
        val pending = held.filterKeys { it.world == world.name }
        if (pending.isEmpty()) return

        var restoredCount = 0
        for ((spot, entry) in pending) {
            if (restore(registry, entry, spot, world)) {
                held.remove(spot)
                restoredCount++
            }
        }
        plugin.logger.atlasInfo(
            "World '${world.name}' loaded: restored $restoredCount of ${pending.size} held blocks, ${held.size} still held",
        )
    }

    private fun logHeld() {
        for ((worldName, spots) in held.keys.groupBy { it.world }) {
            if (plugin.server.getWorld(worldName) == null) {
                plugin.logger.warning(
                    "Holding ${spots.size} blocks in world '$worldName', which is not loaded; they will be restored when it loads",
                )
            } else {
                plugin.logger.warning(
                    "Holding ${spots.size} blocks in world '$worldName' that could not be created; they stay in $FILE_NAME unchanged",
                )
            }
        }
    }

    private fun loadFiles(registry: BlockRegistry) {
        val legacyFiles = LEGACY_FILES.keys.map { File(plugin.dataFolder, it) }.filter { it.isFile }

        if (dataFile.isFile) {
            val config = YamlConfiguration.loadConfiguration(dataFile)
            val version = config.getInt(VERSION_KEY, VERSION)
            if (version > VERSION) {
                plugin.logger.warning("$FILE_NAME is schema version $version, newer than $VERSION; loading what this version understands")
            }
            loadEntries(registry, config.getMapList(BLOCKS_KEY), FILE_NAME)
            if (legacyFiles.isNotEmpty()) {
                plugin.logger.warning(
                    "Setting aside ${legacyFiles.map { it.name }} without loading them: $FILE_NAME already holds the blocks",
                )
                legacyFiles.forEach(::retire)
            }
            return
        }

        if (legacyFiles.isEmpty()) {
            plugin.logger.atlasInfo("No $FILE_NAME data file found, starting fresh")
            return
        }

        migrate(registry, legacyFiles)
    }

    private fun migrate(
        registry: BlockRegistry,
        legacyFiles: List<File>,
    ) {
        plugin.logger.atlasInfo("Migrating ${legacyFiles.map { it.name }} to $FILE_NAME...")
        for (file in legacyFiles) {
            val config = YamlConfiguration.loadConfiguration(file)
            loadEntries(registry, config.getMapList(LEGACY_FILES.getValue(file.name)), file.name)
        }

        if (save(registry)) {
            legacyFiles.forEach(::retire)
        } else {
            plugin.logger.severe("Keeping ${legacyFiles.map { it.name }} in place; migration will be retried on the next start")
        }
    }

    private fun retire(file: File) {
        var target = File(file.parentFile, file.name + MIGRATED_SUFFIX)
        if (target.exists()) {
            target = File(file.parentFile, "${file.name}$MIGRATED_SUFFIX-${System.currentTimeMillis()}")
        }
        if (file.renameTo(target)) {
            plugin.logger.atlasInfo("Renamed ${file.name} to ${target.name}")
        } else {
            plugin.logger.warning("Could not rename ${file.name} to ${target.name}")
        }
    }

    private fun loadEntries(
        registry: BlockRegistry,
        blockDataList: List<Map<*, *>>,
        source: String,
    ) {
        plugin.logger.atlasInfo("Loading ${blockDataList.size} blocks from $source...")

        var loadedCount = 0
        var heldCount = 0
        var skippedCount = 0

        for (entry in blockDataList) {
            val spot = spotOf(entry)
            if (spot == null || entry["blockId"] !is String) {
                skippedCount++
                continue
            }
            val world = plugin.server.getWorld(spot.world)
            if (world != null && restore(registry, entry, spot, world)) {
                loadedCount++
            } else {
                held[spot] = entry
                heldCount++
            }
        }

        plugin.logger.atlasInfo("Loaded $loadedCount blocks from $source, holding $heldCount, skipped $skippedCount malformed")
    }

    private fun restore(
        registry: BlockRegistry,
        entry: Map<*, *>,
        spot: Spot,
        world: World,
    ): Boolean {
        val blockId = entry["blockId"] as String
        return try {
            val location = Location(world, spot.x.toDouble(), spot.y.toDouble(), spot.z.toDouble())
            val facing = (entry["facing"] as? String)?.let { runCatching { BlockFace.valueOf(it) }.getOrNull() } ?: BlockFace.SELF
            val block = catalog.create(blockId, location, facing)
            if (block == null) {
                plugin.logger.warning("Unknown block ID $blockId at $spot; keeping its saved entry")
                return false
            }
            @Suppress("UNCHECKED_CAST")
            block.readSaveData(entry as Map<String, Any>)
            registry.register(block, blockId)
            true
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load $blockId at $spot, keeping its saved entry: ${e.message}")
            false
        }
    }

    private fun spotOf(entry: Map<*, *>): Spot? {
        val world = entry["world"] as? String ?: return null
        val x = (entry["x"] as? Number)?.toInt() ?: return null
        val y = (entry["y"] as? Number)?.toInt() ?: return null
        val z = (entry["z"] as? Number)?.toInt() ?: return null
        return Spot(world, x, y, z)
    }
}
