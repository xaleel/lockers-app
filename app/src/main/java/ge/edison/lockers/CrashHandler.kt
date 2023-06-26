package ge.edison.lockers

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.lang.Thread.UncaughtExceptionHandler
import kotlin.system.exitProcess

class CrashHandler(private val context: Context): UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        Toast.makeText(context, "Uncaught Exception: ".plus(e.message).plus(e.stackTraceToString()), Toast.LENGTH_LONG).show()
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification: Notification = NotificationCompat.Builder(this.context, Notification())
            .setContentTitle("Service running")
            .setContentText("Uncaught Exception: ".plus(e.message))
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()
        notificationManager?.notify(123, notification)

//        val serviceIntent = Intent(context, BackgroundService::class.java)
//        context.stopService(serviceIntent)
//
//        val intent = Intent(context, ErrorDisplay::class.java)
//        intent.putExtra("stackTrace", e.stackTraceToString())
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        context.startActivity(intent)

        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(10)
    }
}