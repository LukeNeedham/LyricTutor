# kotlinx.serialization: keep the generated serializers of the @Serializable classes (the routes, and the app's own).
# These are the rules of the library's README, for this app's package.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lukeneedham.lyrictutor.**$$serializer { *; }
-keepclassmembers class com.lukeneedham.lyrictutor.** {
    *** Companion;
}
-keepclasseswithmembers class com.lukeneedham.lyrictutor.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# The shared library's logs and crashes pages read these by serialization (docs/using-the-library.md in ci-workflows)
-keep class com.lukeneedham.common.logger.LogEntry { *; }
-keep class com.lukeneedham.common.crash.CrashEntry { *; }

# Room: the generated database implementation is created by reflection
-keep class * extends androidx.room.RoomDatabase
