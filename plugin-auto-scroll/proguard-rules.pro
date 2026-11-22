# Otomatik Kaydırma ProGuard Rules

-keep class androidx.compose.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.serialization.** { *; }
-keep class com.toolbox.contract.** { *; }
-keep class com.toolbox.plugin.** { *; }
-keep class com.toolbox.plugin.autoscroll.** { *; }

-keep class android.accessibilityservice.** { *; }
-keep class android.view.accessibility.** { *; }
