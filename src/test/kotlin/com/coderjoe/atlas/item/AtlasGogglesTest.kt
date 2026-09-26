package com.coderjoe.atlas.item

import com.coderjoe.atlas.testing.MockServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AtlasGogglesTest {
    @BeforeEach
    fun setup() {
        MockServer.setup()
    }

    @AfterEach
    fun teardown() {
        MockServer.teardown()
    }

    @Test
    fun `an empty slot is not goggles`() {
        assertFalse(AtlasGoggles.isGoggles(null, MockServer.plugin))
    }
}
