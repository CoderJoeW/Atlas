package com.coderjoe.atlas.fluid

import com.coderjoe.atlas.core.AtlasBlockDialog
import com.coderjoe.atlas.core.BlockRegistry
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

object FluidBlockDialog {

    fun init(plugin: JavaPlugin) {
        AtlasBlockDialog.init(plugin)
    }

    fun showFluidDialog(player: Player, fluidBlock: FluidBlock, registry: BlockRegistry<*>) {
        AtlasBlockDialog.showDialog(player, fluidBlock, registry) { p, block, onClose ->
            sendDialog(p, block as FluidBlock, onClose)
        }
    }

    fun cleanup() {
        AtlasBlockDialog.cleanup()
    }

    private fun sendDialog(player: Player, fluidBlock: FluidBlock, onClose: (Player) -> Unit) {
        val title = Component.text(getBlockDisplayName(fluidBlock))
        val bodyText = buildFluidInfo(fluidBlock)
        val body = DialogBody.plainMessage(bodyText)

        val closeAction = DialogAction.customClick(
            DialogActionCallback { _, audience ->
                val p = audience as? Player ?: return@DialogActionCallback
                onClose(p)
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
