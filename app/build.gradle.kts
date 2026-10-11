import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

fun git(vararg args: String): String =
    providers.exec { commandLine("git", *args) }.standardOutput.asText.get().trim()

val keystoreProperties = Properties().apply {
    val file = rootProject.file("key.properties")
    if (file.exists()) file.reader().use { load(it) }
}

android {
    namespace = "dev.yukineko.ekimemo_map"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.yukineko.ekimemo_map"
        minSdk = 24
        targetSdk = 35
        versionCode = git("rev-list", "HEAD", "--count").toInt()
        versionName = "1.0"
        buildConfigField("String", "COMMIT_HASH", "\"${git("rev-parse", "--short", "HEAD")}\"")
    }

    signingConfigs {
        create("release") {
            if (keystoreProperties.isEmpty) {
                storeFile = rootProject.file("release.keystore")
                storePassword = System.getenv("STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            resValue("string", "app_name", "駅メモマップ")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            resValue("string", "app_name", "駅メモマップ (Dev)")
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        buildConfig = true
        resValues = true
        compose = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material.kolor)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.msgpack.core)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
