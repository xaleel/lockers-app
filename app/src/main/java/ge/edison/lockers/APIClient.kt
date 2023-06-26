package ge.edison.lockers

import android.os.Handler
import android.os.Looper
import androidx.annotation.UiThread
import kotlinx.coroutines.*
import java.net.URL

object APIClient {
    val lastOutput: MutableList<Int> = mutableListOf()

    // 0 -> connected, health fine.
    // when health == 0%, check status:
    // 1 -> to be disconnected next cycle (10 timeouts)
    // 2 -> disconnected
    var disconnected: Int = 0

    @OptIn(DelicateCoroutinesApi::class)
    fun pushStatus(statusAsJson: String, console: Console?) {
        if (disconnected == 2) {
            console?.log(
                listOf(
                    Console.Message(
                        "HTTPUtil::pushStatus:",
                        "#DF4759",
                        3
                    ),
                    Console.Message(
                        "API health 0%, Disconnected. Check the API and click 'APPLY'",
                        "#DEDEDE",
                        0
                    )
                )
            )
            return
        }
        var result: String? = null
        runBlocking {
            GlobalScope.launch {
                result = try {
                    withTimeout((State.timeout - 80).toLong()) {
                        return@withTimeout URL("http://".plus(State.ip).plus(":5000/post/").plus(statusAsJson)).readText()
                    }
                } catch (e: Exception) {  // Timeout, permission, URL or network error
                    null
                }
            }
        }
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            if (result == null) {
                UiThread().run {
                    console?.log(
                        listOf(
                            Console.Message(
                                "APIClient::pushStatus: Response failed.",
                                "#DEDEDE",
                                3
                            ),
                        )
                    )
                }
                lastOutput.add(408)
            } else {
                val doorId = if (result == "null") null else result?.toInt()
                if (State.connectionHandler != null && doorId !== null) {
                    State.connectionHandler!!.openDoor(doorId)
                }
                lastOutput.add(200)
            }
        }, (State.timeout - 50).toLong())
    }
}