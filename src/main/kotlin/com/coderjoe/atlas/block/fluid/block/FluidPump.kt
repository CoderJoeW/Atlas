package com.coderjoe.atlas.block.fluid.block

import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.block.BlockRegistry
import com.coderjoe.atlas.block.Inspection
import com.coderjoe.atlas.block.PlacementType
import com.coderjoe.atlas.block.StatusLine
import com.coderjoe.atlas.block.Tone
import com.coderjoe.atlas.block.capability.FluidConsumer
import com.coderjoe.atlas.block.capability.FluidType
import com.coderjoe.atlas.block.capability.PowerConsumer
import com.coderjoe.atlas.block.fluid.FluidBlock
import com.coderjoe.atlas.craftengine.CraftEngineHelper
import com.coderjoe.atlas.util.atlasInfo
import com.coderjoe.atlas.util.coordinates
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

    override fun writeSaveData(data: MutableMap<String, Any>) {
        super.writeSaveData(data)
        data["storedPower"] = storedPower
    }

    override fun readSaveData(data: Map<String, Any?>) {
        super.readSaveData(data)
        storedPower = ((data["storedPower"] as? Number)?.toInt() ?: 0).coerceIn(0, POWER_CAPACITY)
    }

    override fun inspect(): Inspection {
        val base = super.inspect()
        val power = if (isPowered) StatusLine("Powered", Tone.GOOD) else StatusLine("No Power", Tone.FAULT)

        val status =
            when (pumpStatus) {
                PumpStatus.IDLE -> StatusLine("Idle — holding fluid", Tone.WARNING)
                PumpStatus.EXTRACTING -> StatusLine("Extracting from source", Tone.GOOD)
                PumpStatus.NO_SOURCE -> StatusLine("No source nearby", Tone.FAULT)
                PumpStatus.NO_POWER -> StatusLine("Waiting for power", Tone.FAULT)
            }

        return base.copy(lines = base.lines + power + status)
    }

    override val pushesFluid: Boolean = true

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
        val registry = BlockRegistry.active ?: return emptySet()
        return ADJACENT_FACES.filter { face ->
            val back = face.oppositeFace

            when (val neighbor = registry.getAdjacentBlock(location, face)) {
                is FluidPipe -> true
                is FluidBlock -> neighbor.canAcceptFluid(back)
                // A machine from another system is fed straight off the pump when it sits against
                // one, so it has to show as plumbed in like anything else.
                else -> (neighbor as? FluidConsumer)?.drawsFluidFrom(back) == true
            }
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
        val registry = BlockRegistry.active ?: return false
        val fluid = storedFluid
        if (fluid == FluidType.NONE) return false

        for (face in ADJACENT_FACES) {
            val back = face.oppositeFace
            when (val neighbor = registry.getAdjacentBlock(location, face)) {
                is FluidBlock -> {
                    if (!neighbor.canAcceptFluid(back, fluid)) continue
                    if (neighbor.storeFluid(fluid)) {
                        removeFluid()
                        return true
                    }
                }

                // A block that takes fluid without being a fluid block - a material factory is
                // one - is pushed to like anything else rather than left to reach back for what
                // it needs. The same fold PowerBlock.pushPowerToward makes on the power side.
                is FluidConsumer -> {
                    if (!neighbor.drawsFluidFrom(back) || !neighbor.wantsFluid(fluid)) continue
                    if (neighbor.acceptFluid(back, fluid)) {
                        removeFluid()
                        return true
                    }
                }

                else -> continue
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
