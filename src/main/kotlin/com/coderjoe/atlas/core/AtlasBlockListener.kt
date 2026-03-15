package com.coderjoe.atlas.core

import com.nexomc.nexo.api.NexoBlocks
import com.nexomc.nexo.api.NexoItems
import org.bukkit.Material
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

        val mechanic = NexoBlocks.customBlockMechanic(event.block) ?: return
        val blockId = mechanic.itemID

        for (system in systems) {
            val baseDescriptor = system.findDescriptorByBaseId(blockId)
            if (baseDescriptor != null) {
                handlePlacement(event, system, baseDescriptor, blockId)
                return
            }

            val descriptor = system.findDescriptorForBlockId(blockId)
            if (descriptor != null) {
                val facing = resolveFacingFromVariant(descriptor, blockId)
                createAndRegister(system, blockId, event.block.location, facing)
                return
            }
        }
    }

    private fun handlePlacement(
        event: BlockPlaceEvent,
        system: BlockSystem<*>,
        descriptor: BlockDescriptor,
        blockId: String,
    ) {
        when (descriptor.placementType) {
            PlacementType.SIMPLE -> {
                val location = event.block.location.clone()
                val facing =
                    if (descriptor.directionalVariants.isEmpty()) {
                        getPlayerFacing(event)
                    } else {
                        BlockFace.SELF
                    }
                createAndRegister(system, blockId, location, facing)
            }
            PlacementType.DIRECTIONAL -> {
                val facing = getDirectionalFacing(event, descriptor)
                val variantId = descriptor.directionalVariants[facing] ?: return
                val location = event.block.location.clone()
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        location.block.setType(Material.AIR, false)
                        NexoBlocks.place(variantId, location)
                        createAndRegister(system, variantId, location, facing)
                    },
                )
            }
            PlacementType.DIRECTIONAL_OPPOSITE -> {
                val facing = getDirectionalFacing(event, descriptor, opposite = true)
                val variantId = descriptor.directionalVariants[facing] ?: blockId
                val location = event.block.location.clone()
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        location.block.setType(Material.AIR, false)
                        NexoBlocks.place(variantId, location)
                        createAndRegister(system, variantId, location, facing)
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
                val baseItemId = block.baseBlockId.ifEmpty { null }
                if (baseItemId != null) {
                    val itemBuilder = NexoItems.itemFromId(baseItemId)
                    if (itemBuilder != null) {
                        val dropLocation = event.block.location.add(0.5, 0.5, 0.5)
                        event.block.world.dropItemNaturally(dropLocation, itemBuilder.build())
                        event.isDropItems = false
                    }
                }
                return
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

    private fun resolveFacingFromVariant(
        descriptor: BlockDescriptor,
        blockId: String,
    ): BlockFace {
        for ((face, id) in descriptor.directionalVariants) {
            if (id == blockId) return face
        }
        return BlockFace.SELF
    }

    companion object {
        fun getDirectionalFacing(
            event: BlockPlaceEvent,
            descriptor: BlockDescriptor,
            opposite: Boolean = false,
        ): BlockFace {
            val raw = getPlayerFacing(event)
            val facing = if (opposite) raw.oppositeFace else raw
            if (descriptor.directionalVariants.containsKey(facing)) return facing
            // Fall back to the player's horizontal look direction
            val fallback = event.player.facing
            return if (opposite) fallback.oppositeFace else fallback
        }

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
