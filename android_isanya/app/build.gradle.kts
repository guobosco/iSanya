plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

// Emulator debug default (Android Emulator -> host machine loopback)
val localApiBaseUrl = "http://10.0.2.2:8000/"
val devApiBaseUrl = providers.gradleProperty("DEV_API_BASE_URL").orElse(localApiBaseUrl).get()
val prodApiBaseUrl = providers.gradleProperty("PROD_API_BASE_URL").orElse(localApiBaseUrl).get()

android {
    namespace = "com.example.Lulu"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.Lulu"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // 配置支持的CPU架构
        ndk {
            abiFilters += "armeabi-v7a"
            abiFilters += "arm64-v8a"
            abiFilters += "x86"
            abiFilters += "x86_64"
        }

        // 高德地图 Key：在 gradle.properties 中设置 AMAP_API_KEY=你的Key（控制台绑定包名 com.example.Lulu 与 SHA1）
        manifestPlaceholders["AMAP_API_KEY"] =
            providers.gradleProperty("AMAP_API_KEY").orElse("").get()
    }

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"$devApiBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"$prodApiBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
    }

    /*
    signingConfigs {
        create("release") {
            storeFile = file("../Lulu.keystore")
            storePassword = "android"
            keyAlias = "Lulu"
            keyPassword = "android"
        }
    }
    */

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            buildConfigField("Boolean", "ENABLE_HTTP_LOGGING", "true")
        }
        getByName("release") {
            // signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            isCrunchPngs = true
            buildConfigField("Boolean", "ENABLE_HTTP_LOGGING", "false")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("huawei") {
            // signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            isCrunchPngs = true
            buildConfigField("Boolean", "ENABLE_HTTP_LOGGING", "false")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    
    // 禁用基线配置文件生成
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Network and JSON dependencies
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Compose dependencies
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    
    // System UI Controller
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
    
    // Lunar Calendar
    implementation("cn.6tail:lunar:1.3.11")

    // Room dependencies
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Image Cropping (uCrop)
    implementation("com.github.yalantis:ucrop:2.2.8")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // ViewModel dependencies
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Navigation dependencies with explicit version
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // ZXing for QR Code
    implementation("com.google.zxing:core:3.5.3")

    // CameraX dependencies
    val camerax_version = "1.3.1"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.guava:guava:31.1-android")

    // 高德组合包：统一 3D 地图与检索版本，避免拆分依赖产生重复类
    implementation("com.amap.api:3dmap-location-search:10.1.600_loc6.5.1_sea9.7.4")

    // ShortcutBadger for App Icon Badges (WeChat style)
    implementation("me.leolin:ShortcutBadger:1.1.22")
    
    // Background work dependency
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
