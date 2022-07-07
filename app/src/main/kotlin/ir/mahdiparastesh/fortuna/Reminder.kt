package ir.mahdiparastesh.fortuna

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.mahdiparastesh.fortuna.Vita.Companion.toKey

class Reminder : BroadcastReceiver() {
    companion object {
        const val REMIND = "${BuildConfig.APPLICATION_ID}.remind"
        const val CHANNEL = 666

        fun alarm(c: Context) {
            (c.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.setInexactRepeating(
                AlarmManager.RTC, Calendar.getInstance().apply {
                    timeInMillis += 86400000L
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis, AlarmManager.INTERVAL_DAY, broadcast(c)
            )
        }

        private fun broadcast(c: Context): PendingIntent = PendingIntent.getBroadcast(
            c, 0, Intent(c, Reminder::class.java).setAction(REMIND), PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onReceive(c: Context, intent: Intent) {
        when (intent.action) {
            REMIND -> {
                val cal = Main.calType.newInstance().apply { timeInMillis -= 86400000L }
                val score = Vita.load(c).getOrDefault(cal.toKey(), null)
                    ?.get(cal[Calendar.DAY_OF_MONTH] - 1)
                if (score == null) NotificationManagerCompat.from(c).notify(
                    CHANNEL, NotificationCompat.Builder(c, REMIND)
                        .setSmallIcon(R.drawable.notification)
                        .setContentTitle(c.getString(R.string.ntfReminder))
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setContentIntent(
                            PendingIntent.getActivity(
                                c, 0,
                                Intent(c, Main::class.java).putExtra(Main.EXTRA_LUNA, cal.toKey()),
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                    PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
                            )
                        )
                        .setAutoCancel(true)
                        .setShowWhen(false)
                        .build()
                )
            }
            Intent.ACTION_BOOT_COMPLETED -> alarm(c)
        }
    }
}
