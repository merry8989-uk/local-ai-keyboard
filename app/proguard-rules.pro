# Keep MediaPipe LLM Inference classes (JNI + reflection).
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
