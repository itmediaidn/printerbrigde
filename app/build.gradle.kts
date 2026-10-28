plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.titiktolak.printer"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.titiktolak.printer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
