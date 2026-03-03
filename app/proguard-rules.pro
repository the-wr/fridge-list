# Error Prone annotations are compile-time only; referenced by Tink (via security-crypto) at runtime
-dontwarn com.google.errorprone.annotations.**

-keep class com.fridgelist.app.data.** { *; }
-keepclassmembers class com.fridgelist.app.provider.todoist.** { *; }
-keepclassmembers class * implements com.squareup.moshi.JsonAdapter { *; }
