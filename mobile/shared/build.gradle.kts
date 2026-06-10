plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.0.21"
    id("com.android.library")
}

android {
    namespace = "com.xxgkami.shared"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    androidTarget()

    listOf(
        iosX64(), iosArm64(), iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:2.3.17")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.17")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.17")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:2.3.17")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.17")
        }
        desktopMain.dependencies {
            implementation("io.ktor:ktor-client-cio:2.3.17")
        }
    }
}

android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    add("androidMainImplementation", "com.android.tools:desugar_jdk_libs:2.0.4")
}
