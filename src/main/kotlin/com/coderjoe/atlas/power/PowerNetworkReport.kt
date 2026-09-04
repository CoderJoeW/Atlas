package com.coderjoe.atlas.power

import com.coderjoe.atlas.power.block.PowerCable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

/**
 * The readout for the otherwise invisible half of the power system.
 *
 * Reports what the run a block belongs to is doing: how many producers and consumers sit on it,
 * how much charge they hold, and how much cable ties them together. Without this a player has no
 * way to tell a network that is starved from one that is simply idle.
 */
object PowerNetworkReport {
    /** Reports on the network reachable from [block], whether it is a cable or something on the edge. */
    fun report(
        player: Player,
        block: PowerBlock,
    ) {
        val cable = nearestCable(block)
        if (cable == null) {
            reportStandalone(player, block)
            return
        }

        val network = PowerNetworks.networkFor(cable)
        val (sources, sinks) = network.terminals()
        val stored = (sources + sinks).distinctBy { it.block.location }.sumOf { it.block.currentPower }
        val capacity = (sources + sinks).distinctBy { it.block.location }.sumOf { it.block.maxStorage }

        player.sendMessage(heading("Power Network"))
        player.sendMessage(row("Cable", "${network.cables.size} block${plural(network.cables.size)}"))
        player.sendMessage(row("Producing", "${sources.size} block${plural(sources.size)} with power to give"))
        player.sendMessage(row("Drawing", "${sinks.size} block${plural(sinks.size)} with room to take"))
        player.sendMessage(row("Stored", "$stored / $capacity"))
        player.sendMessage(diagnosis(sources.size, sinks.size))
    }

    private fun reportStandalone(
        player: Player,
        block: PowerBlock,
    ) {
        player.sendMessage(heading("Power Block"))
        player.sendMessage(row("Stored", "${block.currentPower} / ${block.maxStorage}"))
        player.sendMessage(
            Component.text("  Not joined to any cable.")
                .color(NamedTextColor.YELLOW),
        )
    }

    /** The block itself if it is cable, otherwise any cable touching it. */
    private fun nearestCable(block: PowerBlock): PowerCable? {
        if (block is PowerCable) return block
        val registry = PowerBlockRegistry.instance ?: return null
        return block.let { origin ->
            com.coderjoe.atlas.core.AtlasBlock.ADJACENT_FACES
                .asSequence()
                .mapNotNull { registry.getAdjacentBlock(origin.location, it) }
                .filterIsInstance<PowerCable>()
                .firstOrNull()
        }
    }

    private fun diagnosis(
        sources: Int,
        sinks: Int,
    ): Component =
        when {
            sources == 0 && sinks == 0 ->
                Component.text("  Nothing is attached to this run yet.").color(NamedTextColor.YELLOW)
            sources == 0 ->
                Component.text("  No generator is feeding this run.").color(NamedTextColor.RED)
            sinks == 0 ->
                Component.text("  Nothing on this run can take power.").color(NamedTextColor.RED)
            else ->
                Component.text("  Power is flowing.").color(NamedTextColor.GREEN)
        }

    private fun plural(count: Int) = if (count == 1) "" else "s"

    private fun heading(text: String): Component =
        Component.text("[$text]")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.BOLD, true)

    private fun row(
        label: String,
        value: String,
    ): Component =
        Component.text("  $label: ")
            .color(NamedTextColor.GRAY)
            .append(Component.text(value).color(NamedTextColor.WHITE))
}
