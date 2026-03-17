package com.coderjoe.atlas.transport

import com.coderjoe.atlas.core.AtlasBlockDialog
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.core.displayName
import com.coderjoe.atlas.transport.block.ConveyorBelt
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

object TransportBlockDialog {
    fun init(plugin: JavaPlugin) {
        AtlasBlockDialog.init(plugin)
    }

    fun showTransportDialog(
        player: Player,
        block: TransportBlock,
        registry: BlockRegistry<*>,
    ) {
        AtlasBlockDialog.showDialog(player, block, registry) { p, b, onClose ->
            sendDialog(p, b as TransportBlock, onClose)
        }
    }

    fun cleanup() {
        AtlasBlockDialog.cleanup()
    }

    private fun sendDialog(
        player: Player,
        block: TransportBlock,
        onClose: (Player) -> Unit,
    ) {
        val title = Component.text(getBlockDisplayName(block))
        val bodyText = getBlockDescription(block)
        val dialog = AtlasBlockDialog.createNoticeDialog(title, bodyText, onClose)
        player.showDialog(dialog)
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
