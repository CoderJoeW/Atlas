package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.fluid.block.FluidContainer
import com.coderjoe.atlas.fluid.block.FluidPipe
import com.coderjoe.atlas.fluid.block.FluidPump
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
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object FluidBlockDialog {

    private lateinit var plugin: JavaPlugin
    private val activeDialogs = ConcurrentHashMap<UUID, BukkitTask>()

    fun init(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    fun showFluidDialog(player: Player, fluidBlock: FluidBlock) {
        activeDialogs.remove(player.uniqueId)?.cancel()

        sendDialog(player, fluidBlock)

        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (!player.isOnline) {
                activeDialogs.remove(player.uniqueId)?.cancel()
                return@Runnable
            }
            if (player.location.distance(fluidBlock.location) > 10) {
                activeDialogs.remove(player.uniqueId)?.cancel()
                return@Runnable
            }
            val registry = FluidBlockRegistry.instance
            if (registry == null || registry.getFluidBlock(fluidBlock.location) == null) {
                activeDialogs.remove(player.uniqueId)?.cancel()
                return@Runnable
            }
            sendDialog(player, fluidBlock)
        }, 10L, 10L)

        activeDialogs[player.uniqueId] = task
    }

    private fun sendDialog(player: Player, fluidBlock: FluidBlock) {
        val title = Component.text(getBlockDisplayName(fluidBlock))
        val bodyText = buildFluidInfo(fluidBlock)
        val body = DialogBody.plainMessage(bodyText)

        val closeAction = DialogAction.customClick(
            DialogActionCallback { _, audience ->
                val p = audience as? Player ?: return@DialogActionCallback
                activeDialogs.remove(p.uniqueId)?.cancel()
            },
            ClickCallback.Options.builder().build()
        )

        val closeButton = ActionButton.builder(Component.text("Close"))
            .action(closeAction)
            .build()

        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(
                    DialogBase.builder(title)
                        .body(listOf(body))
                        .canCloseWithEscape(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .build()
                )
                .type(DialogType.notice(closeButton))
        }

        player.showDialog(dialog)
    }

    fun cleanup() {
        activeDialogs.values.forEach { it.cancel() }
        activeDialogs.clear()
    }

    private fun getBlockDisplayName(fluidBlock: FluidBlock): String = when (fluidBlock) {
        is FluidPump -> "Fluid Pump"
        is FluidPipe -> "Fluid Pipe (${fluidBlock.facing.name.lowercase().replaceFirstChar { it.uppercase() }})"
        is FluidContainer -> "Fluid Container (${fluidBlock.facing.name.lowercase().replaceFirstChar { it.uppercase() }})"
        else -> "Fluid Block"
    }

    private fun buildFluidInfo(fluidBlock: FluidBlock): Component {
        val fluidName = if (fluidBlock is FluidContainer && fluidBlock.storedAmount > 0) {
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

        val fluidColor = when (fluidBlock.storedFluid) {
            FluidType.WATER -> NamedTextColor.AQUA
            FluidType.LAVA -> NamedTextColor.GOLD
            FluidType.NONE -> NamedTextColor.GRAY
        }

        val statusLine = Component.text("Fluid: ")
            .color(NamedTextColor.WHITE)
            .decorate(TextDecoration.BOLD)
            .append(Component.text(fluidName).color(fluidColor).decoration(TextDecoration.BOLD, false))

        val infoLine = when (fluidBlock) {
            is FluidPump -> Component.text("Pump - extracts fluid from adjacent cauldrons (1 power/s)")
                .color(NamedTextColor.GRAY)
            is FluidPipe -> Component.text("Pipe - transports fluid in facing direction")
                .color(NamedTextColor.GRAY)
            is FluidContainer -> Component.text("Container - stores up to ${FluidContainer.MAX_CAPACITY} units of fluid")
                .color(NamedTextColor.GRAY)
            else -> Component.text("Fluid block")
                .color(NamedTextColor.GRAY)
        }

        var result = statusLine
            .append(Component.newline())
            .append(infoLine)

        if (fluidBlock is FluidPump) {
            val powerLine = if (fluidBlock.isPowered) {
                Component.text("Powered").color(NamedTextColor.GREEN)
            } else {
                Component.text("No Power").color(NamedTextColor.RED)
            }

            val statusText = when (fluidBlock.pumpStatus) {
                FluidPump.PumpStatus.IDLE -> Component.text("Idle — holding fluid").color(NamedTextColor.YELLOW)
                FluidPump.PumpStatus.EXTRACTING -> Component.text("Extracting from cauldron").color(NamedTextColor.GREEN)
                FluidPump.PumpStatus.NO_SOURCE -> Component.text("No source nearby").color(NamedTextColor.RED)
                FluidPump.PumpStatus.NO_POWER -> Component.text("Waiting for power").color(NamedTextColor.RED)
            }

            result = result
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
