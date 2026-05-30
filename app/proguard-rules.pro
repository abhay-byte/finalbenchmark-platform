# FinalBenchmark ProGuard / R8 Rules

# Keep JNI native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ivarna.finalbenchmark2.**$$serializer { *; }
-keepclassmembers class com.ivarna.finalbenchmark2.** {
    *** Companion;
}
-keepclasseswithmembers class com.ivarna.finalbenchmark2.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keep class androidx.room.** { *; }

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep TFLite / LiteRT / MediaPipe
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.ai.edge.litert.** { *; }
-keep class com.google.mediapipe.** { *; }
-dontwarn org.tensorflow.lite.**
-dontwarn com.google.ai.edge.litert.**
-dontwarn com.google.mediapipe.**

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Haze (glassmorphism)
-keep class dev.chrisbanes.haze.** { *; }

# Keep libsu (root)
-keep class com.github.topjohnwu.libsu.** { *; }


# Keep BenchmarkResult (cpuBenchmark package) — used by Gson for history
-keep class com.ivarna.finalbenchmark2.cpuBenchmark.BenchmarkResult { *; }
# Keep benchmark data models
-keep class com.ivarna.finalbenchmark2.data.** { *; }
-keep class com.ivarna.finalbenchmark2.models.** { *; }

# Keep ViewModels
-keep class com.ivarna.finalbenchmark2.ui.viewmodels.** { *; }

# Keep AndroidX Navigation
-keep class androidx.navigation.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# AutoValue/Annotation processor classes (not available on Android)
-dontwarn javax.lang.model.**
-dontwarn autovalue.shaded.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
