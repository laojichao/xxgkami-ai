# Add project specific ProGuard rules here.

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.xxgkami.**$$serializer { *; }
-keepclassmembers class com.xxgkami.** {
    *** Companion;
}
-keepclasseswithmembers class com.xxgkami.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes used for serialization
-keep class com.xxgkami.shared.model.** { *; }
-keep class com.xxgkami.shared.api.** { *; }

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# OkHttp / Okio (Android 平台 HTTP 引擎依赖)
# OkHttp 官方混淆规则：https://github.com/square/okhttp#proguard
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.PublicClass
-keepnames class okio.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# AndroidX - 收窄 keep 范围，仅保留必要的反射访问点
# Compose 和 ViewModel 等需要反射访问的类保留，其他 androidx 类交给 R8 优化
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.savedstate.** { *; }
-keep class androidx.activity.** { *; }
-dontwarn androidx.**

# Compose
-dontwarn androidx.compose.**

# Keep the application class and entry point
-keep class com.xxgkami.android.MainActivity { *; }

# Keep ViewModels (used by Compose viewModel() which uses reflection)
-keep class com.xxgkami.android.viewmodel.** { *; }

# Keep data store (used by reflection for SharedPreferences)
-keep class com.xxgkami.android.data.** { *; }

# Keep security crypto (EncryptedSharedPreferences uses reflection)
-keep class androidx.security.crypto.** { *; }
