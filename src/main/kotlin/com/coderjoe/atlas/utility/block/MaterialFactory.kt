package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.fluid.FluidBlock
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack

abstract class MaterialFactory(
    location: Location,
    maxStorage: Int,
) : PowerBlock(location, maxStorage) {
    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L

    protected abstract val powerCost: Int
    protected abstract val outputMaterial: Material

    override fun powerUpdate() {
        pullPowerFromNeighbors()

        if (currentPower < powerCost) return

        val fluidRegistry = FluidBlockRegistry.instance ?: return

        var waterSource: Pair<FluidBlock, BlockFace>? = null
        var lavaSource: Pair<FluidBlock, BlockFace>? = null

        for (face in ADJACENT_FACES) {
            val source = fluidRegistry.getAdjacentFluidBlock(location, face) ?: continue
            if (waterSource == null && hasFluidAvailable(source, face, FluidType.WATER)) {
                waterSource = Pair(source, face)
            } else if (lavaSource == null && hasFluidAvailable(source, face, FluidType.LAVA)) {
                lavaSource = Pair(source, face)
            }
            if (waterSource != null && lavaSource != null) break
        }

        if (waterSource == null || lavaSource == null) return

        waterSource.first.removeFluid()
        lavaSource.first.removeFluid()
        removePower(powerCost)

        val world = location.world ?: return
        val dropLocation = location.clone().add(0.5, 1.5, 0.5)
        world.dropItem(dropLocation, ItemStack(outputMaterial))

        plugin.logger.atlasInfo(
            "${this::class.simpleName} at ${location.blockX},${location.blockY},${location.blockZ} " +
                "produced 1 ${outputMaterial.name.lowercase()}",
        )
    }

    private fun hasFluidAvailable(
        source: FluidBlock,
        face: BlockFace,
        fluidType: FluidType,
    ): Boolean {
        return source.canProvideFluid(face.oppositeFace) && source.storedFluid == fluidType
    }
}
