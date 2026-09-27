package com.pushpush2.ui

import kotlin.math.min

/**
 * Pure layout math for keeping the game board and bottom controls at a stable
 * ratio on a given device.
 *
 * The original 66 stages have very different row/column counts. The visible
 * GameView height must not follow those stage dimensions, otherwise the keypad
 * jumps up and down whenever the stage changes. Instead the board viewport is
 * square whenever the screen has enough height, and only shrinks when needed
 * to preserve the minimum control area.
 */
internal object StageLayoutPolicy {

    data class Allocation(
        val boardHeightPx: Int,
        val controlsHeightPx: Int
    )

    fun allocate(
        contentWidthPx: Int,
        contentHeightPx: Int,
        fixedChromeHeightPx: Int,
        boardHorizontalInsetPx: Int,
        boardVerticalInsetPx: Int,
        controlsPanelVerticalPaddingPx: Int,
        minControlsHeightPx: Int
    ): Allocation {
        require(contentWidthPx > 0)
        require(contentHeightPx > 0)

        val usableBoardWidth =
            (contentWidthPx - boardHorizontalInsetPx * 2)
                .coerceAtLeast(1)

        // Keep the board viewport at a fixed 1:1 ratio on every stage.
        val desiredBoardHeight =
            usableBoardWidth + boardVerticalInsetPx * 2

        val availableForBoardAndControls =
            (contentHeightPx - fixedChromeHeightPx)
                .coerceAtLeast(1)

        val maxBoardHeight =
            (
                availableForBoardAndControls -
                    controlsPanelVerticalPaddingPx -
                    minControlsHeightPx
                ).coerceAtLeast(1)

        val boardHeight =
            min(desiredBoardHeight, maxBoardHeight)
                .coerceAtLeast(1)

        val controlsHeight =
            (
                availableForBoardAndControls -
                    boardHeight -
                    controlsPanelVerticalPaddingPx
                ).coerceAtLeast(1)

        return Allocation(
            boardHeightPx = boardHeight,
            controlsHeightPx = controlsHeight
        )
    }
}
