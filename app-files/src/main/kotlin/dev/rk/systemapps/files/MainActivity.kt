package dev.rk.systemapps.files

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.rk.systemapps.core.design.theme.SystemAppsTheme
import dev.rk.systemapps.files.ui.FilesApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SystemAppsTheme {
                FilesApp()
            }
        }
    }
}
