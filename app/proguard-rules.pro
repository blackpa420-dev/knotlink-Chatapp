# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Room database entities & DAOs
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
}

# Keep Moshi models & codegen adapters
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn org.codehaus.mojo.animal_sniffer.*
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }

# Keep Firebase models
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName *;
    @com.google.firebase.database.IgnoreExtraProperties *;
    @com.google.firebase.firestore.PropertyName *;
    @com.google.firebase.firestore.IgnoreExtraProperties *;
}
-keep class com.google.firebase.** { *; }

# Keep Kotlinx Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.coroutines.experimental.android.HandlerContext {
    volatile <fields>;
}

# Keep Data Models
-keep class com.example.data.local.** { *; }
-keep class com.example.data.remote.** { *; }
-keep class com.example.data.model.** { *; }

# WebRTC uses native bindings and runtime-discovered classes.
-keep class org.webrtc.** { *; }
-keep class com.example.webrtc.** { *; }
