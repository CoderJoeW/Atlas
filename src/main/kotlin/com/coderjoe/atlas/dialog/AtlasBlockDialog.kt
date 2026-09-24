package com.coderjoe.atlas.dialog

import com.coderjoe.atlas.block.BlockDescriptor
import com.coderjoe.atlas.util.displayName
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player

object AtlasBlockDialog {
    fun defaultDisplayName(
        descriptor: BlockDescriptor?,
        facing: BlockFace,
        fallback: String,
    ): String {
        val baseName = descriptor?.displayName ?: fallback
        return if (descriptor?.showFacingInDisplayName == true) "$baseName (${facing.displayName()})" else baseName
    }

    fun createNoticeDialog(
        title: Component,
        body: Component,
        onClose: (Player) -> Unit,
    ): Dialog {
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

        return Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(title)
                        .body(listOf(DialogBody.plainMessage(body)))
                        .canCloseWithEscape(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .build(),
                )
                .type(DialogType.notice(closeButton))
        }
    }
}
