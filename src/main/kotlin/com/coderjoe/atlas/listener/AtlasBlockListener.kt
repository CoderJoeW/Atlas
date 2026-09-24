package com.coderjoe.atlas.listener

import com.coderjoe.atlas.block.AtlasBlock
import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.BlockSystem
import com.coderjoe.atlas.block.PlacementType
import com.coderjoe.atlas.block.power.PowerBlock
import com.coderjoe.atlas.craftengine.CraftEngineHelper
import com.coderjoe.atlas.item.AtlasWrench
import com.coderjoe.atlas.power.PowerNetworkReport
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin

class AtlasBlockListener(
    private val plugin: JavaPlugin,
    private val registry: BlockRegistry,
    private val systems: List<BlockSystem>,
    private val showDialog: (Player, AtlasBlock) -> Unit,
) : Listener {
    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val location = event.block.location
        val key = BlockRegistry.locationKey(location)

        if (registry.updatingLocations.contains(key)) return

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
        system: BlockSystem,
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

    private fun createAndRegister(
        system: BlockSystem,
        blockId: String,
        location: Location,
        facing: BlockFace,
    ) {
        val block = system.factory.create(blockId, location, facing)
        if (block != null) {
            system.registry.register(block, blockId)
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

    /**
     * Atlas blocks only open their dialog for a player holding the [com.coderjoe.atlas.item.AtlasWrench].
     *
     * A bare-handed right-click is left entirely alone, so machines can be built around and walked
     * past without a dialog interrupting, and inspecting one stays a deliberate act.
     */
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.isCancelled) return
        if (!AtlasWrench.isWrench(event.item, plugin)) return

        val clickedBlock = event.clickedBlock ?: return
        val location = clickedBlock.location

        val block = registry.getBlock(location) ?: return
        if (event.player.isSneaking) {
            sneakAction(event.player, block)
        } else {
            showDialog(event.player, block)
        }
        event.isCancelled = true
    }

    /** Sneaking with the wrench reads a power block's network instead of opening its dialog. */
    private fun sneakAction(
        player: Player,
        block: AtlasBlock,
    ) {
        if (block is PowerBlock) {
            PowerNetworkReport.report(player, block)
        } else {
            player.sendMessage(
                Component.text("Nothing to read on this block.").color(NamedTextColor.GRAY),
            )
        }
    }
}
