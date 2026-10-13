package io.github.hyunjunghoon.docscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.hyunjunghoon.docscan.ads.Ads
import io.github.hyunjunghoon.docscan.ui.AppRoot
import io.github.hyunjunghoon.docscan.ui.AppTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Ads.gatherConsent(this)
        setContent { AppTheme { AppRoot(vm) } }
    }
}
