# Keep yt-dlp bridge + JNI symbols
-keep class com.scottyab.rootemu.** { *; }
-keep class org.kamranzafar.** { *; }
-keep class com.yausername.** { *; }

# yt-dlp relies on reflection for its python runtime
-keepclassmembers class * {
    native <methods>;
}

-dontwarn com.yausername.**
