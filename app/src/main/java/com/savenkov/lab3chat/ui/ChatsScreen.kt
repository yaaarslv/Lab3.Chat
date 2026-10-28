package com.savenkov.lab3chat.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.savenkov.lab3chat.R
import com.savenkov.lab3chat.model.ChatMessage
import com.savenkov.lab3chat.model.ChatsUiState

@Composable
fun ChatsApp(viewModel: ChatsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    BackHandler(enabled = state.fullScreenImageUrl != null || state.selectedChannel != null) {
        viewModel.back()
    }

    val fullImage = state.fullScreenImageUrl
    if (fullImage != null) {
        FullScreenImage(imageUrl = fullImage, onClose = viewModel::back)
    } else {
        Scaffold { innerPadding ->
            if (landscape) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ChannelsPanel(
                        state = state,
                        onSelect = viewModel::selectChannel,
                        modifier = Modifier.weight(0.38f)
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Box(Modifier.weight(0.62f)) {
                        if (state.selectedChannel == null) {
                            CenteredLabel(stringResource(R.string.select_chat))
                        } else {
                            MessagesPanel(
                                state = state,
                                onBack = viewModel::back,
                                showBack = false,
                                onLoadMore = viewModel::loadMoreSelected,
                                onImageClick = viewModel::openImage
                            )
                        }
                    }
                }
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (state.selectedChannel == null) {
                        ChannelsPanel(
                            state = state,
                            onSelect = viewModel::selectChannel,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MessagesPanel(
                            state = state,
                            onBack = viewModel::back,
                            showBack = true,
                            onLoadMore = viewModel::loadMoreSelected,
                            onImageClick = viewModel::openImage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelsPanel(
    state: ChatsUiState,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.chats_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        HorizontalDivider()
        when {
            state.channelsLoading && state.channels.isEmpty() -> LoadingIndicator()
            state.channelsError && state.channels.isEmpty() ->
                CenteredLabel(stringResource(R.string.network_error))

            state.channels.isEmpty() -> CenteredLabel(stringResource(R.string.no_chats))
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.channels, key = { it }) { channel ->
                    val chosen = channel == state.selectedChannel
                    Surface(
                        color = if (chosen) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(channel) }
                    ) {
                        Text(
                            text = channel,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MessagesPanel(
    state: ChatsUiState,
    onBack: () -> Unit,
    showBack: Boolean,
    onLoadMore: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back_to_chats)) }
            }
            Text(
                text = state.selectedChannel.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        HorizontalDivider()
        when {
            state.messagesLoading && state.messages.isEmpty() -> LoadingIndicator()
            state.messagesError && state.messages.isEmpty() ->
                CenteredLabel(stringResource(R.string.network_error))

            state.messages.isEmpty() -> CenteredLabel(stringResource(R.string.no_messages))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageRow(message, onImageClick)
                    HorizontalDivider()
                }
                if (state.messagesLoading || state.messagesError || state.hasMoreMessages) {
                    item(key = "page_footer") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                state.messagesLoading -> CircularProgressIndicator()
                                state.messagesError -> Text(stringResource(R.string.network_error))
                                else -> Button(onClick = onLoadMore) {
                                    Text(stringResource(R.string.load_more))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(message: ChatMessage, onImageClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = message.sender,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        val text = message.text
        if (text != null) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
        val thumbnail = message.thumbnailUrl
        val fullImage = message.fullImageUrl
        if (thumbnail != null && fullImage != null) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable { onImageClick(fullImage) }
            )
        }
    }
}

@Composable
private fun FullScreenImage(imageUrl: String, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        TextButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(8.dp)
        ) {
            Text(stringResource(R.string.close_image), color = Color.White)
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredLabel(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
