package com.coderjoe.atlas.block

import com.coderjoe.atlas.block.deposit.Purity

data class Inspection(
    val gauges: List<Gauge> = emptyList(),
    val lines: List<StatusLine> = emptyList(),
)

data class Gauge(
    val label: String,
    val current: Int,
    val max: Int,
)

data class StatusLine(
    val text: String,
    val tone: Tone = Tone.NEUTRAL,
)

enum class Tone {
    NEUTRAL,
    GOOD,
    WARNING,
    FAULT,
}

/** How a deposit of this purity reads on a readout: short of Normal is a warning, past it is good. */
val Purity.tone: Tone
    get() =
        when (this) {
            Purity.BARREN -> Tone.FAULT
            Purity.POOR -> Tone.WARNING
            Purity.NORMAL -> Tone.NEUTRAL
            Purity.RICH, Purity.PURE -> Tone.GOOD
        }
