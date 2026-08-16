# Keep line numbers so stack traces pasted into a GitHub issue are usable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization: the plugin generates serializers as companion objects
# reached reflectively by name.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class app.plainly.music.** {
    *** Companion;
}
-keepclasseswithmembers class app.plainly.music.** {
    kotlinx.serialization.KSerializer serializer(...);
}
