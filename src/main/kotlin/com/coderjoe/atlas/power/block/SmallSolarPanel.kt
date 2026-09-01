package com.coderjoe.atlas.power.block

import com.coderjoe.atlas.atlasInfo
import com.coderjoe.atlas.coordinates
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.PlacementType
import com.coderjoe.atlas.power.PowerBlock
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World

class SmallSolarPanel(location: Location) : PowerBlock(location, maxStorage = 4) {
    override val canReceivePower: Boolean = false
    override val updateIntervalTicks: Long = 200L
    override val effectIntervalTicks: Long = 20L

    companion object {
        const val BLOCK_ID = "atlas:small_solar_panel"
        const val BLOCK_ID_FULL = "atlas:small_solar_panel_full"

        /** Height of the tilted panel face above the block origin, in blocks. */
        private const val PANEL_SURFACE_HEIGHT = 0.7

        private const val DAYTIME_START = 0L
        private const val DAYTIME_END = 12000L

        private val SUNLIGHT_COLOR: Color = Color.fromRGB(255, 190, 60)

        val descriptor =
            BlockDescriptor(
                baseBlockId = BLOCK_ID,
                displayName = "Small Solar Panel",
                description = "Generator - produces 2 power/10s during daytime",
                placementType = PlacementType.SIMPLE,
                additionalBlockIds = listOf(BLOCK_ID_FULL),
                constructor = { loc, _ -> SmallSolarPanel(loc) },
            )
    }

    override val baseBlockId: String = BLOCK_ID

    override fun getVisualStateBlockId(): String =
        when (currentPower) {
            0 -> BLOCK_ID
            else -> BLOCK_ID_FULL
        }

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
    }

    override fun spawnEffects() {
        val world = location.world ?: return
        if (!isCollectingSunlight(world)) return

        val x = location.x + 0.5
        val y = location.y + PANEL_SURFACE_HEIGHT
        val z = location.z + 0.5

        world.spawnParticle(Particle.ELECTRIC_SPARK, x, y, z, 2, 0.3, 0.05, 0.3, 0.0)
        world.spawnParticle(
            Particle.DUST,
            x, y, z,
            1, 0.25, 0.05, 0.25, 0.0,
            Particle.DustOptions(SUNLIGHT_COLOR, 0.8f),
        )
    }

    private fun isCollectingSunlight(world: World): Boolean = world.time in DAYTIME_START..DAYTIME_END
}
