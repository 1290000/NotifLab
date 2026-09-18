package app.quieta.notiflab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Debug-only ADB hook so agents/tests can fire lab notifications without UI taps.
 * Does not change any system notification settings.
 *
 * adb shell am broadcast -a app.quieta.notiflab.debug.ACTION_LAB_CMD \
 *   -n app.quieta.notiflab.debug/.LabDebugReceiver --es cmd send_all
 *
 * cmd: send_all | send | reset | clear
 * optional: --es channel lab.order.status --ei seq 1
 */
class LabDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION) return
        val pending = goAsync()
        try {
            val notifier = LabNotifier(context)
            notifier.ensureChannels()
            when (intent.getStringExtra("cmd") ?: "send_all") {
                "send" -> {
                    val id = intent.getStringExtra("channel")
                    val spec = LabCatalog.channels.firstOrNull { it.id == id }
                        ?: LabCatalog.channels.first()
                    val seq = intent.getIntExtra("seq", 1)
                    notifier.send(spec, seq)
                    Log.i(TAG, "sent ${spec.id} #$seq")
                }
                "send_all" -> {
                    val seq = intent.getIntExtra("seq", 1)
                    notifier.sendAll(seq)
                    Log.i(TAG, "send_all from #$seq")
                }
                "reset" -> {
                    notifier.resetChannels()
                    Log.i(TAG, "channels reset")
                }
                "clear" -> {
                    notifier.clearAll()
                    Log.i(TAG, "notifications cleared")
                }
                else -> Log.w(TAG, "unknown cmd")
            }
        } catch (error: Throwable) {
            Log.e(TAG, "lab debug command failed", error)
        } finally {
            pending.finish()
        }
    }

    companion object {
        const val ACTION = "app.quieta.notiflab.debug.ACTION_LAB_CMD"
        private const val TAG = "NotifLabDebug"
    }
}
