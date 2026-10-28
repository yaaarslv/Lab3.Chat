package com.savenkov.lab3chat.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savenkov.lab3chat.data.BASE_URL
import com.savenkov.lab3chat.data.ChatApi
import com.savenkov.lab3chat.data.MessageDto
import com.savenkov.lab3chat.data.PAGE_SIZE
import com.savenkov.lab3chat.di
import com.savenkov.lab3chat.model.ChatMessage
import com.savenkov.lab3chat.model.ChatsUiState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.instance

@Stable
class ChatsViewModel : ViewModel() {
    private val api: ChatApi by di.instance()

    private data class Page(
        val messages: PersistentList<ChatMessage> = persistentListOf(),
        val cursor: Long = 0L,
        val hasMore: Boolean = true
    )

    private val savedPages = mutableMapOf<String, Page>()
    private val pageJobs = mutableMapOf<String, Job>()

    private val _uiState = MutableStateFlow(ChatsUiState(channelsLoading = true))
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    init {
        loadChannels()
    }

    private fun loadChannels() {
        _uiState.update { it.copy(channelsLoading = true, channelsError = false) }
        viewModelScope.launch {
            try {
                val channels = api.getChannels().distinct().toPersistentList()
                _uiState.update {
                    it.copy(channels = channels, channelsError = false)
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _uiState.update { it.copy(channelsError = true) }
            } finally {
                _uiState.update { it.copy(channelsLoading = false) }
            }
        }
    }

    fun selectChannel(channel: String) {
        if (_uiState.value.selectedChannel == channel) return

        val previousPage = savedPages[channel]
        _uiState.update {
            it.copy(
                selectedChannel = channel,
                fullScreenImageUrl = null,
                messages = previousPage?.messages ?: persistentListOf(),
                hasMoreMessages = previousPage?.hasMore ?: true,
                messagesLoading = pageJobs[channel]?.isActive == true,
                messagesError = false
            )
        }
        if (previousPage == null) loadMore(channel)
    }

    fun loadMoreSelected() {
        _uiState.value.selectedChannel?.let(::loadMore)
    }

    private fun loadMore(channel: String) {
        if (pageJobs[channel]?.isActive == true) return
        val previousPage = savedPages[channel] ?: Page()
        if (!previousPage.hasMore) return
        if (_uiState.value.selectedChannel == channel) {
            _uiState.update { it.copy(messagesLoading = true, messagesError = false) }
        }

        pageJobs[channel] = viewModelScope.launch {
            try {
                val next = api.getMessages(
                    channel = channel,
                    limit = PAGE_SIZE,
                    lastKnownId = previousPage.cursor,
                    reverse = false
                ).map { it.toChatMessage() }
                val newCursor = next.lastOrNull()?.id ?: previousPage.cursor
                val accumulated = (previousPage.messages + next)
                    .distinctBy(ChatMessage::id)
                    .toPersistentList()
                val moreAvailable = next.size == PAGE_SIZE &&
                        newCursor != previousPage.cursor &&
                        accumulated.size > previousPage.messages.size
                val updated = Page(accumulated, newCursor, moreAvailable)
                savedPages[channel] = updated
                if (_uiState.value.selectedChannel == channel) {
                    _uiState.update {
                        it.copy(
                            messages = updated.messages,
                            hasMoreMessages = updated.hasMore,
                            messagesError = false
                        )
                    }
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                if (_uiState.value.selectedChannel == channel) {
                    _uiState.update { it.copy(messagesError = true) }
                }
            } finally {
                if (_uiState.value.selectedChannel == channel) {
                    _uiState.update { it.copy(messagesLoading = false) }
                }
            }
        }
    }

    fun openImage(url: String) {
        _uiState.update { it.copy(fullScreenImageUrl = url) }
    }

    fun back() {
        _uiState.update { state ->
            when {
                state.fullScreenImageUrl != null -> state.copy(fullScreenImageUrl = null)
                state.selectedChannel != null -> state.copy(
                    selectedChannel = null,
                    messages = persistentListOf(),
                    messagesLoading = false,
                    messagesError = false
                )

                else -> state
            }
        }
    }

    private fun MessageDto.toChatMessage(): ChatMessage {
        val imagePath = data?.image?.link?.takeIf(String::isNotBlank)
        return ChatMessage(
            id = id,
            sender = from,
            text = data?.text?.text,
            thumbnailUrl = imagePath?.let { mediaUrl("thumb", it) },
            fullImageUrl = imagePath?.let { mediaUrl("img", it) }
        )
    }

    private fun mediaUrl(endpoint: String, link: String): String {
        val path = link.trimStart('/')
            .removePrefix("img/")
            .removePrefix("thumb/")
        return "${BASE_URL}$endpoint/$path"
    }
}
