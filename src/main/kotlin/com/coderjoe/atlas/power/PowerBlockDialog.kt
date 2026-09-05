package com.coderjoe.atlas.power

import com.coderjoe.atlas.core.AtlasBlockDialog
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.power.block.LavaGenerator
import com.coderjoe.atlas.power.block.PowerCable
import com.coderjoe.atlas.power.block.SmallBattery
import com.coderjoe.atlas.power.block.SmallSolarPanel
import com.coderjoe.atlas.utility.block.AutoSmelter
import com.coderjoe.atlas.utility.block.CobblestoneFactory
import com.coderjoe.atlas.utility.block.Crusher
import com.coderjoe.atlas.utility.block.Mine
import com.coderjoe.atlas.utility.block.ObsidianFactory
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

object PowerBlockDialog {
    fun showPowerDialog(
        player: Player,
        powerBlock: PowerBlock,
        registry: BlockRegistry<*>,
        descriptors: Map<String, BlockDescriptor>,
    ) {
        AtlasBlockDialog.showBlockDialog(
            player,
            powerBlock,
            registry,
            { block -> getBlockDisplayName(block, descriptors) },
            ::buildPowerInfo,
        )
    }

    private fun getBlockDisplayName(
        powerBlock: PowerBlock,
        descriptors: Map<String, BlockDescriptor>,
    ): String =
        AtlasBlockDialog.defaultDisplayName(
            descriptors[powerBlock.baseBlockId],
            powerBlock.facing,
            fallback = "Power Block",
        )

    private fun buildPowerInfo(powerBlock: PowerBlock): Component {
        val ratio =
            if (powerBlock.maxStorage > 0) {
                powerBlock.currentPower.toFloat() / powerBlock.maxStorage
            } else {
                0f
            }

        val barLength = 10
        val filled = (ratio * barLength).toInt()
        val empty = barLength - filled

        val barColor =
            when {
                ratio >= 0.7f -> NamedTextColor.GREEN
                ratio >= 0.3f -> NamedTextColor.YELLOW
                else -> NamedTextColor.RED
            }

        val bar =
            Component.text("[")
                .color(NamedTextColor.GRAY)
                .append(Component.text("\u2588".repeat(filled)).color(barColor))
                .append(Component.text("\u2591".repeat(empty)).color(NamedTextColor.DARK_GRAY))
                .append(Component.text("]").color(NamedTextColor.GRAY))
                .append(Component.text(" ${(ratio * 100).toInt()}%").color(barColor))

        val powerLine =
            Component.text("Power: ${powerBlock.currentPower}/${powerBlock.maxStorage}")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)

        val infoLine =
            when (powerBlock) {
                is SmallSolarPanel ->
                    Component.text("Generator - produces 1 power/min during daytime")
                        .color(NamedTextColor.GRAY)
                is SmallBattery ->
                    Component.text("Storage - holds up to 10 power")
                        .color(NamedTextColor.GRAY)
                is PowerCable ->
                    Component.text("Cable - carries power across the whole run")
                        .color(NamedTextColor.GRAY)
                is LavaGenerator ->
                    Component.text("Generator - produces ${LavaGenerator.POWER_PER_LAVA} power per lava unit")
                        .color(NamedTextColor.GRAY)
                is AutoSmelter ->
                    Component.text("Machine - smelts items passing through, consumes ${AutoSmelter.POWER_PER_SMELT} power/item")
                        .color(NamedTextColor.GRAY)
                is CobblestoneFactory ->
                    Component.text("Machine - consumes ${CobblestoneFactory.POWER_COST} power + water + lava → cobblestone")
                        .color(NamedTextColor.GRAY)
                is ObsidianFactory ->
                    Component.text("Machine - consumes ${ObsidianFactory.POWER_COST} power + water + lava → obsidian")
                        .color(NamedTextColor.GRAY)
                is Crusher ->
                    Component.text("Machine - crushes ore blocks into 2x ores, consumes ${Crusher.POWER_PER_CRUSH} power/item")
                        .color(NamedTextColor.GRAY)
                // One branch covers all seven mines: they differ only in what they dig, what a
                // haul costs and how long the bore takes.
                is Mine ->
                    Component.text(
                        "Mine - digs 1 ${powerBlock.output.name.lowercase().replace('_', ' ')} " +
                            "per ${powerBlock.powerPerHaul} power",
                    ).color(NamedTextColor.GRAY)
                else ->
                    Component.text("Power block")
                        .color(NamedTextColor.GRAY)
            }

        return powerLine
            .append(Component.newline())
            .append(bar)
            .append(Component.newline())
            .append(infoLine)
    }
}
