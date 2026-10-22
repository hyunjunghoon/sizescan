import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

// Google's official AdMob test IDs. Release builds read real IDs from Gradle properties
// (-PADMOB_APP_ID=... etc. or ~/.gradle/gradle.properties) and fall back to test IDs.
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
fun prop(name: String): String? = (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }

android {
    namespace = "io.github.hyunjunghoon.docscan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tallowbird.sizescan"
        minSdk = 26
        targetSdk = 36
        // Set in gradle.properties and bumped by hand for each Play upload.
        versionCode = prop("VERSION_CODE")?.toInt() ?: error("VERSION_CODE missing in gradle.properties")
        versionName = prop("VERSION_NAME") ?: error("VERSION_NAME missing in gradle.properties")
        resourceConfigurations += listOf("en", "ko", "es", "pt-rBR", "hi", "in", "de", "fr", "ja")
    }

    signingConfigs {
        create("release") {
            val path = System.getenv("SIGNING_KEYSTORE_PATH")
            if (path != null) {
                storeFile = file(path)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "AD_BANNER_ID", "\"$testBannerId\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"$testInterstitialId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = prop("ADMOB_APP_ID") ?: testAppId
            buildConfigField("String", "AD_BANNER_ID", "\"${prop("ADMOB_BANNER_ID") ?: testBannerId}\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"${prop("ADMOB_INTERSTITIAL_ID") ?: testInterstitialId}\"")
            if (System.getenv("SIGNING_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Production uploads must never ship test ad IDs (closed-testing builds may).
            if (prop("REQUIRE_REAL_ADS") == "true") {
                listOf("ADMOB_APP_ID", "ADMOB_BANNER_ID", "ADMOB_INTERSTITIAL_ID").forEach {
                    if (prop(it) == null) throw GradleException("$it is missing for a production build")
                }
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("com.google.android.gms:play-services-ads:24.4.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
    implementation("com.google.android.play:review:2.0.2")

    testImplementation("junit:junit:4.13.2")
    // Store screenshots rendered on the JVM (Robolectric + Roborazzi), no emulator needed.
    testImplementation("org.robolectric:robolectric:4.15.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.43.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.43.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
