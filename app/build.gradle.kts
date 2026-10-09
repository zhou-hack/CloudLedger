import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "xyz.her3.webdavAccount"
    compileSdk = 37

    defaultConfig {
        applicationId = "xyz.her3.webdavAccount"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "WEBDAV_URL", "\"${localProps.getProperty("webdav.url", "")}\"")
        buildConfigField("String", "WEBDAV_USER", "\"${localProps.getProperty("webdav.user", "")}\"")
        buildConfigField("String", "WEBDAV_PASS", "\"${localProps.getProperty("webdav.pass", "")}\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(platform("androidx.compose:compose-bom:2025.09.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
