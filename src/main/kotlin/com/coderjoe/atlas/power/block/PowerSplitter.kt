package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.core.pullFromOpposite
import com.coderjoe.atlas.core.pushRoundRobinTo
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.power.branchFaces
import org.bukkit.Location
import org.bukkit.block.BlockFace

class PowerSplitter(location: Location, override val facing: BlockFace) : PowerBlock(location, maxStorage = 10) {
    companion object {
        const val BLOCK_ID = "atlas:power_splitter"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Power Splitter",
                description = "Cable - splits power to two side branches",
                placementType = PlacementType.DIRECTIONAL,
                showFacingInDisplayName = true,
                constructor = { loc, facing -> PowerSplitter(loc, facing) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override val updateIntervalTicks: Long = 20L
    private var nextOutputIndex: Int = 0

    override fun getVisualStateBlockId(): String = BLOCK_ID

    /** One input behind, two outputs branching off either side. */
    override fun canOutputToward(face: BlockFace): Boolean = face in branchFaces(facing)

    /** Power may only enter the splitter through the input face behind it. */
    override fun canAcceptFrom(face: BlockFace): Boolean = face == facing.oppositeFace

    override fun powerUpdate() {
        val registry = PowerBlockRegistry.instance ?: return

        pullFromOpposite(
            facing = facing,
            getAdjacent = { face -> registry.getAdjacentBlock(location, face) },
            canPullSelf = { canAcceptPower() },
            canProvide = { source -> source.hasPower() },
            transfer = { source ->
                val remaining = maxStorage - currentPower
                val pulled = source.removePowerToward(facing, minOf(remaining, source.currentPower))
                if (pulled > 0) {
                    addPower(pulled)
                    plugin.logger.atlasInfo(
                        "PowerSplitter at ${location.coordinates} " +
                            "pulled $pulled power (now $currentPower/$maxStorage)",
                    )
                }
            },
        )

        if (hasPower()) {
            nextOutputIndex =
                pushRoundRobinTo(
                    outputFaces = branchFaces(facing),
                    startIndex = nextOutputIndex,
                    getAdjacent = { face -> registry.getAdjacentBlock(location, face) },
                    hasResource = { hasPower() },
                    isCandidate = { target -> target.canAcceptPower() },
                    tryPush = { target, face ->
                        val pushed = removePower(1)
                        val accepted = target.addPowerFrom(face.oppositeFace, pushed)
                        if (accepted < pushed) addPower(pushed - accepted)
                        if (accepted > 0) {
                            plugin.logger.atlasInfo(
                                "PowerSplitter at ${location.coordinates} " +
                                    "pushed $accepted power to ${target::class.simpleName} at ${face.name}",
                            )
                        }
                        accepted > 0
                    },
                )
        }

        updatePoweredState()
    }
}
