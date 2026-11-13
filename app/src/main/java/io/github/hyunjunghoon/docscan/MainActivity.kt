package io.github.hyunjunghoon.docscan

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hyunjunghoon.docscan.ads.Ads
import io.github.hyunjunghoon.docscan.ui.AppRoot
import io.github.hyunjunghoon.docscan.ui.AppTheme
import io.github.hyunjunghoon.docscan.ui.isDark

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Ads.gatherConsent(this)
        // Only on a fresh start: after rotation or process death the files were already imported.
        if (savedInstanceState == null) importShared(intent)
        setContent {
            val mode by vm.themeMode.collectAsStateWithLifecycle()
            val dark = mode.isDark()
            // Keep status/navigation bar icons readable when the app theme differs from the phone's.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            AppTheme(mode = mode) { AppRoot(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        importShared(intent)
    }

    private fun importShared(intent: Intent?) {
        val uris = SharedFiles.uris(intent)
        if (uris.isNotEmpty()) {
            vm.openDoc(null)
            vm.importShared(uris)
        }
    }
}
