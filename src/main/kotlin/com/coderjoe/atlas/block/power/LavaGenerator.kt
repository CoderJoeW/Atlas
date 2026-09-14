package com.coderjoe.atlas.block.power

import com.coderjoe.atlas.util.atlasInfo
import com.coderjoe.atlas.util.coordinates
import com.coderjoe.atlas.block.AtlasBlocks
import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.block.PlacementType
import com.coderjoe.atlas.block.capability.PowerConsumer
import com.coderjoe.atlas.block.pushRoundRobinTo
import com.coderjoe.atlas.block.capability.FluidConsumer
import com.coderjoe.atlas.block.capability.FluidType
import org.bukkit.Location
import org.bukkit.block.BlockFace

class LavaGenerator(location: Location) : PowerBlock(location, maxStorage = 20), FluidConsumer {
    override val canReceivePower: Boolean = false
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:lava_generator"
        const val BLOCK_ID_ACTIVE = "atlas:lava_generator_active"
        const val POWER_PER_LAVA = 2

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Lava Generator",
                description = "Generator - produces $POWER_PER_LAVA power per lava unit",
                placementType = PlacementType.SIMPLE,
                additionalBlockIds = listOf(BLOCK_ID_ACTIVE),
                constructor = { loc, _ -> LavaGenerator(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    /**
     * Set the instant a pushed unit of lava is burned, and read once per tick by
     * [getVisualStateBlockId] via [showBurning] - see that property for why it isn't read
     * directly.
     */
    private var burning: Boolean = false

    /**
     * What the last tick actually rendered as burning.
     *
     * A pipe network pushes lava to this generator on the network's own tick, not this block's,
     * so [burning] can be set at any point between two of this generator's own updates. Reading it
     * once per [powerUpdate] and resetting it there - rather than reading [burning] directly from
     * [getVisualStateBlockId], which [AtlasBlock.start] calls immediately after [powerUpdate] in
     * the same cycle and would otherwise see whatever this same tick just reset it to - keeps the
     * glow tied to "burned since my last update" instead of racing that reset.
     */
    private var showBurning: Boolean = false

    /**
     * Lit while the generator is burning lava, dark when it is not.
     *
     * Reporting stored charge instead would leave a generator that has filled up and has nothing
     * drawing from it glowing indefinitely, and a generator burning steadily while its output is
     * consumed as fast as it is made would look idle - both the wrong way round.
     */
    override fun getVisualStateBlockId(): String = if (showBurning) BLOCK_ID_ACTIVE else BLOCK_ID

    /** The generator has no facing, so a pipe against any side reads as plumbed in. */
    override fun drawsFluidFrom(face: BlockFace): Boolean = currentPower < maxStorage

    override fun wantsFluid(type: FluidType): Boolean = type == FluidType.LAVA && currentPower < maxStorage

    /** Burns a pushed unit of lava on the spot, rather than banking it for later. */
    override fun acceptFluid(
        face: BlockFace,
        type: FluidType,
    ): Boolean {
        if (type != FluidType.LAVA || currentPower >= maxStorage) return false

        val generated = addPower(POWER_PER_LAVA)
        burning = true
        plugin.logger.atlasInfo(
            "LavaGenerator at ${location.coordinates} " +
                "consumed 1 lava, generated $generated power (now $currentPower/$maxStorage)",
        )
        return true
    }

    private var nextOutputIndex: Int = 0

    override fun powerUpdate() {
        showBurning = burning
        burning = false
        pushPowerToNeighbors()
    }

    /**
     * The generator has no facing, so it offers its output to every side in turn and lets each
     * neighbour's own input rules decide whether to take it.
     */
    private fun pushPowerToNeighbors() {
        if (!hasPower()) return

        nextOutputIndex =
            pushRoundRobinTo(
                outputFaces = ADJACENT_FACES,
                startIndex = nextOutputIndex,
                // Every Atlas block, not just power blocks: a fluid pump takes power too, and it
                // is registered elsewhere, so scanning the power registry alone walked past it.
                getAdjacent = { face -> AtlasBlocks.adjacent(location, face) },
                hasResource = { hasPower() },
                isCandidate = { target ->
                    (target as? PowerBlock)?.canAcceptPower() == true ||
                        (target as? PowerConsumer)?.wantsPower() == true
                },
                tryPush = { _, face ->
                    val accepted = pushPowerToward(face, 1)
                    if (accepted > 0) {
                        plugin.logger.atlasInfo(
                            "LavaGenerator at ${location.coordinates} " +
                                "pushed $accepted power ${face.name} (now $currentPower/$maxStorage)",
                        )
                    }
                    accepted > 0
                },
            )
    }
}
