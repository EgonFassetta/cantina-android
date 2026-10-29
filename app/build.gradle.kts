plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "dev.pages.mywinecellar"
    compileSdk = 36

    defaultConfig {
        applicationId = "mywine.cellar"
        minSdk = 24
        targetSdk = 36
        // Per ogni nuovo caricamento su Google Play il numero deve crescere: il flusso "Release" lo imposta da solo.
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Dati pubblici del progetto Supabase (gli stessi già presenti nel sito web).
        buildConfigField("String", "SUPABASE_URL", "\"https://xqhoznbjyoityjtrolzb.supabase.co\"")
        buildConfigField("String", "SUPABASE_KEY", "\"sb_publishable_f_jswE7Sf0BU2hHKkQYuEg_ZT7e1rGh\"")
    }

    // La chiave di firma NON è nel codice: arriva dai segreti di GitHub (vedi .github/workflows/release.yml).
    val keystorePath: String? = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    // Un'unica app: clienti e amministratore nello stesso APK (l'area Admin si apre solo con la password).
    flavorDimensions += "edition"
    productFlavors {
        create("cantina") {
            dimension = "edition"
        }
    }

    buildTypes {
        // Variante solo per le prove automatiche: parla con un server simulato sul computer di prova
        // (10.0.2.2 = il computer che ospita l'emulatore). Non viene mai pubblicata.
        create("fake") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".fake"
            buildConfigField("String", "SUPABASE_URL", "\"http://10.0.2.2:8787\"")
        }
        release {
            isMinifyEnabled = false
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
