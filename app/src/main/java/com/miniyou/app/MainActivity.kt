package com.miniyou.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.miniyou.app.ui.AppNav
import com.miniyou.app.ui.theme.LocalWindowLayout
import com.miniyou.app.ui.theme.MiniYouTheme
import com.miniyou.app.ui.theme.WindowLayout

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniYouTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val layout = WindowLayout(maxWidth.value.toInt(), maxHeight.value.toInt())
                        CompositionLocalProvider(LocalWindowLayout provides layout) {
                            AppNav()
                        }
                    }
                }
            }
        }
    }
}
