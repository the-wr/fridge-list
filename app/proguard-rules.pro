# Error Prone annotations are compile-time only; referenced by Tink (via security-crypto) at runtime
-dontwarn com.google.errorprone.annotations.**

# Keep all annotation attributes — critical for:
#   - Retrofit reading @Header/@Body/@Query/@Path on method PARAMETERS (RuntimeVisibleParameterAnnotations)
#   - Moshi finding @JsonClass and @Json on classes/fields (RuntimeVisibleAnnotations)
#   - KotlinJsonAdapterFactory detecting Kotlin classes via @kotlin.Metadata
#   - Generic type signatures needed by Retrofit for suspend return types
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# App data models (Room entities, etc.)
-keep class com.fridgelist.app.data.** { *; }

# Provider data classes and API interfaces — full keep so R8 doesn't rename or remove them
-keep class com.fridgelist.app.provider.** { *; }

# Moshi — keep generated JsonAdapters
-keep class **JsonAdapter { *; }
