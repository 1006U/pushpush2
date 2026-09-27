package com.pushpush2.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StageLayoutPolicyTest {

    @Test
    fun compactScreenKeepsControlsVisible() {
        val allocation = StageLayoutPolicy.allocate(
            contentWidthPx = 320,
            contentHeightPx = 568,
            fixedChromeHeightPx = 172,
            boardHorizontalInsetPx = 4,
            boardVerticalInsetPx = 4,
            controlsPanelVerticalPaddingPx = 10,
            minControlsHeightPx = 148
        )

        assertEquals(238, allocation.boardHeightPx)
        assertEquals(148, allocation.controlsHeightPx)
    }

    @Test
    fun normalPortraitScreenUsesSquareBoardViewport() {
        val allocation = StageLayoutPolicy.allocate(
            contentWidthPx = 360,
            contentHeightPx = 740,
            fixedChromeHeightPx = 172,
            boardHorizontalInsetPx = 4,
            boardVerticalInsetPx = 4,
            controlsPanelVerticalPaddingPx = 10,
            minControlsHeightPx = 148
        )

        assertEquals(360, allocation.boardHeightPx)
        assertEquals(198, allocation.controlsHeightPx)
    }

    @Test
    fun allocationDependsOnScreenNotStageShape() {
        val first = StageLayoutPolicy.allocate(
            contentWidthPx = 360,
            contentHeightPx = 740,
            fixedChromeHeightPx = 172,
            boardHorizontalInsetPx = 4,
            boardVerticalInsetPx = 4,
            controlsPanelVerticalPaddingPx = 10,
            minControlsHeightPx = 148
        )

        val second = StageLayoutPolicy.allocate(
            contentWidthPx = 360,
            contentHeightPx = 740,
            fixedChromeHeightPx = 172,
            boardHorizontalInsetPx = 4,
            boardVerticalInsetPx = 4,
            controlsPanelVerticalPaddingPx = 10,
            minControlsHeightPx = 148
        )

        assertEquals(first, second)
        assertTrue(first.boardHeightPx > 0)
        assertTrue(first.controlsHeightPx >= 148)
    }
}
