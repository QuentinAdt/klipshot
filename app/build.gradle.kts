plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.quentinadt.klipshot"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.quentinadt.klipshot"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
