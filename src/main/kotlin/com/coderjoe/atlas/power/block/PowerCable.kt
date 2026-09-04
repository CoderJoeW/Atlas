package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.CraftEngineHelper
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.power.PowerNetworks
import org.bukkit.Location
import org.bukkit.block.BlockFace

/**
 * The only piece of power transport there is.
 *
 * A cable has no facing and stores nothing. It joins itself to whatever power block sits against
 * it, and the run it belongs to moves power from producers to consumers as one
 * [com.coderjoe.atlas.power.PowerNetwork]. The model grows an arm on exactly the faces that are
 * connected, so what the player sees is the wiring, not an orientation they have to remember.
 */
class PowerCable(location: Location) : PowerBlock(location, maxStorage = 0) {
    companion object {
        const val BLOCK_ID = "atlas:power_cable"

        /** Block state property names for the six connection arms, in [ADJACENT_FACES] order. */
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
                displayName = "Power Cable",
                description = "Cable - joins to its neighbours and carries power across the whole run",
                placementType = PlacementType.SIMPLE,
                constructor = { loc, _ -> PowerCable(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override val updateIntervalTicks: Long = 20L

    /** Set by the run's leader each tick: whether this network moved any power. */
    internal var carrying: Boolean = false

    /** Remembered so the block state is only rewritten when the shape or flow changes. */
    private var renderedConnections: Set<BlockFace>? = null
    private var renderedPowered: Boolean? = null

    override fun getVisualStateBlockId(): String = BLOCK_ID

    /**
     * The faces the model grows an arm on: those with a power block against them that can
     * actually trade power through that side.
     *
     * A neighbour is asked about the face pointing back at this cable, so a block with a
     * dedicated port - a solar panel handing power out of its base only - is joined from that
     * side alone and left alone everywhere else.
     */
    fun connections(): Set<BlockFace> {
        val registry = PowerBlockRegistry.instance ?: return emptySet()
        return ADJACENT_FACES.filter { face ->
            val neighbor = registry.getAdjacentBlock(location, face)
            neighbor != null && neighbor.canConnectToward(face.oppositeFace)
        }.toSet()
    }

    /**
     * A cable holds nothing, so a consumer drawing from it is really drawing from the producers
     * on its run. The face is ignored: a cable is the same on every side.
     */
    override fun removePowerToward(
        face: BlockFace,
        amount: Int,
    ): Int = PowerNetworks.networkFor(this).draw(amount)

    /** A cable is "live" when the run it belongs to has a producer with something to give. */
    override fun canSupplyPower(): Boolean = PowerNetworks.networkFor(this).terminals().first.isNotEmpty()

    override fun powerUpdate() {
        val network = PowerNetworks.networkFor(this)

        // every cable in a run discovers the same network, so only its leader runs the transfer,
        // and it tells the whole run whether it is live
        if (network.leader === this) {
            val moved = network.transfer() > 0
            // A run is lit whenever a generator on it has charge, not only in the tick power
            // happens to move. Otherwise a full solar panel with nothing drawing from it yet
            // looks exactly like a run with no generator at all.
            val live = moved || network.hasSupply()
            for (cable in network.cables) cable.carrying = live
        }

        renderConnections()
    }

    /** Rewrites the six arm properties and the glow, but only when something actually changed. */
    private fun renderConnections() {
        val connections = connections()
        if (connections == renderedConnections && carrying == renderedPowered) return

        val properties =
            CONNECTION_PROPERTIES.entries.associate { (face, property) ->
                property to (face in connections)
            } + ("powered" to carrying)
        CraftEngineHelper.setBooleanProperties(location, properties)

        renderedConnections = connections
        renderedPowered = carrying
    }
}
