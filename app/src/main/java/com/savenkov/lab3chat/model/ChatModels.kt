package com.savenkov.lab3chat.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ChatMessage(
    val id: Long,
    val sender: String,
    val text: String?,
    val thumbnailUrl: String?,
    val fullImageUrl: String?
)

@Immutable
data class ChatsUiState(
    val channels: ImmutableList<String> = persistentListOf(),
    val selectedChannel: String? = null,
    val messages: ImmutableList<ChatMessage> = persistentListOf(),
    val fullScreenImageUrl: String? = null,
    val channelsLoading: Boolean = false,
    val channelsError: Boolean = false,
    val messagesLoading: Boolean = false,
    val messagesError: Boolean = false,
    val hasMoreMessages: Boolean = true
)
