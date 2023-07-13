package ge.edison.lockers

import android.hardware.usb.UsbDeviceConnection
import androidx.annotation.IntDef
import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlin.annotation.Retention
import kotlin.annotation.AnnotationRetention


object State {

    @IntDef(DATA_BITS_5, DATA_BITS_6, DATA_BITS_7, DATA_BITS_8)
    @Retention(AnnotationRetention.SOURCE)
    annotation class DataBits
    const val DATA_BITS_5 = 5
    const val DATA_BITS_6 = 6
    const val DATA_BITS_7 = 7
    const val DATA_BITS_8 = 8

    @IntDef(STOP_BITS_1, STOP_BITS_1_5, STOP_BITS_2)
    @Retention(AnnotationRetention.SOURCE)
    annotation class StopBits
    const val STOP_BITS_1 = UsbSerialPort.STOPBITS_1
    const val STOP_BITS_1_5 = UsbSerialPort.STOPBITS_1_5
    const val STOP_BITS_2 = UsbSerialPort.STOPBITS_2

    @IntDef(PARITY_NONE, PARITY_ODD, PARITY_EVEN, PARITY_MARK, PARITY_SPACE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Parity
    const val PARITY_NONE = 0
    const val PARITY_ODD = 1
    const val PARITY_EVEN = 2
    const val PARITY_MARK = 3
    const val PARITY_SPACE = 4

    var baudRate: Int = 19200
    var dataBits: Int = DATA_BITS_8
    var stopBits: Int = STOP_BITS_1
    var parity: Int = PARITY_NONE
    var connections: List<UsbDeviceConnection> = emptyList()
    var connectionHandler: SerialUtil? = null
    val lastOutput: MutableList<Boolean> = mutableListOf()
    var timeout: Int = 500 // millisecond
    var toBeDisconnected: Boolean = false
    var ip: String? = "127.0.0.1"
}