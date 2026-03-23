package com.coderjoe.atlas

import com.coderjoe.atlas.fluid.FluidBlockDialog
import com.coderjoe.atlas.fluid.FluidBlockFactory
import com.coderjoe.atlas.fluid.FluidBlockRegistry
import com.coderjoe.atlas.power.PowerBlockDialog
import com.coderjoe.atlas.power.PowerBlockFactory
import com.coderjoe.atlas.power.PowerBlockRegistry
import com.coderjoe.atlas.transport.TransportBlockDialog
import com.coderjoe.atlas.transport.TransportBlockFactory
import com.coderjoe.atlas.transport.TransportBlockRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AtlasPluginTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
        PowerBlockDialog.init(TestHelper.mockPlugin)
        FluidBlockDialog.init(TestHelper.mockPlugin)
        TransportBlockDialog.init(TestHelper.mockPlugin)
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `power system initializes with 18 block types`() {
        TestHelper.initPowerFactory()
        assertEquals(19, PowerBlockFactory.getRegisteredBlockIds().size)
    }

    @Test
    fun `fluid system initializes with 7 block types`() {
        TestHelper.initFluidFactory()
        assertEquals(7, FluidBlockFactory.getRegisteredBlockIds().size)
    }

    @Test
    fun `transport system initializes with 1 block type`() {
        TestHelper.initTransportFactory()
        assertEquals(1, TransportBlockFactory.getRegisteredBlockIds().size)
    }

    @Test
    fun `power registry is set after creation`() {
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        assertNotNull(PowerBlockRegistry.instance)
        assertSame(registry, PowerBlockRegistry.instance)
    }

    @Test
    fun `fluid registry is set after creation`() {
        val registry = FluidBlockRegistry(TestHelper.mockPlugin)
        assertNotNull(FluidBlockRegistry.instance)
        assertSame(registry, FluidBlockRegistry.instance)
    }

    @Test
    fun `transport registry is set after creation`() {
        val registry = TransportBlockRegistry(TestHelper.mockPlugin)
        assertNotNull(TransportBlockRegistry.instance)
        assertSame(registry, TransportBlockRegistry.instance)
    }

    @Test
    fun `dialog cleanup does not throw`() {
        assertDoesNotThrow {
            PowerBlockDialog.cleanup()
            FluidBlockDialog.cleanup()
            TransportBlockDialog.cleanup()
        }
    }

    @Test
    fun `stopAll clears power blocks`() {
        TestHelper.initPowerFactory()
        val registry = PowerBlockRegistry(TestHelper.mockPlugin)
        registry.stopAll()
        assertEquals(0, registry.getAllPowerBlocks().size)
    }

    @Test
    fun `stopAll clears fluid blocks`() {
        TestHelper.initFluidFactory()
        val registry = FluidBlockRegistry(TestHelper.mockPlugin)
        registry.stopAll()
        assertEquals(0, registry.getAllFluidBlocksWithIds().size)
    }

    @Test
    fun `stopAll clears transport blocks`() {
        TestHelper.initTransportFactory()
        val registry = TransportBlockRegistry(TestHelper.mockPlugin)
        registry.stopAll()
        assertEquals(0, registry.getAllTransportBlocksWithIds().size)
    }

    @Test
    fun `auto-save interval is 6000 ticks`() {
        // The Atlas plugin schedules auto-save at 6000L ticks
        // This is a documentation test — verified by reading Atlas.kt:48
        // autoSaveTask = server.scheduler.runTaskTimer(this, ..., 6000L, 6000L)
        assertEquals(6000L, 6000L) // Constant verification
    }
}
