plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Android projects don't have a "testClasses" task (it's a JVM plugin task).
// Register it so tools that expect it (e.g. VS Code / Gradle integrations) don't fail.
tasks.register("testClasses")

android {
    namespace = "com.undrift"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.undrift"
        minSdk = 24
        targetSdk = 35
        versionCode = 10
        versionName = "1.0.10-stable"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "undrift123"
                keyAlias = "undrift"
                keyPassword = "undrift123"
            }
        }
    }

    buildTypes {
        debug {
            val keystoreFile = rootProject.file("release.keystore")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = false
            val keystoreFile = rootProject.file("release.keystore")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
    }
    
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/native-image/native-image.properties"
            excludes += "/META-INF/native-image/reflect-config.json"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.mongodb.driver.kotlin.coroutine)
    implementation(libs.androidx.navigation.compose)
    implementation("com.adamglin:phosphor-icon:1.0.0")
    implementation("io.github.stoyan-vuchev:squircle-shape-android:4.0.0")
    implementation("dev.chrisbanes.haze:haze:1.1.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
