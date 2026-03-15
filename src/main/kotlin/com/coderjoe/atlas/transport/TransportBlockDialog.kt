package com.coderjoe.atlas.transport

import com.coderjoe.atlas.core.AtlasBlockDialog
import com.coderjoe.atlas.core.BlockRegistry
import com.coderjoe.atlas.transport.block.ConveyorBelt
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
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
        val body = DialogBody.plainMessage(bodyText)

        val closeAction =
            DialogAction.customClick(
                DialogActionCallback { _, audience ->
                    val p = audience as? Player ?: return@DialogActionCallback
                    onClose(p)
                },
                ClickCallback.Options.builder().build(),
            )

        val closeButton =
            ActionButton.builder(Component.text("Close"))
                .action(closeAction)
                .build()

        val dialog =
            Dialog.create { factory ->
                factory.empty()
                    .base(
                        DialogBase.builder(title)
                            .body(listOf(body))
                            .canCloseWithEscape(false)
                            .afterAction(DialogBase.DialogAfterAction.CLOSE)
                            .build(),
                    )
                    .type(DialogType.notice(closeButton))
            }

        player.showDialog(dialog)
    }

    private fun getBlockDisplayName(block: TransportBlock): String =
        when (block) {
            is ConveyorBelt -> "Conveyor Belt (${block.facing.name.lowercase().replaceFirstChar { it.uppercase() }})"
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
