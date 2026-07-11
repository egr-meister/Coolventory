import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Release signing configuration.
 *
 * Signing values are read from (in priority order):
 *   1. Environment variables (used by CI):
 *        ANDROID_KEYSTORE_FILE, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD
 *   2. A local, git-ignored keystore.properties file at the project root:
 *        storeFile, storePassword, keyAlias, keyPassword
 *
 * If no complete set of credentials is found, the release signing config is NOT created and a
 * release build that requires signing will fail clearly instead of silently using the debug key.
 */
data class ReleaseSigning(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

fun resolveReleaseSigning(): ReleaseSigning? {
    fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

    // 1) Environment variables (CI path).
    val envStore = env("ANDROID_KEYSTORE_FILE")
    val envStorePassword = env("ANDROID_KEYSTORE_PASSWORD")
    val envAlias = env("ANDROID_KEY_ALIAS")
    val envKeyPassword = env("ANDROID_KEY_PASSWORD") ?: envStorePassword
    if (envStore != null && envStorePassword != null && envAlias != null && envKeyPassword != null) {
        val f = file(envStore)
        if (f.exists()) {
            return ReleaseSigning(f, envStorePassword, envAlias, envKeyPassword)
        }
    }

    // 2) Local keystore.properties (developer machine path).
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) {
        val props = Properties().apply { FileInputStream(propsFile).use { load(it) } }
        val storeFile = props.getProperty("storeFile")?.takeIf { it.isNotBlank() }
        val storePassword = props.getProperty("storePassword")?.takeIf { it.isNotBlank() }
        val keyAlias = props.getProperty("keyAlias")?.takeIf { it.isNotBlank() }
        val keyPassword = props.getProperty("keyPassword")?.takeIf { it.isNotBlank() } ?: storePassword
        if (storeFile != null && storePassword != null && keyAlias != null && keyPassword != null) {
            val f = file(storeFile)
            if (f.exists()) {
                return ReleaseSigning(f, storePassword, keyAlias, keyPassword)
            }
        }
    }
    return null
}

val releaseSigning: ReleaseSigning? = resolveReleaseSigning()

android {
    namespace = "com.coolventory.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.coolventory.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = releaseSigning.storeFile
                storePassword = releaseSigning.storePassword
                keyAlias = releaseSigning.keyAlias
                keyPassword = releaseSigning.keyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            // Staged R8 rollout (see README): for the FIRST on-device release verification, set both
            // of these to false to validate a plain release build. After that passes, restore them to
            // true (the shipped configuration below) and re-test. Complete keep rules live in
            // proguard-rules.pro so kotlinx.serialization + DataStore survive shrinking.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigning != null) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Fail clearly rather than silently falling back to the debug key.
                // Assigning null keeps the build unsigned; the assemble task will report the missing
                // signing config. We also surface a readable message during configuration.
                logger.warn(
                    "Coolventory: No release signing credentials found. " +
                        "Set ANDROID_KEYSTORE_FILE/ANDROID_KEYSTORE_PASSWORD/ANDROID_KEY_ALIAS/ANDROID_KEY_PASSWORD " +
                        "or provide keystore.properties. Release artifacts will be UNSIGNED.",
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Activity + Compose
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose BOM keeps Compose artifacts aligned
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines + Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
