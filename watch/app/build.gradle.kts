import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.imsproject.watch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.imsproject.watch"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootDir}/../watch_keystore.jks")
            storePassword = "qwerty12345"
            keyAlias = "key0"
            keyPassword = "qwerty12345"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            signingConfig = signingConfigs.getByName("release")
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            if (name.contains("Debug", ignoreCase = true)) {
                freeCompilerArgs.add("-Xdebug")
            }
        }
    }
}

dependencies {

    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.fragment.ktx)

    implementation(libs.imsproject.common)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.material.icons.core)
    implementation(libs.okhttp)
    implementation(libs.material3)
    implementation(libs.zxing.core)

    implementation (fileTree(
        "dir" to "libs",
        "include" to "*.aar"
    ))
}

android.applicationVariants.all {
    outputs.all {
        this as BaseVariantOutputImpl
        outputFileName = "ims.apk"
    }
}