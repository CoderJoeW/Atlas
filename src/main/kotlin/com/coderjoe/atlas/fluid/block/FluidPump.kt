package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.CraftEngineHelper
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.core.PowerConsumer
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Levelled

class FluidPump(location: Location) : FluidBlock(location), PowerConsumer {
    enum class PumpStatus {
        IDLE,
        NO_SOURCE,
        NO_POWER,
        EXTRACTING,
    }

    override val updateIntervalTicks: Long = 20L

    var cauldronFace: BlockFace? = null
        private set

    /**
     * Power pushed in by a cable run and not yet spent.
     *
     * The pump used to reach out and take a unit from whatever generator it could see at the
     * moment it extracted. Power is a push system, so it waits to be fed instead and spends from
     * this buffer, which also means a pump keeps working through a tick where the run happens to
     * be busy elsewhere.
     */
    var storedPower: Int = 0
        private set

    val isPowered: Boolean get() = storedPower >= POWER_PER_EXTRACT

    var pumpStatus: PumpStatus = PumpStatus.NO_SOURCE
        private set

    companion object {
        const val BLOCK_ID = "atlas:fluid_pump"

        /** Spent on each unit of fluid lifted out of the world. */
        const val POWER_PER_EXTRACT = 1

        /** How much pushed power the pump will hold. A few extractions' worth is plenty. */
        const val POWER_CAPACITY = 4

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

    /** A pump takes power in through any side, so a cable touching it anywhere joins to it. */
    override fun drawsPowerFrom(face: BlockFace): Boolean = true

    override fun wantsPower(): Boolean = storedPower < POWER_CAPACITY

    override fun acceptPower(
        face: BlockFace,
        amount: Int,
    ): Int {
        val taken = minOf(amount, POWER_CAPACITY - storedPower)
        if (taken <= 0) return 0
        storedPower += taken
        return taken
    }

    /** Restores the buffer across a restart. */
    fun restorePower(amount: Int) {
        storedPower = amount.coerceIn(0, POWER_CAPACITY)
    }

    /**
     * The pump moves its own fluid out rather than waiting for a run to pull it.
     *
     * It is the only block in the system that lifts fluid out of the world, so it is the one that
     * knows a unit exists; leaving the run to notice meant the pipe had to reach back into the
     * pump on every tick to check.
     */
    override val pushesFluid: Boolean = true

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
    private var renderedStatus: String? = null

    /**
     * The `status` block state.
     *
     * Working states carry the fluid as well, so a pump reads as water or lava at a glance
     * instead of only "busy" - the two look nothing alike downstream, and a player wiring a lava
     * line into a water tank wants to see the mistake on the pump rather than at the tank.
     * The states with nothing in hand have no fluid to name.
     */
    private fun statusProperty(): String =
        when (pumpStatus) {
            PumpStatus.NO_SOURCE, PumpStatus.NO_POWER -> pumpStatus.name.lowercase()
            PumpStatus.IDLE, PumpStatus.EXTRACTING ->
                "${pumpStatus.name.lowercase()}_${if (storedFluid == FluidType.LAVA) "lava" else "water"}"
        }

    /** Shows which faces are plumbed in, what the pump is doing, and what it is handling. */
    private fun renderState() {
        val connections = connections()
        val status = statusProperty()
        if (connections == renderedConnections && status == renderedStatus) return

        CraftEngineHelper.setBooleanProperties(
            location,
            CONNECTION_PROPERTIES.entries.associate { (face, property) -> property to (face in connections) },
        )
        CraftEngineHelper.setStringProperty(location, "status", status)

        renderedConnections = connections
        renderedStatus = status
    }

    /**
     * Hands what the pump is holding to whatever will take it.
     *
     * A pipe takes it on behalf of its whole run, so this reaches anything plumbed to that run;
     * a tank sitting straight against the pump is fed directly. Returns whether the unit moved.
     */
    private fun pushFluid(): Boolean {
        val registry = FluidBlockRegistry.instance ?: return false
        val fluid = storedFluid
        if (fluid == FluidType.NONE) return false

        for (face in ADJACENT_FACES) {
            val neighbor = registry.getAdjacentBlock(location, face) ?: continue
            if (!neighbor.canAcceptFluid(face.oppositeFace, fluid)) continue
            if (neighbor.storeFluid(fluid)) {
                removeFluid()
                return true
            }
        }
        return false
    }

    override fun fluidUpdate() {
        // The pump owns what it lifted, so it hands it on itself rather than waiting to be drained
        if (hasFluid()) pushFluid()

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

        if (storedPower < POWER_PER_EXTRACT) {
            pumpStatus = PumpStatus.NO_POWER
            renderState()
            return
        }
        storedPower -= POWER_PER_EXTRACT

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
