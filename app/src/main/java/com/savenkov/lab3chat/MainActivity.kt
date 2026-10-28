package com.savenkov.lab3chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.savenkov.lab3chat.ui.ChatsApp
import com.savenkov.lab3chat.ui.ChatsViewModel
import com.savenkov.lab3chat.ui.theme.Lab3ChatTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ChatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Lab3ChatTheme {
                ChatsApp(viewModel)
            }
        }
    }
}
