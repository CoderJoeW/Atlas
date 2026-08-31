package com.coderjoe.atlas.transport

import com.coderjoe.atlas.core.AtlasBlockDialog
import com.coderjoe.atlas.core.BlockDescriptor
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.transport.block.ConveyorBelt
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object TransportBlockDialog {
    fun showTransportDialog(
        player: Player,
        block: TransportBlock,
        registry: BlockRegistry<*>,
        descriptors: Map<String, BlockDescriptor>,
    ) {
        AtlasBlockDialog.showBlockDialog(
            player,
            block,
            registry,
            { b -> getBlockDisplayName(b, descriptors) },
            ::getBlockDescription,
        )
    }

    private fun getBlockDisplayName(
        block: TransportBlock,
        descriptors: Map<String, BlockDescriptor>,
    ): String =
        AtlasBlockDialog.defaultDisplayName(
            descriptors[block.baseBlockId],
            block.facing,
            fallback = "Transport Block",
        )

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
