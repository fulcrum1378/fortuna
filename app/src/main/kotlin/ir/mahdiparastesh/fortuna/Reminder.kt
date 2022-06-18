package ir.mahdiparastesh.fortuna

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
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
                }.timeInMillis, AlarmManager.INTERVAL_DAY, intent(c)
            )
        }

        private fun intent(c: Context): PendingIntent = PendingIntent.getBroadcast(
            c, 0, Intent(c, Reminder::class.java).setAction(REMIND),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onReceive(c: Context, intent: Intent) {
        when (intent.action) {
            REMIND -> {
                val cal = Main.calType.newInstance()
                val score = Vita.load(c).getOrDefault(cal.toKey(), null)
                    ?.get(cal[Calendar.DAY_OF_MONTH])
                if (score == null) NotificationManagerCompat.from(c).notify(
                    CHANNEL, NotificationCompat.Builder(c, REMIND)
                        .setSmallIcon(R.mipmap.launcher_round)
                        .setContentTitle(c.getString(R.string.ntfReminder))
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setContentIntent(
                            PendingIntent.getActivity(
                                c, 0, Intent(c, Main::class.java), PendingIntent.FLAG_IMMUTABLE
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
