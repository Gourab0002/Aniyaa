package com.nyaa.aniyaa.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nyaa.aniyaa.util.DescriptionBlock
import com.nyaa.aniyaa.util.DescriptionFormatter
import com.nyaa.aniyaa.util.DescriptionImage
import com.nyaa.aniyaa.util.isSafeHttpUrl

@Composable
fun DescriptionCard(
    markdown: String,
    onCatalogLink: (String) -> Boolean
) {
    val blocks = remember(markdown) { DescriptionFormatter.blocks(markdown) }
    if (blocks.isEmpty()) return
    val allImages = remember(blocks) {
        blocks.filterIsInstance<DescriptionBlock.Gallery>().flatMap { it.images }.distinctBy { it.url }
    }
    val longDescription = markdown.length > 2_400
    var expanded by remember(markdown) { mutableStateOf(!longDescription) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    val visible = remember(blocks, expanded) {
        if (expanded) blocks else previewBlocks(blocks)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            visible.forEach { block ->
                when (block) {
                    is DescriptionBlock.Markdown -> MarkdownContent(
                        markdown = block.text,
                        onCatalogLink = onCatalogLink,
                        compact = true,
                        renderInlineImages = false,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    is DescriptionBlock.Gallery -> ImageGallery(
                        images = block.images,
                        onOpen = { image ->
                            viewerIndex = allImages.indexOfFirst { it.url == image.url }.takeIf { it >= 0 } ?: 0
                        }
                    )
                    is DescriptionBlock.Table -> DescriptionTable(block)
                    is DescriptionBlock.Code -> DescriptionCode(block.body)
                }
            }
            if (longDescription) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (expanded) "Show less" else "Show full description",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
    val startIndex = viewerIndex
    if (startIndex != null && allImages.isNotEmpty()) {
        ImageViewerDialog(
            images = allImages,
            startIndex = startIndex.coerceIn(allImages.indices),
            onDismiss = { viewerIndex = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageGallery(
    images: List<DescriptionImage>,
    onOpen: (DescriptionImage) -> Unit
) {
    var hidden by remember(images) { mutableStateOf(emptySet<String>()) }
    val visible = images.filter { it.url !in hidden && isSafeHttpUrl(it.url) }
    if (visible.isEmpty()) return
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        visible.forEach { image ->
            DescriptionImageThumb(
                image = image,
                onClick = { onOpen(image) },
                onGone = { hidden = hidden + image.url }
            )
        }
    }
}

@Composable
private fun DescriptionImageThumb(
    image: DescriptionImage,
    onClick: () -> Unit,
    onGone: () -> Unit
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(image.url)
            .size(256)
            .crossfade(true)
            .build(),
        contentDescription = image.alt.ifBlank { "Description image" },
        contentScale = ContentScale.Crop,
        onError = { onGone() },
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageViewerDialog(
    images: List<DescriptionImage>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { images.size }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val image = images[page]
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = image.alt.ifBlank { "Image ${page + 1}" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            if (images.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${images.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun DescriptionTable(table: DescriptionBlock.Table) {
    val columns = table.headers.size.coerceAtLeast(1)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(2.dp)
        ) {
            TableRow(values = table.headers, header = true, columns = columns)
            table.rows.forEachIndexed { index, row ->
                TableRow(values = row, header = false, columns = columns, striped = index % 2 == 1)
            }
        }
    }
}

@Composable
private fun TableRow(values: List<String>, header: Boolean, columns: Int, striped: Boolean = false) {
    val background = when {
        header -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        striped -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(4.dp))
            .padding(horizontal = 2.dp)
    ) {
        repeat(columns) { index ->
            Text(
                text = values.getOrElse(index) { "" },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                color = if (header) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(104.dp)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DescriptionCode(body: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Text(
            text = body,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

private fun previewBlocks(blocks: List<DescriptionBlock>): List<DescriptionBlock> {
    val preview = ArrayList<DescriptionBlock>()
    for (block in blocks) {
        when (block) {
            is DescriptionBlock.Gallery -> {
                val thumbs = if (block.images.size > 8) block.images.take(8) else block.images
                preview += DescriptionBlock.Gallery(thumbs)
            }
            is DescriptionBlock.Markdown -> {
                preview += if (block.text.length > 1_200) {
                    DescriptionBlock.Markdown(block.text.take(1_200).trimEnd() + "…")
                } else {
                    block
                }
            }
            else -> preview += block
        }
        if (preview.size >= 8) break
    }
    return preview.ifEmpty { blocks.take(1) }
}
