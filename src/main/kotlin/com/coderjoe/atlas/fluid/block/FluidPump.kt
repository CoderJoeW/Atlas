package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.CraftEngineHelper
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Levelled

class FluidPump(location: Location) : FluidBlock(location) {
    enum class PumpStatus {
        IDLE,
        NO_SOURCE,
        NO_POWER,
        EXTRACTING,
    }

    override val updateIntervalTicks: Long = 20L

    var cauldronFace: BlockFace? = null
        private set

    var isPowered: Boolean = false
        private set

    var pumpStatus: PumpStatus = PumpStatus.NO_SOURCE
        private set

    companion object {
        const val BLOCK_ID = "atlas:fluid_pump"

        /** Block state property names for the six connection ports, in [ADJACENT_FACES] order. */
        val CONNECTION_PROPERTIES: Map<BlockFace, String> =
            mapOf(
                BlockFace.NORTH to "north",
                BlockFace.SOUTH to "south",
                BlockFace.EAST to "east",
                BlockFace.WEST to "west",
                BlockFace.UP to "up",
                BlockFace.DOWN to "down",
            )

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Fluid Pump",
                description = "Pump - extracts fluid from adjacent cauldrons or source blocks (1 power/s)",
                placementType = PlacementType.SIMPLE,
                constructor = { loc, _ -> FluidPump(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    /**
     * Hands fluid out through any face.
     *
     * This used to answer only for the face opposite whatever side the source was found on, which
     * gave the pump an output port the player could neither see nor choose - and one that moved
     * on its own if the source was removed and re-found elsewhere.
     */
    override fun canProvideFluid(requestDirection: BlockFace): Boolean = hasFluid()

    /** A pump only ever sources - it fills itself from the world, never from a pipe run. */
    override fun canAcceptFluid(
        face: BlockFace,
        type: FluidType,
    ): Boolean = false

    override fun getVisualStateBlockId(): String = BLOCK_ID

    /**
     * The faces with something plumbed against them.
     *
     * Like the cable's arms, this describes the port rather than the moment: a pipe run counts
     * even while it has nowhere to deliver yet, because it is still connected.
     */
    fun connections(): Set<BlockFace> {
        val registry = FluidBlockRegistry.instance ?: return emptySet()
        return ADJACENT_FACES.filter { face ->
            val neighbor = registry.getAdjacentBlock(location, face) ?: return@filter false
            neighbor is FluidPipe || neighbor.canAcceptFluid(face.oppositeFace)
        }.toSet()
    }

    /** Remembered so the block state is only rewritten when a port or the status changes. */
    private var renderedConnections: Set<BlockFace>? = null
    private var renderedStatus: PumpStatus? = null

    /** Shows which faces are plumbed in, and what the pump is currently doing. */
    private fun renderState() {
        val connections = connections()
        if (connections == renderedConnections && pumpStatus == renderedStatus) return

        CraftEngineHelper.setBooleanProperties(
            location,
            CONNECTION_PROPERTIES.entries.associate { (face, property) -> property to (face in connections) },
        )
        CraftEngineHelper.setStringProperty(location, "status", pumpStatus.name.lowercase())

        renderedConnections = connections
        renderedStatus = pumpStatus
    }

    override fun fluidUpdate() {
        val powerRegistry = PowerBlockRegistry.instance ?: return
        val powerNeighbors =
            ADJACENT_FACES.mapNotNull { face ->
                powerRegistry.getAdjacentBlock(location, face)?.let { face to it }
            }
        isPowered =
            powerNeighbors.any { (face, neighbor) ->
                neighbor.canSupplyPower() && neighbor.canOutputToward(face.oppositeFace)
            }

        if (hasFluid()) {
            pumpStatus = PumpStatus.IDLE
            renderState()
            return
        }

        var foundFace: BlockFace? = null
        var foundBlock: Block? = null
        var fluidType = FluidType.NONE

        for (face in ADJACENT_FACES) {
            val offset = face.direction
            val adjacentBlock =
                location.world?.getBlockAt(
                    location.blockX + offset.blockX,
                    location.blockY + offset.blockY,
                    location.blockZ + offset.blockZ,
                ) ?: continue

            val type =
                when (adjacentBlock.type) {
                    Material.WATER_CAULDRON -> FluidType.WATER
                    Material.LAVA_CAULDRON -> FluidType.LAVA
                    Material.WATER -> {
                        val levelData = adjacentBlock.blockData as? Levelled
                        if (levelData != null && levelData.level == 0) FluidType.WATER else continue
                    }
                    Material.LAVA -> {
                        val levelData = adjacentBlock.blockData as? Levelled
                        if (levelData != null && levelData.level == 0) FluidType.LAVA else continue
                    }
                    else -> continue
                }

            foundFace = face
            foundBlock = adjacentBlock
            fluidType = type
            break
        }

        if (foundFace == null || foundBlock == null) {
            pumpStatus = PumpStatus.NO_SOURCE
            renderState()
            return
        }

        var poweredThisTick = false
        for ((face, neighbor) in powerNeighbors) {
            if (neighbor.canSupplyPower()) {
                val pulled = neighbor.removePowerToward(face.oppositeFace, 1)
                if (pulled > 0) {
                    poweredThisTick = true
                    break
                }
            }
        }

        if (!poweredThisTick) {
            pumpStatus = PumpStatus.NO_POWER
            renderState()
            return
        }

        when (foundBlock.type) {
            Material.WATER_CAULDRON -> {
                val levelled = foundBlock.blockData as? Levelled
                if (levelled != null && levelled.level > 1) {
                    levelled.level = levelled.level - 1
                    foundBlock.blockData = levelled
                } else {
                    foundBlock.setType(Material.CAULDRON, false)
                }
            }
            Material.LAVA_CAULDRON -> {
                foundBlock.setType(Material.CAULDRON, false)
            }
            Material.WATER, Material.LAVA -> {
                foundBlock.setType(Material.AIR, false)
            }
            else -> {}
        }

        storeFluid(fluidType)
        cauldronFace = foundFace
        pumpStatus = PumpStatus.EXTRACTING
        renderState()
        plugin.logger.atlasInfo(
            "FluidPump at ${location.coordinates} extracted ${fluidType.name} from $foundFace",
        )
    }
}
