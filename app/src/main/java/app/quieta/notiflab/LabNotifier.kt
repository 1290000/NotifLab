package app.quieta.notiflab

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/** Fixed lab channel catalog used to stress-test Quieta inventory / mute / intercept. */
data class LabChannelSpec(
    val id: String,
    val title: String,
    val description: String,
    val importance: Int,
    val sampleTitle: String,
    val sampleText: String,
)

object LabCatalog {
    const val GROUP_ID = "quieta_lab"

    val channels: List<LabChannelSpec> = listOf(
        LabChannelSpec(
            id = "lab.marketing.promo",
            title = "营销推送",
            description = "高强度营销类渠道，适合批量静音验收",
            importance = NotificationManager.IMPORTANCE_HIGH,
            sampleTitle = "Quieta Lab 限时折扣",
            sampleText = "marketing.promo #{{n}} 仅用于息匣静音测试",
        ),
        LabChannelSpec(
            id = "lab.marketing.event",
            title = "活动通知",
            description = "中等优先级活动渠道",
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            sampleTitle = "Quieta Lab 活动预告",
            sampleText = "marketing.event #{{n}} 可按关键词批量处理",
        ),
        LabChannelSpec(
            id = "lab.order.status",
            title = "订单状态",
            description = "重要订单变更，应保持可提醒",
            importance = NotificationManager.IMPORTANCE_HIGH,
            sampleTitle = "Quieta Lab 订单已更新",
            sampleText = "order.status #{{n}} 对照组：重要业务通知",
        ),
        LabChannelSpec(
            id = "lab.order.shipping",
            title = "物流更新",
            description = "中等优先级物流信息",
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            sampleTitle = "Quieta Lab 快件已揽收",
            sampleText = "order.shipping #{{n}} 中优先级噪音源",
        ),
        LabChannelSpec(
            id = "lab.live.stream",
            title = "直播开播",
            description = "直播类打扰渠道",
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            sampleTitle = "Quieta Lab 主播开播了",
            sampleText = "live.stream #{{n}} 易与营销渠道混淆",
        ),
        LabChannelSpec(
            id = "lab.system.info",
            title = "系统信息",
            description = "低优先级系统提示",
            importance = NotificationManager.IMPORTANCE_LOW,
            sampleTitle = "Quieta Lab 系统信息",
            sampleText = "system.info #{{n}} 低优先级对照",
        ),
        LabChannelSpec(
            id = "lab.privacy.alert",
            title = "隐私提醒",
            description = "最低可见性渠道",
            importance = NotificationManager.IMPORTANCE_MIN,
            sampleTitle = "Quieta Lab 隐私提醒",
            sampleText = "privacy.alert #{{n}} MIN 级渠道",
        ),
        LabChannelSpec(
            id = "lab.support.chat",
            title = "客服消息",
            description = "客服会话通知",
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            sampleTitle = "Quieta Lab 客服已接入",
            sampleText = "support.chat #{{n}} 默认优先级噪音源",
        ),
    )
}

class LabNotifier(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channelGroup = NotificationChannelGroup(
            LabCatalog.GROUP_ID,
            "息匣通知实验室",
        )
        notificationManager.createNotificationChannelGroup(channelGroup)
        LabCatalog.channels.forEach { spec ->
            // Only create missing channels. Re-creating an existing channel would
            // overwrite importance that Quieta just muted/downgraded.
            if (notificationManager.getNotificationChannel(spec.id) != null) return@forEach
            val channel = NotificationChannel(spec.id, spec.title, spec.importance).apply {
                description = spec.description
                setGroup(LabCatalog.GROUP_ID)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun resetChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.notificationChannels.forEach { channel ->
                if (channel.id.startsWith("lab.")) {
                    notificationManager.deleteNotificationChannel(channel.id)
                }
            }
            notificationManager.deleteNotificationChannelGroup(LabCatalog.GROUP_ID)
        }
        // Force-recreate with catalog importances.
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(LabCatalog.GROUP_ID, "息匣通知实验室"),
        )
        LabCatalog.channels.forEach { spec ->
            val channel = NotificationChannel(spec.id, spec.title, spec.importance).apply {
                description = spec.description
                setGroup(LabCatalog.GROUP_ID)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun send(spec: LabChannelSpec, seq: Int) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannelIfMissing(spec)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, spec.id)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }
        val text = spec.sampleText.replace("{{n}}", seq.toString())
        val notification = builder
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("${spec.sampleTitle} #$seq")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        notificationManager.notify((spec.id.hashCode() and 0x7fffffff) / 16 + seq, notification)
    }

    private fun ensureChannelIfMissing(spec: LabChannelSpec) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (notificationManager.getNotificationChannel(spec.id) != null) return
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(LabCatalog.GROUP_ID, "息匣通知实验室"),
        )
        val channel = NotificationChannel(spec.id, spec.title, spec.importance).apply {
            description = spec.description
            setGroup(LabCatalog.GROUP_ID)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun sendAll(startSeq: Int) {
        LabCatalog.channels.forEach { spec ->
            send(spec, startSeq)
        }
    }

    fun clearAll() {
        notificationManager.cancelAll()
    }
}
