plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.xxgkami.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xxgkami.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.2"
        // API 基础地址（必须以 / 结尾，ApiClient 以 baseUrl + path 直接拼接请求 URL）：
        // - debug 默认指向 Android 模拟器访问宿主机回环地址 10.0.2.2
        // - release 构建时必须覆盖为生产 HTTPS 地址，例如：
        //   ./gradlew assembleRelease -PapiBaseUrl=https://api.example.com/api/
        // - 明文 http:// 仅允许 localhost/10.0.2.2/127.0.0.1（见 ApiProvider.updateBaseUrl 校验）
        val apiBaseUrl = (project.findProperty("apiBaseUrl") as String?) ?: "http://10.0.2.2:8080/api/"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":shared"))
    // BOM 2024.09.00 对应 material3 1.3.0：PullToRefreshBox 首现于 1.3.1，
    // 旧 BOM（2024.06.00 → material3 1.2.1）只有 PullToRefreshContainer，
    // 三个 Screen 的 PullToRefreshBox 无法解析
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    // CreditCard/AccountBalanceWallet/LinkOff 等图标只在 material-icons-extended，
    // icons-core（material3 传递依赖）仅含基础图标
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    // EncryptedSharedPreferences 依赖，用于安全存储 Token
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
