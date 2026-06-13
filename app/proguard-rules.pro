# Keep Room generated implementations
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * { @androidx.room.* <methods>; }

# Kotlin metadata
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
