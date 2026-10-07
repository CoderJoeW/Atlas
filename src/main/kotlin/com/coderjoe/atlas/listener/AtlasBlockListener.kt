package com.coderjoe.atlas.listener

import com.coderjoe.atlas.block.BlockCatalog
import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.PlacementType
import com.coderjoe.atlas.craftengine.CraftEngineHelper
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.java.JavaPlugin

class AtlasBlockListener(
    private val plugin: JavaPlugin,
    private val registry: BlockRegistry,
    private val catalog: BlockCatalog,
) : Listener {
    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val location = event.block.location
        val key = BlockRegistry.locationKey(location)

        if (registry.updatingLocations.contains(key)) return

        val blockId = CraftEngineHelper.getBlockId(event.block) ?: return
        val descriptor = catalog.find(blockId) ?: return
        handlePlacement(event, descriptor)
    }

    private fun handlePlacement(
        event: BlockPlaceEvent,
        descriptor: BlockDescriptor,
    ) {
        val location = event.block.location.clone()

        when (descriptor.placementType) {
            PlacementType.SIMPLE -> {
                val facing = getPlayerFacing(event)
                createAndRegister(descriptor.baseBlockId, location, facing)
            }
            PlacementType.DIRECTIONAL -> {
                val facing = getPlayerFacing(event)
                val playerFacing = event.player.facing
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        val actualFacing =
                            if (CraftEngineHelper.setFacing(location, facing)) {
                                facing
                            } else {
                                CraftEngineHelper.setFacing(location, playerFacing)
                                playerFacing
                            }
                        createAndRegister(descriptor.baseBlockId, location, actualFacing)
                    },
                )
            }
            PlacementType.DIRECTIONAL_OPPOSITE -> {
                val facing = getPlayerFacing(event).oppositeFace
                val playerFacing = event.player.facing.oppositeFace
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        val actualFacing =
                            if (CraftEngineHelper.setFacing(location, facing)) {
                                facing
                            } else {
                                CraftEngineHelper.setFacing(location, playerFacing)
                                playerFacing
                            }
                        createAndRegister(descriptor.baseBlockId, location, actualFacing)
                    },
                )
            }
        }
    }

    private fun createAndRegister(
        blockId: String,
        location: Location,
        facing: BlockFace,
    ) {
        val block = catalog.create(blockId, location, facing)
        if (block != null) {
            registry.register(block, blockId)
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val location = event.block.location
        val key = BlockRegistry.locationKey(location)

        if (registry.updatingLocations.contains(key)) return

        registry.unregister(location)
    }

    companion object {
        fun getPlayerFacing(event: BlockPlaceEvent): BlockFace {
            val against = event.blockAgainst.location
            val placed = event.block.location
            val dx = placed.blockX - against.blockX
            val dy = placed.blockY - against.blockY
            val dz = placed.blockZ - against.blockZ

            return when {
                dy > 0 -> BlockFace.UP
                dy < 0 -> BlockFace.DOWN
                dx > 0 -> BlockFace.EAST
                dx < 0 -> BlockFace.WEST
                dz > 0 -> BlockFace.SOUTH
                dz < 0 -> BlockFace.NORTH
                else -> event.player.facing
            }
        }
    }
}
