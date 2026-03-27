package com.coderjoe.atlas.transport

import com.coderjoe.atlas.core.AtlasBlockDialog
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.core.displayName
import com.coderjoe.atlas.transport.block.ConveyorBelt
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object TransportBlockDialog {
    fun showTransportDialog(
        player: Player,
        block: TransportBlock,
        registry: BlockRegistry<*>,
    ) {
        AtlasBlockDialog.showBlockDialog(player, block, registry, ::getBlockDisplayName, ::getBlockDescription)
    }

    private fun getBlockDisplayName(block: TransportBlock): String =
        when (block) {
            is ConveyorBelt -> "Conveyor Belt (${block.facing.displayName()})"
            else -> "Transport Block"
        }

    private fun getBlockDescription(block: TransportBlock): Component =
        when (block) {
            is ConveyorBelt ->
                Component.text("Moves items forward 1 block every second")
                    .color(NamedTextColor.GRAY)
            else ->
                Component.text("Transport block")
                    .color(NamedTextColor.GRAY)
        }
}
