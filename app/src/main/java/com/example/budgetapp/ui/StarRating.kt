package com.example.budgetapp.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val MAX_STARS = 5

/** Warm gold that reads on both light and dark surfaces. */
private val StarColor = Color(0xFFF5A623)

/**
 * Editable 1..5 star rating. Tapping the star that is already selected clears it
 * back to unrated, so a rating is always optional.
 */
@Composable
fun StarRatingInput(
    rating: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (star in 1..MAX_STARS) {
            val filled = rating != null && star <= rating
            IconButton(
                onClick = { onChange(if (rating == star) null else star) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "$star star" + if (star == 1) "" else "s",
                    tint = if (filled) StarColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

/** Compact read-only stars. Shows nothing when [rating] is null. */
@Composable
fun StarRatingDisplay(
    rating: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    emptyTint: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    if (rating == null) return
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Rated $rating out of $MAX_STARS"
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (star in 1..MAX_STARS) {
            Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (star <= rating) StarColor else emptyTint,
                modifier = Modifier.size(size),
            )
        }
    }
}
