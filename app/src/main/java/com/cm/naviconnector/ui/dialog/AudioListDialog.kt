package com.cm.naviconnector.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.paging.compose.LazyPagingItems
import com.cm.naviconnector.feature.audio.AudioFile
import com.cm.naviconnector.util.formatDuration

@Composable
fun AudioListDialog(
    audioFiles: LazyPagingItems<AudioFile>,
    onAudioFileClick: (AudioFile) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "오디오 파일",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (audioFiles.itemCount == 0) {
                    NoAudioFileView()
                } else {
                    AudioFileListView(audioFiles = audioFiles, onAudioFileClick = onAudioFileClick)
                }
            }
        }
    }
}

@Composable
fun NoAudioFileView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "오디오 파일이 없습니다",
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ColumnScope.AudioFileListView(
    audioFiles: LazyPagingItems<AudioFile>,
    onAudioFileClick: (AudioFile) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .drawWithContent {
                drawContent()

                val fadeHeight = 30.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        endY = fadeHeight
                    ),
                    size = this.size.copy(height = fadeHeight)
                )

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.White),
                        startY = this.size.height - fadeHeight,
                        endY = this.size.height
                    ),
                    topLeft = Offset(
                        x = 0f,
                        y = this.size.height - fadeHeight
                    ),
                    size = this.size.copy(height = fadeHeight)
                )
            },
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(
            count = audioFiles.itemCount,
            key = { index -> audioFiles[index]?.uri.toString() }
        ) { index ->
            val audioFile = audioFiles[index]
            if (audioFile != null) {
                AudioFileItem(
                    audioFile = audioFile,
                    onAudioFileClick = { onAudioFileClick(audioFile) })
                if (index < audioFiles.itemCount - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun AudioFileItem(audioFile: AudioFile, onAudioFileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAudioFileClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audioFile.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDuration(audioFile.duration),
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}