package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.core.AtlasBlockDialog
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

object FluidBlockDialog {
    fun showFluidDialog(
        player: Player,
        fluidBlock: FluidBlock,
        registry: BlockRegistry<*>,
        descriptors: Map<String, BlockDescriptor>,
    ) {
        AtlasBlockDialog.showBlockDialog(
            player,
            fluidBlock,
            registry,
            { block -> getBlockDisplayName(block, descriptors) },
            ::buildFluidInfo,
        )
    }

    private fun getBlockDisplayName(
        fluidBlock: FluidBlock,
        descriptors: Map<String, BlockDescriptor>,
    ): String =
        AtlasBlockDialog.defaultDisplayName(
            descriptors[fluidBlock.baseBlockId],
            fluidBlock.facing,
            fallback = "Fluid Block",
        )

    private fun buildFluidInfo(fluidBlock: FluidBlock): Component {
        val fluidName =
            if (fluidBlock is FluidContainer && fluidBlock.storedAmount > 0) {
                when (fluidBlock.storedFluid) {
                    FluidType.WATER -> "Water (${fluidBlock.storedAmount}/${FluidContainer.MAX_CAPACITY})"
                    FluidType.LAVA -> "Lava (${fluidBlock.storedAmount}/${FluidContainer.MAX_CAPACITY})"
                    FluidType.NONE -> "Empty"
                }
            } else {
                when (fluidBlock.storedFluid) {
                    FluidType.WATER -> "Water"
                    FluidType.LAVA -> "Lava"
                    FluidType.NONE -> "Empty"
                }
            }

        val fluidColor =
            when (fluidBlock.storedFluid) {
                FluidType.WATER -> NamedTextColor.AQUA
                FluidType.LAVA -> NamedTextColor.GOLD
                FluidType.NONE -> NamedTextColor.GRAY
            }

        val statusLine =
            Component.text("Fluid: ")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text(fluidName).color(fluidColor).decoration(TextDecoration.BOLD, false))

        val infoLine =
            when (fluidBlock) {
                is FluidPump ->
                    Component.text("Pump - extracts fluid from adjacent cauldrons (1 power/s)")
                        .color(NamedTextColor.GRAY)
                is FluidPipe ->
                    Component.text("Pipe - joins to its neighbours and carries fluid across the whole run")
                        .color(NamedTextColor.GRAY)
                is FluidContainer ->
                    Component.text("Container - stores up to ${FluidContainer.MAX_CAPACITY} units of fluid")
                        .color(NamedTextColor.GRAY)
                else ->
                    Component.text("Fluid block")
                        .color(NamedTextColor.GRAY)
            }

        var result =
            statusLine
                .append(Component.newline())
                .append(infoLine)

        if (fluidBlock is FluidPump) {
            val powerLine =
                if (fluidBlock.isPowered) {
                    Component.text("Powered").color(NamedTextColor.GREEN)
                } else {
                    Component.text("No Power").color(NamedTextColor.RED)
                }

            val statusText =
                when (fluidBlock.pumpStatus) {
                    FluidPump.PumpStatus.IDLE -> Component.text("Idle — holding fluid").color(NamedTextColor.YELLOW)
                    FluidPump.PumpStatus.EXTRACTING -> Component.text("Extracting from cauldron").color(NamedTextColor.GREEN)
                    FluidPump.PumpStatus.NO_SOURCE -> Component.text("No source nearby").color(NamedTextColor.RED)
                    FluidPump.PumpStatus.NO_POWER -> Component.text("Waiting for power").color(NamedTextColor.RED)
                }

            result =
                result
                    .append(Component.newline())
                    .append(Component.text("Power: ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                    .append(powerLine.decoration(TextDecoration.BOLD, false))
                    .append(Component.newline())
                    .append(Component.text("Status: ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                    .append(statusText.decoration(TextDecoration.BOLD, false))
        }

        return result
    }
}
