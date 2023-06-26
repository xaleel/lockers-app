package ge.edison.lockers

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class BackgroundService : Service() {
    lateinit var mainHandler: Handler
    private var usbHealth = "?"
    var messenger: Messenger? = null
    private var apiHealth = "?"
    private var notificationManager: NotificationManager? = null

    private val incomingHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            messenger = msg.replyTo
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        if (!this::mainHandler.isInitialized) {
            mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post(connectionHealthTask)
        }

        val mMessenger = Messenger(incomingHandler)
        return mMessenger.binder
    }

    override fun onCreate() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        showNotification("Waiting for initialization.")
        super.onCreate()
    }


    private val connectionHealthTask = object : Runnable {
        override fun run() {
            val message = Message()
            if (State.connectionHandler != null) {
                runBlocking {
                    withTimeout(State.timeout.toLong() - 25) {
                        State.connectionHandler!!.fetchStatus()
                    }
                }
                synchronized(State) {
                    logUSBHealth()
                }
                synchronized(APIClient) {
                    logAPIHealth()
                }
            }
            messenger?.send(message)
            mainHandler.postDelayed(this, State.timeout.toLong())
        }
    }

    private fun logUSBHealth(): String? {
        if (State.lastOutput.size < 9) return null
        val percentage: Float =
            State.lastOutput.filter { status -> status }.size.toFloat() / State.lastOutput.size.toFloat() * 100
        if (percentage == 0F || State.connectionHandler?.checkStatus() != true) {
            if (State.toBeDisconnected) {
                State.connectionHandler = null
            }
            else State.toBeDisconnected = true
        }
        val displayPercentage = if (percentage.isNaN()) "?" else "${percentage.toInt()}%"
        usbHealth = displayPercentage

        val message = Message()
        message.obj = listOf(
            Console.Message("USB connection health:", "#FFFFFF", 5),
            Console.Message(displayPercentage, "#FFFFFF", 0)
        )
        messenger?.send(message)
        State.lastOutput.clear()
        return displayPercentage
    }

    private fun logAPIHealth() {
        if (APIClient.lastOutput.size < 9) return
        val percentage: Float =
            APIClient.lastOutput.filter { status -> status in 200..299 }.size.toFloat() / APIClient.lastOutput.size.toFloat() * 100
        if (percentage == 0F) {
            APIClient.disconnected = if (APIClient.disconnected == 0) 1 else 2
        }
        val displayPercentage = if (percentage.isNaN()) "?" else "${percentage.toInt()}%"
        apiHealth = displayPercentage
        val message = Message()
        message.obj = listOf(
            Console.Message("API connection health:", "#FFFFFF", 5),
            Console.Message(displayPercentage, "#FFFFFF", 0)
        )
        messenger?.send(message)
        APIClient.lastOutput.clear()

        if (notificationManager != null){
            showNotification("USB Health: $usbHealth. API Health: $apiHealth")
        }
    }

    private fun showNotification(text: String) {
        val notification: Notification = NotificationCompat.Builder(this, Notification())
            .setContentTitle("Service running")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()
        notificationManager?.notify(123, notification)
    }

    override fun onDestroy() {
        super.onDestroy()

        mainHandler.removeCallbacks(connectionHealthTask)
    }
}