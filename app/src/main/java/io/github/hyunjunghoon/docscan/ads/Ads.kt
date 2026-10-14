package io.github.hyunjunghoon.docscan.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import io.github.hyunjunghoon.docscan.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consent (UMP) first, then AdMob. Ads are only requested after consent allows it.
 * Placement: one banner on the home screen and an interstitial after a finished export,
 * never in the middle of scanning or editing.
 */
object Ads {
    private const val MIN_INTERSTITIAL_GAP_MS = 120_000L
    private val started = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready
    private val _privacyRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyRequired

    private var interstitial: InterstitialAd? = null
    private var loading = false
    private var lastShownAt = 0L

    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updatePrivacy(info)
                    if (info.canRequestAds()) start(activity.applicationContext)
                }
            },
            { if (info.canRequestAds()) start(activity.applicationContext) },
        )
        // Consent from a previous session is enough to start right away.
        if (info.canRequestAds()) start(activity.applicationContext)
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {}
    }

    private fun updatePrivacy(info: ConsentInformation) {
        _privacyRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    // General audiences only: keeps sexual, mature and suggestive ads out of every request.
    internal fun requestConfiguration(): RequestConfiguration =
        RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()

    private fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        MobileAds.setRequestConfiguration(requestConfiguration())
        Thread {
            MobileAds.initialize(context) {
                main.post {
                    _ready.value = true
                    preloadInterstitial(context)
                }
            }
        }.start()
    }

    private fun preloadInterstitial(context: Context) {
        if (!_ready.value || interstitial != null || loading) return
        loading = true
        InterstitialAd.load(
            context,
            BuildConfig.AD_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                }
            },
        )
    }

    /** Shows an interstitial if one is loaded and the frequency cap allows it. */
    fun maybeShowInterstitial(activity: Activity) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        if (ad == null || (lastShownAt != 0L && now - lastShownAt < MIN_INTERSTITIAL_GAP_MS)) {
            preloadInterstitial(activity.applicationContext)
            return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                lastShownAt = SystemClock.elapsedRealtime()
                preloadInterstitial(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preloadInterstitial(activity.applicationContext)
            }
        }
        ad.show(activity)
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ready by Ads.ready.collectAsStateWithLifecycle()
    if (!ready) return
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adView = remember(widthDp) {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            adUnitId = BuildConfig.AD_BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }
    AndroidView(factory = { adView }, modifier = modifier.fillMaxWidth())
}
