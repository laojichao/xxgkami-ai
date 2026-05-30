package com.xxgkami.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xxgkami.android.data.TokenStore
import com.xxgkami.android.ui.theme.XXGKamiTheme
import com.xxgkami.android.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenStore.init(this)
        enableEdgeToEdge()
        setContent {
            XXGKamiTheme {
                AppNavigation()
            }
        }
    }
}
