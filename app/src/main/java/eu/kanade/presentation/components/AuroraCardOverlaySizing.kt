package eu.kanade.presentation.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class AuroraOverlayScaleTier {
    XLarge,
    Large,
    Medium,
    Small,
}

internal data class AuroraCardOverlaySpec(
    val buttonSizeDp: Dp,
    val buttonIconSizeDp: Dp,
    val progressTextSizeSp: TextUnit,
    val footerHorizontalPaddingDp: Dp,
    val footerVerticalPaddingDp: Dp,
    val progressTextEndInsetDp: Dp,
)

internal fun resolveAuroraOverlayScaleTier(
    gridColumns: Int?,
    cardWidthDp: Float?,
): AuroraOverlayScaleTier {
    val widthDp = cardWidthDp
    if (widthDp != null && widthDp >= 200f) return AuroraOverlayScaleTier.XLarge

    val fixedColumns = gridColumns?.takeIf { it > 0 }
    if (fixedColumns != null) {
        return when {
            fixedColumns <= 3 -> AuroraOverlayScaleTier.Large
            fixedColumns == 4 -> AuroraOverlayScaleTier.Medium
            else -> AuroraOverlayScaleTier.Small
        }
    }

    return when {
        widthDp == null -> AuroraOverlayScaleTier.Large
        widthDp >= 112f -> AuroraOverlayScaleTier.Large
        widthDp >= 92f -> AuroraOverlayScaleTier.Medium
        else -> AuroraOverlayScaleTier.Small
    }
}

internal fun resolveAuroraCardOverlaySpec(
    gridColumns: Int?,
    cardWidthDp: Float?,
): AuroraCardOverlaySpec {
    return when (resolveAuroraOverlayScaleTier(gridColumns, cardWidthDp)) {
        AuroraOverlayScaleTier.XLarge -> AuroraCardOverlaySpec(
            buttonSizeDp = 36.dp,
            buttonIconSizeDp = 22.dp,
            progressTextSizeSp = 14.sp,
            footerHorizontalPaddingDp = 12.dp,
            footerVerticalPaddingDp = 11.dp,
            progressTextEndInsetDp = 4.dp,
        )
        AuroraOverlayScaleTier.Large -> AuroraCardOverlaySpec(
            buttonSizeDp = 30.dp,
            buttonIconSizeDp = 18.dp,
            progressTextSizeSp = 13.sp,
            footerHorizontalPaddingDp = 10.dp,
            footerVerticalPaddingDp = 9.dp,
            progressTextEndInsetDp = 3.dp,
        )
        AuroraOverlayScaleTier.Medium -> AuroraCardOverlaySpec(
            buttonSizeDp = 27.dp,
            buttonIconSizeDp = 16.dp,
            progressTextSizeSp = 12.sp,
            footerHorizontalPaddingDp = 8.dp,
            footerVerticalPaddingDp = 7.dp,
            progressTextEndInsetDp = 2.dp,
        )
        AuroraOverlayScaleTier.Small -> AuroraCardOverlaySpec(
            buttonSizeDp = 24.dp,
            buttonIconSizeDp = 14.dp,
            progressTextSizeSp = 11.sp,
            footerHorizontalPaddingDp = 6.dp,
            footerVerticalPaddingDp = 6.dp,
            progressTextEndInsetDp = 1.dp,
        )
    }
}
