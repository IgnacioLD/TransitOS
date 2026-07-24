# Keep Koin's reflection-free runtime.
-keep class org.koin.** { *; }

# Kotlinx Serialization (used by Ktor + domain models).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep domain model classes (serialised across the network boundary).
-keep class app.transitos.core.model.** { *; }
