package com.coderjoe.atlas.core

import com.coderjoe.atlas.TestHelper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AtlasWrenchTest {
    @BeforeEach
    fun setup() {
        TestHelper.setup()
    }

    @AfterEach
    fun teardown() {
        TestHelper.teardown()
    }

    @Test
    fun `an empty hand is not a wrench`() {
        // the gate that keeps a bare-handed right-click from opening any dialog
        assertFalse(AtlasWrench.isWrench(null, TestHelper.mockPlugin))
    }
}
