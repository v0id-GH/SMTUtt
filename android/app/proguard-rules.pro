# Keep Gson models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.smtutt.app.data.model.** { *; }
-keep class com.smtutt.app.data.local.** { *; }

# Jsoup & OkHttp
-keep class org.jsoup.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

