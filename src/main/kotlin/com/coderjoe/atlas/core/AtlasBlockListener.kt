package com.coderjoe.atlas.core

import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin

class AtlasBlockListener(
    private val plugin: JavaPlugin,
    private val systems: List<BlockSystem<*>>,
) : Listener {
    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val location = event.block.location
        val key = BlockRegistry.locationKey(location)

        if (systems.any { it.registry.updatingLocations.contains(key) }) return

        val blockId = CraftEngineHelper.getBlockId(event.block) ?: return

        for (system in systems) {
            val descriptor = system.findDescriptorForBlockId(blockId)
            if (descriptor != null) {
                handlePlacement(event, system, descriptor)
                return
            }
        }
    }

    private fun handlePlacement(
        event: BlockPlaceEvent,
        system: BlockSystem<*>,
        descriptor: BlockDescriptor,
    ) {
        val location = event.block.location.clone()

        when (descriptor.placementType) {
            PlacementType.SIMPLE -> {
                val facing = getPlayerFacing(event)
                createAndRegister(system, descriptor.baseBlockId, location, facing)
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
                        createAndRegister(system, descriptor.baseBlockId, location, actualFacing)
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
                        createAndRegister(system, descriptor.baseBlockId, location, actualFacing)
                    },
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createAndRegister(
        system: BlockSystem<*>,
        blockId: String,
        location: org.bukkit.Location,
        facing: BlockFace,
    ) {
        val factory = system.factory as BlockFactory<AtlasBlock>
        val registry = system.registry as BlockRegistry<AtlasBlock>
        val block = factory.create(blockId, location, facing)
        if (block != null) {
            registry.register(block, blockId)
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val location = event.block.location
        val key = BlockRegistry.locationKey(location)

        if (systems.any { it.registry.updatingLocations.contains(key) }) return

        for (system in systems) {
            val block = system.registry.unregister(location)
            if (block != null) {
                return
            }
        }
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

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.player.isSneaking) return
        val clickedBlock = event.clickedBlock ?: return
        val location = clickedBlock.location

        for (system in systems) {
            val block = system.registry.getBlock(location)
            if (block != null) {
                system.showDialog(event.player, block)
                event.isCancelled = true
                return
            }
        }
    }
}
