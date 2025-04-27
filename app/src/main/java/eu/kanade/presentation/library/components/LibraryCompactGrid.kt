package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover
import eu.kanade.tachiyomi.ui.library.LibraryItem
import androidx.compose.ui.util.fastAny
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun LibraryCompactGrid(
    items: List<LibraryItem>,
    showTitle: Boolean,
    columns: Int,
    selection: List<LibraryManga>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    // Use LocalDensity for pixel-perfect measurements
    val density = LocalDensity.current

    // Track parent size to guarantee we never exceed it
    var parentHeight by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 15.dp)
            .onSizeChanged { parentHeight = it.height }
    ) {
        with(density) {
            // Fixed dimensions
            val horizontalSpacing = 8.dp
            val verticalSpacing = 12.dp

            // Convert parent height to DP for calculations
            val totalHeightDp = parentHeight.toDp()

            // Hard ceiling on grid height - add extra safety margin
            val gridHeight = totalHeightDp - 5.dp

            // Fixed number of rows
            val rows = 3

            // Calculate card height with fixed rows
            val cardHeight = (gridHeight - (verticalSpacing * (rows - 1))) / rows

            // Calculate pagination
            val itemsPerPage = columns * rows
            val hasSearchItem = searchQuery != null
            val totalItems = items.size + (if (hasSearchItem) 1 else 0)
            val pageCount = max(1, (totalItems + itemsPerPage - 1) / itemsPerPage)

            // Page state
            var currentPage by remember { mutableStateOf(0) }
            var pageChangeHandled by remember { mutableStateOf(false) }

            // Keep page in valid range
            LaunchedEffect(pageCount) {
                currentPage = min(currentPage, max(0, pageCount - 1))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeightDp - 5.dp) // Slight reduction to ensure no overflow
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { pageChangeHandled = false },
                            onDragEnd = { pageChangeHandled = false },
                            onHorizontalDrag = { _, dragAmount ->
                                if (!pageChangeHandled) {
                                    when {
                                        dragAmount < -50 -> {
                                            if (currentPage < pageCount - 1) {
                                                currentPage++
                                                pageChangeHandled = true
                                            }
                                        }
                                        dragAmount > 50 -> {
                                            if (currentPage > 0) {
                                                currentPage--
                                                pageChangeHandled = true
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    },
                verticalArrangement = Arrangement.Top
            ) {
                // Calculate items for current page
                val currentPageItems = mutableListOf<Any>()

                // Special handling for search item - it should be first item on first page
                if (currentPage == 0 && hasSearchItem) {
                    currentPageItems.add("SEARCH_PLACEHOLDER")
                }

                // Calculate regular item range for current page
                val regularItemsPerPage = itemsPerPage
                val startIndex = currentPage * regularItemsPerPage -
                    (if (currentPage > 0 && hasSearchItem) 1 else 0)
                val endIndex = min(startIndex + regularItemsPerPage -
                    (if (currentPage == 0 && hasSearchItem) 1 else 0),
                    items.size)

                if (startIndex < items.size) {
                    currentPageItems.addAll(items.subList(max(0, startIndex), endIndex))
                }

                // Fill remaining slots to maintain grid structure
                while (currentPageItems.size < regularItemsPerPage) {
                    currentPageItems.add("EMPTY_PLACEHOLDER")
                }

                // Grid container with full height
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                        .clipToBounds()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Create manual grid layout for perfect control
                        for (rowIndex in 0 until rows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(cardHeight),
                                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
                            ) {
                                for (colIndex in 0 until columns) {
                                    val itemIndex = rowIndex * columns + colIndex

                                    if (itemIndex < currentPageItems.size) {
                                        val item = currentPageItems[itemIndex]

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clipToBounds()
                                        ) {
                                            when (item) {
                                                is LibraryItem -> {
                                                    val manga = item.libraryManga.manga

                                                    MangaCompactGridItem(
                                                        modifier = Modifier.fillMaxSize(),
                                                        isSelected = selection.fastAny { it.id == item.libraryManga.id },
                                                        title = manga.title.takeIf { showTitle },
                                                        coverData = MangaCover(
                                                            mangaId = manga.id,
                                                            sourceId = manga.source,
                                                            isMangaFavorite = manga.favorite,
                                                            url = manga.thumbnailUrl,
                                                            lastModified = manga.coverLastModified,
                                                        ),
                                                        coverBadgeStart = {
                                                            DownloadsBadge(count = item.downloadCount)
                                                            UnreadBadge(count = item.unreadCount)
                                                        },
                                                        coverBadgeEnd = {
                                                            LanguageBadge(
                                                                isLocal = item.isLocal,
                                                                sourceLanguage = item.sourceLanguage,
                                                            )
                                                        },
                                                        onLongClick = { onLongClick(item.libraryManga) },
                                                        onClick = { onClick(item.libraryManga) },
                                                        onClickContinueReading = if (onClickContinueReading != null && item.unreadCount > 0) {
                                                            { onClickContinueReading(item.libraryManga) }
                                                        } else {
                                                            null
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Add spacing between rows (except after the last row)
                            if (rowIndex < rows - 1) {
                                Spacer(modifier = Modifier.height(verticalSpacing))
                            }
                        }
                    }
                }

                // Visual page indicator at bottom instead of counter at top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pageCount) { page ->
                        Box(
                            modifier = Modifier
                                .size(if (page == currentPage) 8.dp else 6.dp)
                                .padding(horizontal = 2.dp)
                                .clipToBounds()
                        ) {
                            // You can use your design system's indicators here
                            // This is a basic placeholder
                        }
                    }
                }
            }
        }
    }
}
