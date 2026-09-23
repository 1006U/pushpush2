package com.pushpush2.ui

import com.pushpush2.game.StageRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StageLayoutPolicyTest {

    @Test
    fun everyOriginalStageKeepsControlsVisibleOnCompactScreen() {
        val contentWidth = 320
        val contentHeight = 568
        val fixedChrome = 172
        val panelPadding = 10
        val minControls = 148

        StageRepository.stages.forEach { stage ->
            val allocation = StageLayoutPolicy.allocate(
                contentWidthPx = contentWidth,
                contentHeightPx = contentHeight,
                stageWidth = stage.width,
                stageHeight = stage.height,
                fixedChromeHeightPx = fixedChrome,
                boardHorizontalInsetPx = 4,
                boardVerticalInsetPx = 4,
                controlsPanelVerticalPaddingPx = panelPadding,
                minControlsHeightPx = minControls
            )

            assertTrue(
                "Stage ${stage.number} controls were clipped: ${allocation.controlsHeightPx}",
                allocation.controlsHeightPx >= minControls
            )

            assertTrue(
                "Stage ${stage.number} board height must stay positive",
                allocation.boardHeightPx > 0
            )

            assertTrue(
                "Stage ${stage.number} total layout exceeded compact screen",
                fixedChrome +
                    allocation.boardHeightPx +
                    panelPadding +
                    allocation.controlsHeightPx <= contentHeight
            )
        }
    }

    @Test
    fun tallStageShrinksBoardBeforeControls() {
        val stage = StageRepository.get(4) // 6 x 8, one of the tallest ratios.

        val allocation = StageLayoutPolicy.allocate(
            contentWidthPx = 320,
            contentHeightPx = 568,
            stageWidth = stage.width,
            stageHeight = stage.height,
            fixedChromeHeightPx = 172,
            boardHorizontalInsetPx = 4,
            boardVerticalInsetPx = 4,
            controlsPanelVerticalPaddingPx = 10,
            minControlsHeightPx = 148
        )

        assertEquals(148, allocation.controlsHeightPx)
        assertEquals(238, allocation.boardHeightPx)
    }

    @Test
    fun wideStageLeavesMoreRoomForControls() {
        val stage = StageRepository.get(40) // 12 x 5, very wide.

        val allocation = StageLayoutPolicy.allocate(
            contentWidthPx = 360,
            contentHeightPx = 740,
            stageWidth = stage.width,
            stageHeight = stage.height,
            fixedChromeHeightPx = 172,
            boardHorizontalInsetPx = 4,
            boardVerticalInsetPx = 4,
            controlsPanelVerticalPaddingPx = 10,
            minControlsHeightPx = 148
        )

        assertTrue(allocation.controlsHeightPx > 248)
    }
}
