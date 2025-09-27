package com.cm.naviconnector.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cm.naviconnector.R
import com.cm.naviconnector.feature.control.PlaylistItem
import com.cm.naviconnector.ui.theme.LightBlueish
import com.cm.naviconnector.ui.theme.LightPurple

@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.nabi_jjong),
            contentDescription = "NABI&JJONG Logo"
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CircleButton(
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    backgroundColor: Color = Color.White,
    enabled: Boolean = true
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(60.dp)
            .shadow(
                elevation = 6.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                enabled = enabled
            )
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RectangleButton(
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .size(60.dp)
            .shadow(
                elevation = 6.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(Color.White)
            .combinedClickable(
                onClick = onClick,
                enabled = enabled
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null
        )
    }
}

@Composable
fun PlaylistPanel(
    modifier: Modifier = Modifier,
    playlist: List<PlaylistItem>,
    selectedFileName: String?,
    onItemClick: (PlaylistItem) -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(148.dp)
            .shadow(
                elevation = 6.dp,
                shape = shape,
                clip = false
            )
            .background(Color.White, shape)
    ) {
        if (playlist.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "PLAY LIST",
                    fontSize = 16.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(vertical = 8.dp)) {
                items(playlist, key = { it.fileName }) {
                    PlaylistItem(
                        item = it,
                        isSelected = it.fileName == selectedFileName,
                        onClick = { onItemClick(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistItem(
    item: PlaylistItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color.LightGray else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.fileName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        LightBlueish,
                        LightPurple
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun Label(
    modifier: Modifier = Modifier,
    text: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clickable(onClick = onClick)
            .shadow(
                elevation = 6.dp,
                shape = shape,
                clip = false
            ),
        shape = shape,
        color = Color.White,
        border = BorderStroke(1.dp, tint)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            text = text,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}