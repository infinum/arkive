@file:Suppress("MagicNumber")

package com.infinum.arkive.sample.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.theme.Amber
import com.infinum.arkive.sample.theme.Cloud
import com.infinum.arkive.sample.theme.Ink
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.Pine
import com.infinum.arkive.sample.theme.PineLight
import com.infinum.arkive.sample.theme.Sky
import com.infinum.arkive.sample.theme.Slate

private val avatarColors = listOf(Pine, Sky, Amber, PineLight)

@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val color = avatarColors[initials.hashCode().mod(avatarColors.size)]
    Box(
        modifier = modifier
            .size(size)
            .background(color = color.copy(alpha = 0.18f), shape = CircleShape)
            .border(width = 1.dp, color = color.copy(alpha = 0.4f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
fun AvatarStack(
    members: List<String>,
    modifier: Modifier = Modifier,
    extraCount: Int = 0,
) {
    Row(modifier = modifier) {
        members.forEachIndexed { index, initials ->
            Box(
                modifier = Modifier
                    .offset(x = (-10 * index).dp)
                    .background(color = Cloud, shape = CircleShape)
                    .border(width = 2.dp, color = Cloud, shape = CircleShape),
            ) {
                Avatar(initials = initials, size = 40.dp)
            }
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (-10 * members.size).dp)
                    .size(44.dp)
                    .background(color = Cloud, shape = CircleShape)
                    .border(width = 2.dp, color = Cloud, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color = Slate.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "+$extraCount", style = MaterialTheme.typography.labelSmall, color = Ink)
                }
            }
        }
    }
}

@Preview
@ArkiveComposable(name = "Avatar", group = "Avatars", tags = ["identity"])
@Composable
internal fun PreviewAvatar() {
    PeakTheme {
        Avatar(initials = "MH")
    }
}

@Preview
@ArkiveComposable(name = "Avatar Stack", group = "Avatars", tags = ["identity", "group"])
@Composable
internal fun PreviewAvatarStack() {
    PeakTheme {
        AvatarStack(members = listOf("MH", "JK", "AP"), extraCount = 5)
    }
}
