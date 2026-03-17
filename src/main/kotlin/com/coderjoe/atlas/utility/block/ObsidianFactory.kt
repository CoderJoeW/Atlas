package com.coderjoe.atlas.utility.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.fluid.FluidType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack

class ObsidianFactory(location: Location) : PowerBlock(location, maxStorage = 100) {
    override val canReceivePower: Boolean = true
    override val updateIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:obsidian_factory"
        const val BLOCK_ID_ACTIVE = "atlas:obsidian_factory_active"
        const val POWER_COST = 100

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Obsidian Factory",
                description = "Machine - consumes $POWER_COST power + water + lava → obsidian",
                placementType = PlacementType.SIMPLE,
                additionalBlockIds = listOf(BLOCK_ID_ACTIVE),
                constructor = { loc, _ -> ObsidianFactory(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String =
        when {
            currentPower >= POWER_COST -> BLOCK_ID_ACTIVE
            else -> BLOCK_ID
        }

    override fun powerUpdate() {
        pullPowerFromNeighbors()

        if (currentPower < POWER_COST) return

        val fluidRegistry = FluidBlockRegistry.instance ?: return

        var waterSource: Pair<com.coderjoe.atlas.fluid.FluidBlock, BlockFace>? = null
        var lavaSource: Pair<com.coderjoe.atlas.fluid.FluidBlock, BlockFace>? = null

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
        removePower(POWER_COST)

        val world = location.world ?: return
        val dropLocation = location.clone().add(0.5, 1.5, 0.5)
        world.dropItem(dropLocation, ItemStack(Material.OBSIDIAN))

        plugin.logger.atlasInfo(
            "ObsidianFactory at ${location.blockX},${location.blockY},${location.blockZ} produced 1 obsidian",
        )
    }

    private fun hasFluidAvailable(
        source: com.coderjoe.atlas.fluid.FluidBlock,
        face: BlockFace,
        fluidType: FluidType,
    ): Boolean {
        return source.canProvideFluid(face.oppositeFace) && source.storedFluid == fluidType
    }
}
