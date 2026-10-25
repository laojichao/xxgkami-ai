plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.0.21"
    id("com.android.library")
}

android {
    namespace = "com.xxgkami.shared"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    androidTarget()
    // 必须显式指定 jvmTarget=17：仅设 compileOptions 的 source/targetCompatibility
    // 不够——Kotlin 编译默认 jvmTarget 1.8，与 Java 17 混编报
    // "Inconsistent JVM-target compatibility (17 vs 1.8)" 构建失败
    androidTarget {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }

    // 声明 iOS 目标，KMP 会自动创建 iosMain 中间源集供三者共享
    val iosX64 = iosX64()
    val iosArm64 = iosArm64()
    val iosSimulatorArm64 = iosSimulatorArm64()

    // iOS framework 配置，供 iOS App 集成使用
    configure(listOf(iosX64, iosArm64, iosSimulatorArm64)) {
        binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:2.3.13")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.13")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.13")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.3.13")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:2.3.13")
            }
        }
    }
}

// core library desugaring：依赖必须挂在 androidMain 的 compileClasspath 上，
// 此前用 add("androidMainImplementation", ...) 会导致配置为空，
// AGP DesugarLibUtils 直接抛 RuntimeException（启用但无依赖）
android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    add("coreLibraryDesugaring", "com.android.tools:desugar_jdk_libs:2.0.4")
}
