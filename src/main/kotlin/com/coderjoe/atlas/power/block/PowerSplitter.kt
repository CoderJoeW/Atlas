package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.block.BlockFace

class PowerSplitter(location: Location, override val facing: BlockFace) : PowerBlock(location, maxStorage = 10) {
    companion object {
        const val BLOCK_ID = "atlas:power_splitter"

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Power Splitter",
                description = "Cable - distributes power to all adjacent faces",
                placementType = PlacementType.DIRECTIONAL,
                constructor = { loc, facing -> PowerSplitter(loc, facing) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override val updateIntervalTicks: Long = 20L
    private var nextOutputIndex: Int = 0

    override fun getVisualStateBlockId(): String = BLOCK_ID

    override fun powerUpdate() {
        val registry = PowerBlockRegistry.instance ?: return

        val source = registry.getAdjacentPowerBlock(location, facing.oppositeFace)

        if (source != null && canAcceptPower() && source.hasPower()) {
            val remaining = maxStorage - currentPower
            val pulled = source.removePower(minOf(remaining, source.currentPower))
            if (pulled > 0) {
                addPower(pulled)
                plugin.logger.atlasInfo(
                    "PowerSplitter at ${location.coordinates} " +
                        "pulled $pulled power (now $currentPower/$maxStorage)",
                )
            }
        }

        if (hasPower()) {
            val outputFaces = ADJACENT_FACES.filter { it != facing.oppositeFace }
            val faceCount = outputFaces.size
            var lastPushOffset = -1

            for (i in outputFaces.indices) {
                if (!hasPower()) break
                val face = outputFaces[(nextOutputIndex + i) % faceCount]
                val target = registry.getAdjacentPowerBlock(location, face) ?: continue
                if (target.canAcceptPower()) {
                    val pushed = removePower(1)
                    if (pushed > 0) {
                        target.addPower(pushed)
                        lastPushOffset = i
                        plugin.logger.atlasInfo(
                            "PowerSplitter at ${location.coordinates} " +
                                "pushed $pushed power to ${target::class.simpleName} at ${face.name}",
                        )
                    }
                }
            }
            if (lastPushOffset >= 0) {
                nextOutputIndex = (nextOutputIndex + lastPushOffset + 1) % faceCount
            }
        }

        updatePoweredState()
    }
}
