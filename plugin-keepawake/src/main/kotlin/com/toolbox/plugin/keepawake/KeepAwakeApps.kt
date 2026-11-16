package com.toolbox.plugin.keepawake

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

data class LaunchableApp(
    val packageName: String,
    val label: String
)

/**
 * Uygulama bazlı mod için başlatılabilir uygulamaları listeler.
 *
 * `QUERY_ALL_PACKAGES` yerine manifestte tanımlı launcher intent sorgusu
 * kullanılır; bu F-Droid politikasıyla da uyumludur.
 */
fun loadLaunchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(0L)
        )
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }

    return resolved
        .mapNotNull { info ->
            val activityInfo = info.activityInfo ?: return@mapNotNull null
            val packageName = activityInfo.packageName ?: return@mapNotNull null
            if (packageName == context.packageName) return@mapNotNull null
            val label = runCatching {
                pm.getApplicationLabel(activityInfo.applicationInfo).toString()
            }.getOrDefault(packageName)
            LaunchableApp(packageName = packageName, label = label)
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
