package com.coderjoe.atlas.fluid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FluidTypeTest {
    @Test
    fun `all enum values exist`() {
        val values = FluidType.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(FluidType.WATER))
        assertTrue(values.contains(FluidType.LAVA))
        assertTrue(values.contains(FluidType.EXPERIENCE))
        assertTrue(values.contains(FluidType.NONE))
    }

    @Test
    fun `valueOf works for WATER`() {
        assertEquals(FluidType.WATER, FluidType.valueOf("WATER"))
    }

    @Test
    fun `valueOf works for LAVA`() {
        assertEquals(FluidType.LAVA, FluidType.valueOf("LAVA"))
    }

    @Test
    fun `valueOf works for NONE`() {
        assertEquals(FluidType.NONE, FluidType.valueOf("NONE"))
    }
}
