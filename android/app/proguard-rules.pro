# Keep data classes
-keep class com.jarvis.ai.data.** { *; }
-keep class com.jarvis.ai.network.** { *; }
# Gson
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class com.google.gson.** { *; }
