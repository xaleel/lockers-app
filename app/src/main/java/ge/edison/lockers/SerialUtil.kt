package ge.edison.lockers

import com.hoho.android.usbserial.driver.SerialTimeoutException
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer

class SerialUtil(private val port: UsbSerialPort, private val console: Console) {
    var statusToBeLogged: Boolean = false
    private val mReadBuffer = ByteBuffer.allocate(port.readEndpoint.maxPacketSize)
    private val segments = listOf(
        "02 00 30 03 35", "02 10 30 03 3E"
    )

    private val doors = listOf(
        //----- 1 ------
        "02 00 31 03 36",
        "02 01 31 03 37",
        "02 02 31 03 38",
        "02 03 31 03 39",
        "02 04 31 03 3A",
        "02 05 31 03 3B",
        "02 06 31 03 3C",
        "02 07 31 03 3D",
        "02 08 31 03 3E",
        "02 09 31 03 3F",
        "02 0A 31 03 40",
        "02 0B 31 03 41",
        "02 0C 31 03 42",
        "02 0D 31 03 43",
        "02 0E 31 03 44",
        "02 0F 31 03 45",
        //----- 2 ------
        "02 10 31 03 46",
        "02 11 31 03 47",
        "02 12 31 03 48",
        "02 0F 31 03 49",
    )

    fun openDoor(index: Int) {
        var opened = true
        if (index < 0 || index >= doors.size) {
            console.log(
                listOf(
                    Console.Message(
                        "openDoor :: ", "#DEDEDE", 3
                    ),
                    Console.Message(
                        "Error: request failed to open door with index", "#AA4040", 3
                    ),
                    Console.Message(index.toString(), "#FFFFFF", 3),
                    Console.Message("Door index not in the array.", "#AA4040", 3)
                )
            )
            opened = false
        }
        val doorHex = doors[index].replace(" ", "")
        try {
            port.write(decodeHex(doorHex), State.timeout)
        } catch (e: SerialTimeoutException) {
            console.log(
                listOf(
                    Console.Message(
                        "openDoor :: ", "#DEDEDE", 3
                    ), Console.Message("Error: timeout during write.", "#AA4040", 0)
                )
            )
            opened = false
        } catch (e: IOException) {
            console.log(
                listOf(
                    Console.Message(
                        "openDoor :: ", "#DEDEDE", 3
                    ), Console.Message(
                        "Error: IOException thrown while writing.", "#AA4040", 0
                    )
                )
            )
            opened = false
        }
        State.lastOutput.add(opened)
    }

    fun fetchStatus() {
        for (segment in segments) {
            try {
                readData(State.timeout / segments.size)
                port.write(decodeHex(segment.replace(" ", "")), State.timeout / (segments.size + 1))
            } catch (e: SerialTimeoutException) {
                console.log(
                    listOf(
                        Console.Message(
                            "fetchStatus :: ", "#DEDEDE", 3
                        ), Console.Message("Error: timeout during write.", "#AA4040", 0)
                    )
                )
                State.lastOutput.add(false)
            } catch (e: IOException) {
                console.log(
                    listOf(
                        Console.Message(
                            "fetchStatus :: ", "#DEDEDE", 3
                        ), Console.Message(
                            "Error: IOException thrown while writing.", "#AA4040", 0
                        )
                    )
                )
                State.lastOutput.add(false)
            }
        }
    }

    private fun decodeHex(string: String): ByteArray {
        return BigInteger(string, 16).toByteArray()
    }

    private fun leftPad(value: String): String {
        return if (value.length >= 8) value else "0".repeat(8 - value.length) + value
    }

    private fun hexToBinary(hexStr: String): String {
        val i: Long
        try {
            i = hexStr.toLong(16)
        } catch (e: NumberFormatException) {
            console.log(
                listOf(
                    Console.Message(
                        "hexToBinary :: ", "#DEDEDE", 3
                    ), Console.Message(
                        "Error: received byte that is un-parsable into hex.", "#AA4040", 0
                    )
                )
            )
            return "0"
        }
        return i.toString(2)
    }

    private fun decodeStatus(status: String): List<Int> {
        val chunks = status.chunked(2)
        val doorStatus = leftPad(hexToBinary(chunks[3])).plus(leftPad(hexToBinary(chunks[4])))
            .map { c -> c.toString().toInt() }
        return listOf(
            doorStatus.subList(0, 8).reversed(), doorStatus.subList(8, doorStatus.size).reversed()
        ).flatten()
    }

    private fun readData(timeout: Int) {
        val buffer = mReadBuffer.array()
        port.read(buffer, timeout)
        onNewData(buffer)
    }

    private fun onNewData(data: ByteArray?) {
        if (data == null) {
            console.log(
                listOf(
                    Console.Message(
                        "fetchStatus :: ", "#DEDEDE", 3
                    ), Console.Message(
                        "Error: received 0 bytes while reading status.", "#AA4040", 0
                    )
                )
            )
            return
        }
        val hexString = data.joinToString("") { "%02x".format(it) }
        State.lastOutput.add(true)
        val arr = decodeStatus(hexString)
        State.lastOutput.add(true)
        if (statusToBeLogged) {
            statusToBeLogged = false
            console.log(
                listOf(
                    Console.Message(
                        "Door status :: ", "#4BB543", 3
                    ), Console.Message(
                        "[${arr.joinToString(", ")}]", "#FFFFFF", 0
                    )
                )
            )
        }
        val arrayString = "[${arr.joinToString(", ")}]"

        if (State.ip == null) {
            console.log(
                listOf(
                    Console.Message(
                        "IP not set. Printing status instead: ".plus(arrayString), "#BCBCBC", 0
                    )
                )
            )
        }
        if (State.ip != null && arr.isNotEmpty()) {
            APIClient.pushStatus(arrayString, console)
        }
    }

    fun checkStatus(): Boolean {
        return port.isOpen
    }

    init {
        port.setParameters(
            State.baudRate, State.dataBits, State.stopBits, State.parity
        )
    }
}