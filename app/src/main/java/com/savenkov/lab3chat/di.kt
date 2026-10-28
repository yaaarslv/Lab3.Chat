package com.savenkov.lab3chat

import com.savenkov.lab3chat.data.BASE_URL
import com.savenkov.lab3chat.data.ChatApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val di = DI {
    bindSingleton {
        Json { ignoreUnknownKeys = true }
    }

    bindSingleton {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(instance<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }

    bindSingleton {
        instance<Retrofit>().create(ChatApi::class.java)
    }
}
