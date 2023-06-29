package ge.edison.lockers

import android.content.Context
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object FileUtil {

    private fun serializeState(): String {
        return "${State.baudRate}/${State.dataBits}/${State.stopBits}/${State.parity}/${State.timeout}/${State.ip}"
    }

    fun saveState(mcoContext: Context): Boolean {
        return writeFileOnInternalStorage(mcoContext, serializeState())
    }

    fun fetchState(mcoContext: Context): Boolean {
        val dir = File(mcoContext.filesDir, "saved")
        if (dir.exists()) {
            val file = File(dir, "state")
            val reader = FileReader(file)
            var str = ""
            try {
                str = reader.readText()
                reader.close()
            } catch (e: IOException) {
                return false
            }
            val values = str.split("/")
            return try {
                State.baudRate = values[0].toInt()
                State.dataBits = values[1].toInt()
                State.stopBits = values[2].toInt()
                State.parity = values[3].toInt()
                State.timeout = values[4].toInt()
                State.ip = values[5]
                true
            } catch (e: Exception) {
                false
            }
        }
        return false
    }

    private fun writeFileOnInternalStorage(
        mcoContext: Context,
        sBody: String
    ): Boolean {
        val dir = File(mcoContext.filesDir, "saved")
        if (!dir.exists()) {
            dir.mkdir()
        }
        return try {
            val file = File(dir, "state")
            val writer = FileWriter(file)
            writer.append(sBody)
            writer.flush()
            writer.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}