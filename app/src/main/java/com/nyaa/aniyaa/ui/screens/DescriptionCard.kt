package com.nyaa.aniyaa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nyaa.aniyaa.util.DescriptionBlock
import com.nyaa.aniyaa.util.DescriptionFormatter
import com.nyaa.aniyaa.util.DescriptionImage
import com.nyaa.aniyaa.util.isSafeHttpUrl
import com.nyaa.aniyaa.util.openHttpUrl

@Composable
fun DescriptionCard(
    markdown: String,
    onCatalogLink: (String) -> Boolean
) {
    val blocks = remember(markdown) { DescriptionFormatter.blocks(markdown) }
    if (blocks.isEmpty()) return
    val imageCount = blocks.sumOf { block ->
        if (block is DescriptionBlock.Gallery) block.images.size else 0
    }
    val longDescription = markdown.length > 2_400 || imageCount > 8
    var expanded by remember(markdown) { mutableStateOf(!longDescription) }
    val visible = remember(blocks, expanded) {
        if (expanded) blocks else previewBlocks(blocks)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            visible.forEach { block ->
                when (block) {
                    is DescriptionBlock.Markdown -> MarkdownContent(
                        markdown = block.text,
                        onCatalogLink = onCatalogLink,
                        compact = false
                    )
                    is DescriptionBlock.Gallery -> ImageGallery(
                        images = if (expanded) block.images else block.images.take(4),
                        extraCount = if (expanded) 0 else (block.images.size - 4).coerceAtLeast(0)
                    )
                    is DescriptionBlock.Table -> DescriptionTable(block)
                    is DescriptionBlock.Code -> DescriptionCode(block.body)
                }
            }
            if (longDescription) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(if (expanded) "Show less" else "Show full description")
                }
            }
        }
    }
}

@Composable
private fun ImageGallery(images: List<DescriptionImage>, extraCount: Int) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        images.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { image ->
                    DescriptionImageFrame(
                        image = image,
                        modifier = Modifier.weight(1f),
                        onClick = { openHttpUrl(context, image.url) }
                    )
                }
                if (row.size == 1 && images.size > 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        if (extraCount > 0) {
            Text(
                text = "+$extraCount more screenshots",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun DescriptionImageFrame(
    image: DescriptionImage,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (!isSafeHttpUrl(image.url)) return
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(14.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.url)
                .crossfade(true)
                .build(),
            contentDescription = image.alt.ifBlank { "Description image" },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp, max = 240.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        )
    }
}

@Composable
private fun DescriptionTable(table: DescriptionBlock.Table) {
    val columns = table.headers.size.coerceAtLeast(1)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(4.dp)
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
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        repeat(columns) { index ->
            Text(
                text = values.getOrElse(index) { "" },
                style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                color = if (header) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(128.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun DescriptionCode(body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(14.dp)
        )
    }
}

private fun previewBlocks(blocks: List<DescriptionBlock>): List<DescriptionBlock> {
    val preview = ArrayList<DescriptionBlock>()
    var images = 0
    for (block in blocks) {
        when (block) {
            is DescriptionBlock.Gallery -> {
                val remaining = (4 - images).coerceAtLeast(0)
                if (remaining == 0) break
                preview += block.copy(images = block.images.take(remaining))
                images += remaining
            }
            is DescriptionBlock.Markdown -> {
                preview += if (block.text.length > 900) {
                    DescriptionBlock.Markdown(block.text.take(900).trimEnd() + "…")
                } else {
                    block
                }
            }
            else -> preview += block
        }
        if (preview.size >= 4 && images >= 4) break
    }
    return preview.ifEmpty { blocks.take(1) }
}
