package com.coderjoe.atlas.core

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
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

fun BlockFace.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

object AtlasBlockDialog {
    private lateinit var plugin: JavaPlugin
    private val activeDialogs = ConcurrentHashMap<UUID, BukkitTask>()

    fun init(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    fun cleanup() {
        activeDialogs.values.forEach { it.cancel() }
        activeDialogs.clear()
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

    @Suppress("UNCHECKED_CAST")
    fun <T : AtlasBlock> showBlockDialog(
        player: Player,
        block: T,
        registry: BlockRegistry<*>,
        displayName: (T) -> String,
        buildBody: (T) -> Component,
    ) {
        showDialog(player, block, registry) { p, b, onClose ->
            val typed = b as T
            val title = Component.text(displayName(typed))
            val body = buildBody(typed)
            val dialog = createNoticeDialog(title, body, onClose)
            p.showDialog(dialog)
        }
    }

    fun showDialog(
        player: Player,
        block: AtlasBlock,
        registry: BlockRegistry<*>,
        renderDialog: (Player, AtlasBlock, onClose: (Player) -> Unit) -> Unit,
    ) {
        activeDialogs.remove(player.uniqueId)?.cancel()

        val onClose: (Player) -> Unit = { p -> activeDialogs.remove(p.uniqueId)?.cancel() }

        renderDialog(player, block, onClose)

        val task =
            plugin.server.scheduler.runTaskTimer(
                plugin,
                Runnable {
                    if (!player.isOnline) {
                        activeDialogs.remove(player.uniqueId)?.cancel()
                        return@Runnable
                    }
                    if (player.location.distance(block.location) > 10) {
                        activeDialogs.remove(player.uniqueId)?.cancel()
                        return@Runnable
                    }
                    if (registry.getBlock(block.location) == null) {
                        activeDialogs.remove(player.uniqueId)?.cancel()
                        return@Runnable
                    }
                    renderDialog(player, block, onClose)
                },
                10L,
                10L,
            )

        activeDialogs[player.uniqueId] = task
    }
}
