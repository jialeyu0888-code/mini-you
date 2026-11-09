package com.miniyou.app.ui.diary

import android.view.WindowManager.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import com.miniyou.app.R
import java.io.File
import kotlinx.coroutines.launch

internal data class DiaryPhotoSelection(val photos: List<String>, val index: Int)

@Composable
internal fun DiaryPhotoImage(
    photo: String,
    description: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    val model = remember(photo) {
        when (photo) {
            "preset:sunset" -> R.drawable.diary_card_polaroid_sunset
            "preset:cat" -> R.drawable.diary_card_cat
            else -> File(photo)
        }
    }
    AsyncImage(
        model = model,
        contentDescription = description,
        contentScale = contentScale,
        error = rememberVectorPainter(Icons.Default.BrokenImage),
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DiaryPhotoViewer(selection: DiaryPhotoSelection, onDismiss: () -> Unit) {
    val photos = selection.photos
    val pager = rememberPagerState(initialPage = selection.index) { photos.size }
    val thumbnails = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentPage = pager.currentPage

    LaunchedEffect(pager.settledPage) {
        val page = pager.settledPage
        val layout = thumbnails.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.index == page }
        if (item == null || item.offset < layout.viewportStartOffset || item.offset + item.size > layout.viewportEndOffset) {
            thumbnails.animateScrollToItem(page)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        // Use the dialog window's actual bounds, which account for the system bars.
        SideEffect { window.setLayout(MATCH_PARENT, MATCH_PARENT) }
        Column(
            Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text("${currentPage + 1} / ${photos.size}", color = Color.White, modifier = Modifier.align(Alignment.Center))
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Default.Close, contentDescription = "关闭照片预览", tint = Color.White)
                }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                userScrollEnabled = photos.size > 1,
            ) { page ->
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                val transform = rememberTransformableState { zoom, pan, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offset = if (scale == 1f) Offset.Zero else offset + pan
                }
                Box(Modifier.fillMaxSize().clipToBounds().transformable(transform, canPan = { scale > 1f })) {
                    DiaryPhotoImage(
                        photo = photos[page],
                        description = "大图 ${page + 1}/${photos.size}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    )
                }
            }

            LazyRow(
                state = thumbnails,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(photos.size) { index ->
                    val selected = index == currentPage
                    val shape = RoundedCornerShape(8.dp)
                    DiaryPhotoImage(
                        photo = photos[index],
                        description = "缩略图 ${index + 1}/${photos.size}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(shape)
                            .border(2.dp, if (selected) Color(0xFFFFA6AC) else Color.Transparent, shape)
                            .selectable(selected = selected, role = Role.Button) {
                                scope.launch { pager.animateScrollToPage(index) }
                            },
                    )
                }
            }
        }
    }
}
