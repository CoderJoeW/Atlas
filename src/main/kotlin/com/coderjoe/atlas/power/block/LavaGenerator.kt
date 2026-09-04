package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.core.pushRoundRobinTo
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace

class LavaGenerator(location: Location) : PowerBlock(location, maxStorage = 20) {
    override val canReceivePower: Boolean = false
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:lava_generator"
        const val BLOCK_ID_ACTIVE = "atlas:lava_generator_active"
        const val POWER_PER_LAVA = 5

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
     * Whether the last update actually burned lava. Reset every tick, so the glow follows the
     * fire rather than the buffer.
     */
    private var burning: Boolean = false

    /**
     * Lit while the generator is burning lava, dark when it is not.
     *
     * Reporting stored charge instead would leave a generator that has filled up and has nothing
     * drawing from it glowing indefinitely, and a generator burning steadily while its output is
     * consumed as fast as it is made would look idle - both the wrong way round.
     */
    override fun getVisualStateBlockId(): String = if (burning) BLOCK_ID_ACTIVE else BLOCK_ID

    private var nextOutputIndex: Int = 0

    override fun powerUpdate() {
        burning = generateFromLava()
        pushPowerToNeighbors()
    }

    /**
     * The generator has no facing, so it offers its output to every side in turn and lets each
     * neighbour's own input rules decide whether to take it.
     */
    private fun pushPowerToNeighbors() {
        if (!hasPower()) return
        val registry = PowerBlockRegistry.instance ?: return

        nextOutputIndex =
            pushRoundRobinTo(
                outputFaces = ADJACENT_FACES,
                startIndex = nextOutputIndex,
                getAdjacent = { face -> registry.getAdjacentBlock(location, face) },
                hasResource = { hasPower() },
                isCandidate = { target -> target.canAcceptPower() },
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

    /** Burns whatever lava the neighbours will give up. Returns whether any was consumed. */
    private fun generateFromLava(): Boolean {
        if (currentPower >= maxStorage) return false

        val fluidRegistry = FluidBlockRegistry.instance ?: return false

        var consumed = false
        for (face in ADJACENT_FACES) {
            val spaceAvailable = maxStorage - currentPower
            if (spaceAvailable < POWER_PER_LAVA) break

            val source = fluidRegistry.getAdjacentBlock(location, face) ?: continue

            val lava = tryPullLava(source, face)
            if (lava) {
                consumed = true
                val generated = addPower(POWER_PER_LAVA)
                plugin.logger.atlasInfo(
                    "LavaGenerator at ${location.coordinates} " +
                        "consumed 1 lava, generated $generated power (now $currentPower/$maxStorage)",
                )
            }
        }
        return consumed
    }

    private fun tryPullLava(
        source: FluidBlock,
        face: BlockFace,
    ): Boolean {
        if (source.canProvideFluid(face.oppositeFace) && source.storedFluid == FluidType.LAVA) {
            source.removeFluid()
            return true
        }
        return false
    }
}
