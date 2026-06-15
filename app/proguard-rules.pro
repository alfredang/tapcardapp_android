# Keep kotlinx.serialization generated serializers for our @Serializable models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.tertiaryinfotech.tapcard.** {
    *** Companion;
}
-keepclasseswithmembers class com.tertiaryinfotech.tapcard.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ML Kit text recognition keeps its own consumer rules; nothing extra needed.
