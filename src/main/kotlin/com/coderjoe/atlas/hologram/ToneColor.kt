package com.coderjoe.atlas.hologram

import com.coderjoe.atlas.block.Tone
import net.kyori.adventure.text.format.NamedTextColor

/** The colour a readout paints a line of this tone in. */
val Tone.color: NamedTextColor
    get() =
        when (this) {
            Tone.NEUTRAL -> NamedTextColor.GRAY
            Tone.GOOD -> NamedTextColor.GREEN
            Tone.WARNING -> NamedTextColor.YELLOW
            Tone.FAULT -> NamedTextColor.RED
        }
