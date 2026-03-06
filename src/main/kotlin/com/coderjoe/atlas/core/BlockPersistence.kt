package com.coderjoe.atlas.core

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class BlockPersistence<T : AtlasBlock>(
    private val plugin: JavaPlugin,
    private val fileName: String,
    private val yamlKey: String,
    private val factory: BlockFactory<T>,
    private val serialize: (T, String) -> Map<String, Any>,
    private val restore: (T, Map<String, Any>) -> Unit
) {
    private val dataFile = File(plugin.dataFolder, fileName)

    fun save(registry: BlockRegistry<T>) {
        val config = YamlConfiguration()
        val blocksWithIds = registry.getAllBlocksWithIds()

        plugin.logger.info("Saving ${blocksWithIds.size} blocks to $fileName...")

        val blockDataList = mutableListOf<Map<String, Any>>()

        for ((block, blockId) in blocksWithIds) {
            val map = mutableMapOf<String, Any>(
                "blockId" to blockId,
                "world" to (block.location.world?.name ?: "world"),
                "x" to block.location.blockX,
                "y" to block.location.blockY,
                "z" to block.location.blockZ
            )
            val facing = block.facing
            if (facing != BlockFace.SELF) {
                map["facing"] = facing.name
            }
            map.putAll(serialize(block, blockId))
            blockDataList.add(map)
        }

        config.set(yamlKey, blockDataList)

        try {
            config.save(dataFile)
            plugin.logger.info("Successfully saved ${blockDataList.size} blocks to $fileName")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save blocks to $fileName: ${e.message}")
            e.printStackTrace()
        }
    }

    fun load(registry: BlockRegistry<T>) {
        if (!dataFile.exists()) {
            plugin.logger.info("No $fileName data file found, starting fresh")
            return
        }

        val config = YamlConfiguration.loadConfiguration(dataFile)
        val blockDataList = config.getMapList(yamlKey)

        plugin.logger.info("Loading ${blockDataList.size} blocks from $fileName...")

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
                val facing = if (facingStr != null) {
                    try { BlockFace.valueOf(facingStr) } catch (_: Exception) { BlockFace.SELF }
                } else {
                    BlockFace.SELF
                }

                val block = factory.create(blockId, location, facing)
                if (block != null) {
                    @Suppress("UNCHECKED_CAST")
                    restore(block, blockDataMap as Map<String, Any>)
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

        plugin.logger.info("Loaded $loadedCount blocks from $fileName, $failedCount failed")
    }
}
