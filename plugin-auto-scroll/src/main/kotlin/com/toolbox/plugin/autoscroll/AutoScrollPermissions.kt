package com.toolbox.plugin.autoscroll

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object AutoScrollPermissions {

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
        } catch (_: Throwable) {
            null
        } ?: return false

        val expected = ComponentName(
            context,
            AutoScrollAccessibilityService::class.java,
        ).flattenToString()
        val className = AutoScrollAccessibilityService::class.java.name
        return enabled.split(':').any { entry ->
            entry.equals(expected, ignoreCase = true) ||
                entry.contains(className, ignoreCase = true)
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val component = ComponentName(context, AutoScrollAccessibilityService::class.java)
        val targeted = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("android.provider.extra.COMPONENT_NAME", component)
        try {
            context.startActivity(targeted)
        } catch (_: Throwable) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Throwable) {
            }
        }
    }

    /** Yüzen kısayol için "diğer uygulamaların üstünde göster" izni. */
    fun canDrawOverlays(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
