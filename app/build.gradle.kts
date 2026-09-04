import java.util.Properties

plugins {
    id("com.android.application")
}

// Cle de signature. Absente du depot : le fichier vit hors de l'arborescence, et son
// emplacement peut etre surcharge par la variable d'environnement KLIPSHOT_KEYSTORE.
// Sans ce fichier, seul le build debug est possible — ce qui suffit pour contribuer.
val keystoreProps = Properties().apply {
    val path = System.getenv("KLIPSHOT_KEYSTORE")
        ?: "${System.getProperty("user.home")}/.klipshot/keystore.properties"
    val f = File(path)
    if (f.exists()) f.inputStream().use { load(it) }
}
val canSign = keystoreProps.getProperty("storeFile") != null

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

    signingConfigs {
        if (canSign) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (canSign) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
