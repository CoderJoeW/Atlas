package com.coderjoe.atlas.block

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
