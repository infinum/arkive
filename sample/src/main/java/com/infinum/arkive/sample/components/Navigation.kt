@file:Suppress("MagicNumber")

package com.infinum.arkive.sample.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.infinum.arkive.annotations.ArkiveComposable
import com.infinum.arkive.sample.theme.Cloud
import com.infinum.arkive.sample.theme.Ink
import com.infinum.arkive.sample.theme.Moss
import com.infinum.arkive.sample.theme.PeakTheme
import com.infinum.arkive.sample.theme.PineDark
import com.infinum.arkive.sample.theme.Slate

data class BottomNavItem(val icon: ImageVector, val label: String)

val defaultNavItems = listOf(
    BottomNavItem(Icons.Filled.Home, "Home"),
    BottomNavItem(Icons.Filled.Search, "Explore"),
    BottomNavItem(Icons.Filled.Star, "Goals"),
    BottomNavItem(Icons.Filled.Person, "Profile"),
)

@Composable
fun PeakTopBar(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(390.dp)
            .height(60.dp)
            .background(Cloud)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        )
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun PeakBottomBar(
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem> = defaultNavItems,
) {
    Row(
        modifier = modifier
            .width(390.dp)
            .background(Cloud)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEachIndexed { index, item ->
            BottomBarItem(item = item, selected = index == selectedIndex)
        }
    }
}

@Composable
private fun BottomBarItem(item: BottomNavItem, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(
                    color = if (selected) Moss else Cloud,
                    shape = CircleShape,
                )
                .padding(horizontal = 18.dp, vertical = 5.dp),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (selected) PineDark else Slate,
                modifier = Modifier.size(22.dp),
            )
        }
        Box(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) PineDark else Slate,
        )
    }
}

@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(color = Moss, shape = CircleShape)
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .background(
                        color = if (selected) Cloud else Moss,
                        shape = CircleShape,
                    )
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Ink else Slate,
                )
            }
        }
    }
}

@Preview
@ArkiveComposable(name = "Top Bar", group = "Navigation", tags = ["bar"])
@Composable
internal fun PreviewPeakTopBar() {
    PeakTheme {
        PeakTopBar(title = "Trail details")
    }
}

@Preview
@ArkiveComposable(name = "Bottom Bar", group = "Navigation", tags = ["bar"])
@Composable
internal fun PreviewPeakBottomBar() {
    PeakTheme {
        PeakBottomBar(selectedIndex = 0)
    }
}

@Preview
@ArkiveComposable(name = "Segmented Tabs", group = "Navigation", tags = ["tabs"])
@Composable
internal fun PreviewSegmentedTabs() {
    PeakTheme {
        SegmentedTabs(options = listOf("Day", "Week", "Month"), selectedIndex = 1)
    }
}
