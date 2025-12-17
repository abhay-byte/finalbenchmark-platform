import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

// F-Droid reproducible builds: disable baseline profiles
tasks.whenTaskAdded { if (name.contains("ArtProfile")) { enabled = false } }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ivarna.finalbenchmark2"
    compileSdk { version = release(36) }

    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.ivarna.finalbenchmark2"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64") }

        // C++ Optimization Flags for maximum performance
        externalNativeBuild {
            cmake {
                cppFlags +=
                        listOf(
                                "-O3", // Maximum optimization
                                "-ffast-math", // Fast floating-point math
                                "-funroll-loops", // Unroll loops for speed
                                "-fomit-frame-pointer", // Don't keep frame pointers
                                "-ffunction-sections", // Separate functions for linker optimization
                                "-fdata-sections", // Separate data for linker optimization
                                "-fvisibility=hidden" // Hide symbols by default
                                // Note: -flto removed because NDK 25 uses gold linker which doesn't
                                // support LTO on arm64
                                )
                cFlags += listOf("-O3", "-ffast-math", "-funroll-loops")
                arguments +=
                        listOf(
                                "-DANDROID_STL=c++_shared",
                                "-DANDROID_PLATFORM=android-24",
                                "-DCMAKE_BUILD_TYPE=Release"
                        )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    androidResources {
        // Disable PNG crunching for reproducible builds
        @Suppress("UnstableApiUsage")
        ignoreAssetsPattern = "!.svn:!.git:.*:!CVS:!thumbs.db:!picasa.ini:!*.scc:*~"
    }

    // Disable dependency metadata block for F-Droid
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        debug {
            isDebuggable = true
            isJniDebuggable = false
            packaging { resources.excludes.add("META-INF/**") }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
            // Disable baseline profiles for F-Droid reproducible builds
            packaging {
                resources.excludes.add("META-INF/**")
                resources.excludes.add("**.prof")
                resources.excludes.add("assets/dexopt/baseline.prof")
            }
        }

        // Signing configuration for release builds
        signingConfigs {
            create("release") { storeFile = file("../keystore/my-release-key.keystore") }
        }

        buildTypes {
            debug {
                isDebuggable = true
                isJniDebuggable = false
                packaging { resources.excludes.add("META-INF/**") }
            }
            release {
                isMinifyEnabled = false
                proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                )
                packaging { resources.excludes.add("META-INF/**") }
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }

    packaging { jniLibs { useLegacyPackaging = true } }
}

// Reproducible builds configuration for F-Droid
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation("dev.chrisbanes.haze:haze:1.0.0") // Compatible with Kotlin 2.0.x
    implementation("dev.chrisbanes.haze:haze-materials:1.0.0") // Compatible with Kotlin 2.0.x
    implementation(
            "androidx.compose.material:material-icons-extended:1.7.5"
    ) // Add icons extended directly
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
