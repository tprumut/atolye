# Atölye Host ProGuard Rules

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class com.atolye.host.**Hilt* { *; }

# Keep Compose compiler generated classes
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep plugin contract classes for reflection
-keep class com.toolbox.contract.** { *; }

# Keep DataStore generated classes
-keep class androidx.datastore.** { *; }

# Keep RxJava
-keep class io.reactivex.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep serialization
-keep class kotlinx.serialization.** { *; }

# Keep plugin discovery classes
-keep class com.toolbox.plugin.** { *; }

# Keep Hilt ViewModel
-keep class com.atolye.host.ui.**ViewModel { *; }