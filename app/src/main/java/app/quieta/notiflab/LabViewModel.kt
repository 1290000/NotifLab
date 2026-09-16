package app.quieta.notiflab

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel

class LabViewModel(application: Application) : AndroidViewModel(application) {
    private val notifier = LabNotifier(application)

    var lastMessage by mutableStateOf("就绪：8 个预置渠道")
        private set

    var sendSeq by mutableStateOf(1)
        private set

    var notificationsEnabled by mutableStateOf(true)
        private set

    init {
        ensureChannels()
        refreshPermission()
    }

    fun refreshPermission() {
        notificationsEnabled = NotificationManagerCompat
            .from(getApplication())
            .areNotificationsEnabled()
    }

    fun ensureChannels() {
        notifier.ensureChannels()
        lastMessage = "已确保 8 个 lab.* 渠道存在"
    }

    fun sendChannel(spec: LabChannelSpec) {
        refreshPermission()
        if (!notificationsEnabled) {
            lastMessage = "通知权限未授予，无法发送 ${spec.title}"
            return
        }
        val seq = sendSeq
        sendSeq += 1
        notifier.send(spec, seq)
        lastMessage = "已发送 ${spec.id} #$seq"
    }

    fun sendAll() {
        refreshPermission()
        if (!notificationsEnabled) {
            lastMessage = "通知权限未授予，无法连发"
            return
        }
        val seq = sendSeq
        sendSeq += LabCatalog.channels.size
        notifier.sendAll(seq)
        lastMessage = "已连发全部 8 个渠道，起始序号 #$seq"
    }

    fun resetChannels() {
        notifier.resetChannels()
        sendSeq = 1
        lastMessage = "渠道已删除并重建；发送序号已重置"
    }

    fun clearNotifications() {
        notifier.clearAll()
        lastMessage = "已清空本应用通知"
    }
}
