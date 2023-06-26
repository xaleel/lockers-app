package ge.edison.lockers

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
import ge.edison.lockers.databinding.FragmentSettingsBinding

class Settings : Fragment() {

    internal class ListItem(var device: UsbDevice, var driver: UsbSerialDriver?)

    private var _binding: FragmentSettingsBinding? = null
    private val listItems: ArrayList<ListItem> = ArrayList()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        var source = this.context
        if (source == null) {
            source = this.activity
        }
        if (source != null) {
            this.refresh()
            val text = ""
            if (listItems.isEmpty()) {
                text.plus("No devices detected")
            } else {
                for (item in listItems) {
                    val driver = item.driver
                    val device = item.device
                    text.plus("Device: ")
                        .plus(device.deviceName)
                        .plus("\n")
                    if (driver != null) {
                        text.plus("Ports: \n")
                        val ports = driver.ports
                        ports.forEachIndexed { index, usbSerialPort ->
                            text.plus(index)
                                .plus(".\n")
                                .plus("\tportNumber: ")
                                .plus(usbSerialPort.portNumber)
                                .plus("\n\tserial: ")
                                .plus(usbSerialPort.serial)
                        }
                    } else {
                        text.plus("No driver")
                    }
                }
            }

            binding.textviewSecond.text = text
        }
        return binding.root
    }

    private fun refresh() {
        var source = this.context
        if (source == null) {
            source = this.activity
        }
        if (source == null) {
            return
        }
        val usbManager = source.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
        val usbDefaultProber = UsbSerialProber.getDefaultProber()
        listItems.clear()
        for (device in usbManager.deviceList.values) {
            val driver = usbDefaultProber.probeDevice(device)
            if (driver != null) {
                listItems.add(ListItem(device, driver))
            } else {
                listItems.add(ListItem(device, null))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}