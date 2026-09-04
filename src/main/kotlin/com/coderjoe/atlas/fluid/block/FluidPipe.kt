package com.coderjoe.atlas.fluid.block

import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.CraftEngineHelper
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidNetworks
import com.coderjoe.atlas.fluid.FluidType
import org.bukkit.Location
import org.bukkit.block.BlockFace

/**
 * The only piece of fluid transport there is.
 *
 * A pipe has no facing and holds nothing. It joins itself to whatever fluid block sits against
 * it, and the run it belongs to moves fluid from providers to acceptors as one
 * [com.coderjoe.atlas.fluid.FluidNetwork]. The model grows an arm on exactly the faces that are
 * connected, so what the player sees is the plumbing, not an orientation they have to remember.
 */
class FluidPipe(location: Location) : FluidBlock(location) {
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:fluid_pipe"

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
                displayName = "Fluid Pipe",
                description = "Pipe - joins to its neighbours and carries fluid across the whole run",
                placementType = PlacementType.SIMPLE,
                constructor = { loc, _ -> FluidPipe(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String = BLOCK_ID

    /** Set by the run's leader each tick: what the network is carrying, for the glow in the pipe. */
    internal var carrying: FluidType = FluidType.NONE

    /** Remembered so the block state is only rewritten when the shape or the flow changes. */
    private var renderedConnections: Set<BlockFace>? = null
    private var renderedFluid: FluidType? = null

    /**
     * The faces the model grows an arm on: those with a fluid block against them that can
     * actually trade fluid through that side.
     *
     * A neighbour is asked about the face pointing back at this pipe, so a block with a
     * dedicated port - a container filling through its back only - is joined from that side
     * alone and left alone everywhere else.
     */
    fun connections(): Set<BlockFace> {
        val registry = FluidBlockRegistry.instance ?: return emptySet()
        return ADJACENT_FACES.filter { face ->
            val neighbor = registry.getAdjacentBlock(location, face) ?: return@filter false
            val back = face.oppositeFace
            neighbor is FluidPipe || neighbor.canProvideFluid(back) || neighbor.canAcceptFluid(back)
        }.toSet()
    }

    /** A pipe holds nothing of its own, so it answers for the run it belongs to. */
    override fun hasFluid(): Boolean = FluidNetworks.networkFor(this).availableFluid() != FluidType.NONE

    override fun canProvideFluid(requestDirection: BlockFace): Boolean = hasFluid()

    override fun canAcceptFluid(
        face: BlockFace,
        type: FluidType,
    ): Boolean = FluidNetworks.networkFor(this).terminals().second.isNotEmpty()

    /** Drawing from a pipe is really drawing from the providers on its run. */
    override fun removeFluid(): FluidType = FluidNetworks.networkFor(this).draw()

    /** Pushing into a pipe is really pushing to an acceptor on its run. */
    override fun storeFluid(type: FluidType): Boolean = FluidNetworks.networkFor(this).deliver(type)

    override fun fluidUpdate() {
        val network = FluidNetworks.networkFor(this)

        // every pipe in a run discovers the same network, so only its leader runs the transfer,
        // and it tells the whole run what is flowing
        if (network.leader === this) {
            val moved = network.transfer()
            // A run reads as carrying whenever a provider on it has something to give, not only
            // in the tick a unit happens to move. Otherwise a full pump with nothing drawing from
            // it yet looks exactly like a run with no source at all.
            val flowing = if (moved != FluidType.NONE) moved else network.availableFluid()
            for (pipe in network.pipes) pipe.carrying = flowing
        }

        renderConnections()
    }

    /** Rewrites the six arm properties and the fluid colour, but only when something changed. */
    private fun renderConnections() {
        val connections = connections()
        if (connections == renderedConnections && carrying == renderedFluid) return

        CraftEngineHelper.setBooleanProperties(
            location,
            CONNECTION_PROPERTIES.entries.associate { (face, property) -> property to (face in connections) },
        )
        CraftEngineHelper.setStringProperty(location, "fluid", carrying.name.lowercase())

        renderedConnections = connections
        renderedFluid = carrying
    }
}
