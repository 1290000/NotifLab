package app.quieta.notiflab

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import java.io.File

/**
 * Foreground debug entry so ADB can trigger lab notifications without UI taps.
 * HyperOS often drops notification posts from background broadcasts; an Activity
 * context is reliable. Does not mutate any system notification settings.
 *
 * adb shell am start -n app.quieta.notiflab.debug/.LabDebugActivity --es cmd send_all
 */
class LabDebugActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cmd = intent?.getStringExtra("cmd") ?: "send_all"
        val channel = intent?.getStringExtra("channel")
        val seq = intent?.getIntExtra("seq", 1) ?: 1
        try {
            val notifier = LabNotifier(this)
            notifier.ensureChannels()
            when (cmd) {
                "send" -> {
                    val spec = LabCatalog.channels.firstOrNull { it.id == channel }
                        ?: LabCatalog.channels.first()
                    notifier.send(spec, seq)
                }
                "send_all" -> notifier.sendAll(seq)
                "reset" -> notifier.resetChannels()
                "clear" -> notifier.clearAll()
            }
            File(filesDir, "lab_debug.log").appendText("$cmd channel=$channel seq=$seq ok\n")
            Log.i(TAG, "debug $cmd ok")
        } catch (error: Throwable) {
            runCatching {
                File(filesDir, "lab_debug.log").appendText("$cmd fail=$error\n")
            }
            Log.e(TAG, "debug $cmd failed", error)
        }
        finish()
    }

    companion object {
        private const val TAG = "NotifLabDebug"
    }
}
