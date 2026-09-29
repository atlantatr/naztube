plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android { namespace = "com.naztube.app"; compileSdk = 35
    defaultConfig { applicationId = "com.naztube.app"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1.0" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
}
