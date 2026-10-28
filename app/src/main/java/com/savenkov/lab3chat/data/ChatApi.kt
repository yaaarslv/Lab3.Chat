package com.savenkov.lab3chat.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @GET("channels")
    suspend fun getChannels(): List<String>

    @GET("channel/{channel}")
    suspend fun getMessages(
        @Path("channel") channel: String,
        @Query("limit") limit: Int,
        @Query("lastKnownId") lastKnownId: Long,
        @Query("reverse") reverse: Boolean
    ): List<MessageDto>
}

@Serializable
data class MessageDto(
    val id: Long,
    val from: String,
    val data: MessageContentDto? = null
)

@Serializable
data class MessageContentDto(
    @SerialName("Text") val text: TextDto? = null,
    @SerialName("Image") val image: ImageDto? = null
)

@Serializable
data class TextDto(val text: String)

@Serializable
data class ImageDto(val link: String = "")
