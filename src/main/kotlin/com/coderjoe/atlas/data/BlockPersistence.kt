package com.coderjoe.atlas.data

import com.coderjoe.atlas.block.BlockCatalog
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.util.atlasInfo
import org.bukkit.Location
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

    private val dataFile = File(plugin.dataFolder, FILE_NAME)

    /** Writes every block in [registry] to [FILE_NAME], returning whether the file was written. */
    fun save(registry: BlockRegistry): Boolean {
        val blocksWithIds = registry.getAllBlocksWithIds()
        plugin.logger.atlasInfo("Saving ${blocksWithIds.size} blocks to $FILE_NAME...")

        val blockDataList =
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
        var failedCount = 0

        for (blockDataMap in blockDataList) {
            try {
                val blockId = blockDataMap["blockId"] as? String ?: continue
                val worldName = blockDataMap["world"] as? String ?: continue
                val x = (blockDataMap["x"] as? Number)?.toInt() ?: continue
                val y = (blockDataMap["y"] as? Number)?.toInt() ?: continue
                val z = (blockDataMap["z"] as? Number)?.toInt() ?: continue
                val facingStr = blockDataMap["facing"] as? String

                val world = plugin.server.getWorld(worldName)
                if (world == null) {
                    plugin.logger.warning("Failed to load block at $worldName $x,$y,$z - world not found")
                    failedCount++
                    continue
                }

                val location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                val facing = facingStr?.let { runCatching { BlockFace.valueOf(it) }.getOrNull() } ?: BlockFace.SELF

                val block = catalog.create(blockId, location, facing)
                if (block != null) {
                    @Suppress("UNCHECKED_CAST")
                    block.readSaveData(blockDataMap as Map<String, Any>)
                    registry.register(block, blockId)
                    loadedCount++
                } else {
                    plugin.logger.warning("Failed to create block for ID: $blockId at $x,$y,$z")
                    failedCount++
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load block: ${e.message}")
                failedCount++
            }
        }

        plugin.logger.atlasInfo("Loaded $loadedCount blocks from $source, $failedCount failed")
    }
}
