package io.github.hyunjunghoon.docscan.ads

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.RequestConfiguration
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AdsConfigTest {
    @Test
    fun adContentIsLimitedToGeneralAudiences() {
        assertEquals(
            RequestConfiguration.MAX_AD_CONTENT_RATING_G,
            Ads.requestConfiguration().maxAdContentRating,
        )
    }
}
