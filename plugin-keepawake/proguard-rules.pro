# Keep Awake Plugin ProGuard Rules

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep RxJava
-keep class io.reactivex.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep serialization
-keep class kotlinx.serialization.** { *; }

# Keep plugin contract
-keep class com.toolbox.contract.** { *; }

# Keep plugin base
-keep class com.toolbox.plugin.** { *; }

# Keep service, tile service, and receivers
-keep class com.toolbox.plugin.keepawake.KeepAwakeOverlayService { *; }
-keep class com.toolbox.plugin.keepawake.KeepAwakeTileService { *; }
-keep class com.toolbox.plugin.keepawake.KeepAwakePluginActivity { *; }
-keep class com.toolbox.plugin.keepawake.KeepAwakeSettingsActivity { *; }
-keep class com.toolbox.plugin.keepawake.KeepAwakeViewModel { *; }
-keep class com.toolbox.plugin.keepawake.KeepAwakeSettingsViewModel { *; }
-keep class com.toolbox.plugin.keepawake.AppInfo { *; }

# Keep WindowManager usage
-keep class android.view.WindowManager { *; }
-keep class android.view.WindowManager$LayoutParams { *; }

# Keep PowerManager
-keep class android.os.PowerManager { *; }
-keep class android.os.PowerManager$WakeLock { *; }

# Keep Settings
-keep class android.provider.Settings { *; }

# Keep UsageStatsManager
-keep class android.app.usage.UsageStatsManager { *; }
-keep class android.app.usage.UsageStats { *; }