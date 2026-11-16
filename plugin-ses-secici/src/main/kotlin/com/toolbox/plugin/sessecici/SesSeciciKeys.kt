package com.toolbox.plugin.sessecici

object SesSeciciKeys {
    const val PLUGIN_ID = "com.toolbox.plugin.sessecici"
    const val NOTIFICATION_ENABLED = "notification_enabled"
    const val AUTO_APPLY_ON_CONNECT = "auto_apply_on_connect"
    const val EXPERIMENTAL_APP_AUDIO = "experimental_app_audio"

    // Per-device prefs are stored as: dev_vol_<key>, dev_rem_<key>
    fun volumeKey(deviceKey: String) = "dev_vol_$deviceKey"
    fun rememberKey(deviceKey: String) = "dev_rem_$deviceKey"
}
