package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.BlockFace

class SmallSolarPanel(location: Location) : PowerBlock(location, maxStorage = 4) {
    override val canReceivePower: Boolean = false
    override val updateIntervalTicks: Long = 200L

    companion object {
        const val BLOCK_ID = "atlas:small_solar_panel"
        const val BLOCK_ID_ACTIVE = "atlas:small_solar_panel_active"

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
                additionalBlockIds = listOf(BLOCK_ID_ACTIVE),
                constructor = { loc, _ -> SmallSolarPanel(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    /**
     * Lit while the panel is taking in sunlight, dark when it is not.
     *
     * The readout answers "is this thing working?", which is what a player standing in front of
     * it wants to know - not how many units happen to be buffered at that instant.
     */
    override fun getVisualStateBlockId(): String {
        val world = location.world ?: return BLOCK_ID
        return if (isCollectingSunlight(world)) BLOCK_ID_ACTIVE else BLOCK_ID
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
        val accepted = pushPowerToward(OUTPUT_FACE)
        if (accepted > 0) {
            plugin.logger.atlasInfo(
                "SmallSolarPanel at ${location.coordinates} " +
                    "pushed $accepted power out of its base (now $currentPower/$maxStorage)",
            )
        }
    }

    private fun isCollectingSunlight(world: World): Boolean = world.time in DAYTIME_START..DAYTIME_END
}
