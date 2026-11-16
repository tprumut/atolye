# Speed Meter Plugin ProGuard Rules

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

# Keep service and receivers
-keep class com.toolbox.plugin.speedmeter.SpeedMeterService { *; }
-keep class com.toolbox.plugin.speedmeter.SpeedMeterBootReceiver { *; }
-keep class com.toolbox.plugin.speedmeter.SpeedMeterPluginActivity { *; }
-keep class com.toolbox.plugin.speedmeter.SpeedMeterSettingsActivity { *; }
-keep class com.toolbox.plugin.speedmeter.SpeedMeterViewModel { *; }
-keep class com.toolbox.plugin.speedmeter.SpeedMeterSettingsViewModel { *; }

# Keep TrafficStats usage
-keep class android.net.TrafficStats { *; }

# Keep notification channel
-keep class android.app.NotificationChannel { *; }