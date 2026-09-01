package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import com.coderjoe.atlas.power.PowerBlockRegistry
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.BlockFace

class SmallSolarPanel(location: Location) : PowerBlock(location, maxStorage = 4) {
    override val canReceivePower: Boolean = false
    override val updateIntervalTicks: Long = 200L

    companion object {
        const val BLOCK_ID = "atlas:small_solar_panel"
        const val BLOCK_ID_LOW = "atlas:small_solar_panel_low"
        const val BLOCK_ID_MEDIUM = "atlas:small_solar_panel_medium"
        const val BLOCK_ID_HIGH = "atlas:small_solar_panel_high"
        const val BLOCK_ID_FULL = "atlas:small_solar_panel_full"

        /** The panel hands power out through its base pad only; every other face is sealed. */
        val OUTPUT_FACE: BlockFace = BlockFace.DOWN

        private const val DAYTIME_START = 0L
        private const val DAYTIME_END = 12000L

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Small Solar Panel",
                description = "Generator - produces 2 power/10s during daytime, outputs from its base",
                placementType = PlacementType.SIMPLE,
                additionalBlockIds =
                    listOf(BLOCK_ID_LOW, BLOCK_ID_MEDIUM, BLOCK_ID_HIGH, BLOCK_ID_FULL),
                constructor = { loc, _ -> SmallSolarPanel(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    /** One visual step per unit of charge; [maxStorage] is 4, so the array reads 0-4 directly. */
    override fun getVisualStateBlockId(): String =
        when (currentPower) {
            0 -> BLOCK_ID
            1 -> BLOCK_ID_LOW
            2 -> BLOCK_ID_MEDIUM
            3 -> BLOCK_ID_HIGH
            else -> BLOCK_ID_FULL
        }

    override fun canOutputToward(face: BlockFace): Boolean = face == OUTPUT_FACE

    override fun powerUpdate() {
        val world = location.world ?: return

        if (isCollectingSunlight(world)) {
            val generated = addPower(2)
            if (generated > 0) {
                plugin.logger.atlasInfo(
                    "SmallSolarPanel at ${location.coordinates} " +
                        "generated $generated power (now $currentPower/$maxStorage)",
                )
            }
        }

        pushPowerToOutput()
    }

    /** Drives stored power out through [OUTPUT_FACE] into whatever sits below the base pad. */
    private fun pushPowerToOutput() {
        if (!hasPower()) return
        val registry = PowerBlockRegistry.instance ?: return
        val target = registry.getAdjacentBlock(location, OUTPUT_FACE) ?: return
        if (!target.canAcceptPower()) return

        val accepted = target.addPower(currentPower)
        if (accepted > 0) {
            removePowerToward(OUTPUT_FACE, accepted)
            plugin.logger.atlasInfo(
                "SmallSolarPanel at ${location.coordinates} " +
                    "pushed $accepted power to ${target::class.simpleName} " +
                    "(now $currentPower/$maxStorage)",
            )
        }
    }

    private fun isCollectingSunlight(world: World): Boolean = world.time in DAYTIME_START..DAYTIME_END
}
