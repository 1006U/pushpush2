package com.pushpush2.ui

import kotlin.math.ceil
import kotlin.math.min

/**
 * Pure layout math for keeping the stage and bottom controls visible together.
 *
 * The stage may vary from wide to tall across the original 66 maps. This
 * allocator caps the stage height whenever necessary so a minimum touch-control
 * area is always reserved.
 */
internal object StageLayoutPolicy {

    data class Allocation(
        val boardHeightPx: Int,
        val controlsHeightPx: Int
    )

    fun allocate(
        contentWidthPx: Int,
        contentHeightPx: Int,
        stageWidth: Int,
        stageHeight: Int,
        fixedChromeHeightPx: Int,
        boardHorizontalInsetPx: Int,
        boardVerticalInsetPx: Int,
        controlsPanelVerticalPaddingPx: Int,
        minControlsHeightPx: Int
    ): Allocation {
        require(contentWidthPx > 0)
        require(contentHeightPx > 0)
        require(stageWidth > 0)
        require(stageHeight > 0)

        val usableBoardWidth =
            (contentWidthPx - boardHorizontalInsetPx * 2)
                .coerceAtLeast(1)

        val desiredCell =
            usableBoardWidth.toDouble() / stageWidth.toDouble()

        val desiredBoardHeight =
            ceil(
                desiredCell * stageHeight +
                    boardVerticalInsetPx * 2
            ).toInt()

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
