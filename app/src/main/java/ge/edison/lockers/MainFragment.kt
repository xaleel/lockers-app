package ge.edison.lockers

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.hardware.usb.UsbManager
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import ge.edison.lockers.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private var _console: Console? = null
    private val console get() = _console!!
    private var _contextSource: Context? = null
    private val contextSource get() = _contextSource!!
    private var _messenger: Messenger? = null
    private val messenger get() = _messenger!!
    private var serviceMessenger: Messenger? = null
    private var bound = false

    private val incomingHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.obj is List<*>) {
                console.log(msg.obj as List<Console.Message>)
            } else {
                attemptApply()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        _console = Console(this.context ?: this.activity, binding)
        _contextSource = context ?: activity
        _messenger = Messenger(incomingHandler)

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(contextSource))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initInputs()
        refresh()

        binding.refreshButton.setOnClickListener {
            refresh()
        }

        binding.status.setOnClickListener {
            if (State.connectionHandler != null) {
                State.connectionHandler!!.statusToBeLogged = true
            }
        }

        binding.open.setOnClickListener {
            if (State.connectionHandler != null) {
                State.connectionHandler!!.openDoor(0)
            }
        }

        val intent = Intent(activity, BackgroundService::class.java)

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                serviceMessenger = Messenger(service)
                val message = Message()
                message.obj = console
                message.replyTo = messenger
                serviceMessenger?.send(message)
                bound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                serviceMessenger = null
                bound = false
            }
        }

        activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    @SuppressLint("SetTextI18n")
    private fun refresh() {
        console.log(listOf(Console.Message("Refreshing..", "#FFFFFF", 0)))

        val usbManager: UsbManager? =
            contextSource.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager?
        if (usbManager == null) {
            console.log(listOf(Console.Message("Error: usbManager is null.", "#AA4040", 0)))
            return
        }
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            console.log(listOf(Console.Message("No available drivers detected.", "#FF6700", 0)))
            return
        }
        console.log(
            listOf(
                Console.Message(
                    "Found ".plus(availableDrivers.size.toString()).plus(" driver(s)."),
                    "#DDDDEE",
                    0
                )
            )
        )

        val linearLayout = binding.connectionsContainer
        linearLayout.removeAllViews()

        State.connections.forEach { connection -> connection.close() }
        State.connections = emptyList()

        availableDrivers.forEachIndexed { index, driver ->
            val connection = usbManager.openDevice(driver.device)
            if (connection == null) {
                renderText(
                    contextSource,
                    linearLayout,
                    (index + 1).toString().plus(". Couldn't connect to device"),
                    "#DF4759",
                    false,
                    null
                )
                return@forEachIndexed
            }
            val port = driver.ports[0] // Most devices have just one port (port 0)
            try {
                port.open(connection)
                State.connections = State.connections.plus(connection)
                renderText(
                    contextSource,
                    linearLayout,
                    (index + 1).toString().plus(". Connected"),
                    "#42ba96",
                    true,
                    port
                )
                State.connectionHandler = SerialUtil(port, console)
            } catch (e: Exception) {
                renderText(
                    contextSource, linearLayout, (index + 1).toString().plus(
                        "Couldn't open a connection to device port.\nPort serial: ".plus(
                            driver.ports[0].serial ?: "None"
                        )
                    ), "#DF4759", false, null
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun renderText(
        contextSource: Context,
        layout: LinearLayout,
        text: String,
        color: String,
        addButton: Boolean,
        port: UsbSerialPort?
    ) {
        val textView = TextView(contextSource)
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textView.text = text
        textView.setTextColor(Color.parseColor(color))
        textView.setPadding(10, 0, 20, 10)

        val horizontalLayout = LinearLayout(contextSource)
        horizontalLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        horizontalLayout.orientation = LinearLayout.HORIZONTAL
        horizontalLayout.addView(textView)


        val rand = (10000000..99999999).random()
        if (addButton && port != null) {
            val button = Button(contextSource)
            button.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            button.id = rand
            button.setBackgroundColor(Color.parseColor("#D2AF18"))
            button.setTextColor(Color.parseColor("#FFFFFF"))
            button.setPadding(0, 0, 0, 0)
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.0F)
            button.setOnClickListener {
                State.connectionHandler = SerialUtil(port, console)
            }
            horizontalLayout.addView(button)
        }

        layout.addView(horizontalLayout)
    }

    private fun initInputs() {
        binding.inputBaudRate.setText(State.baudRate.toString())
        binding.inputDataBits.setText(State.dataBits.toString())
        binding.inputStopBits.setText(State.stopBits.toString())
        binding.inputParity.setText(
            when (State.parity) {
                0 -> "none"
                1 -> "odd"
                2 -> "even"
                3 -> "mark"
                4 -> "space"
                else -> "none"
            }
        )
        binding.inputIp.setText(State.ip)

        binding.applyButton.setOnClickListener { attemptApply() }
    }

    private fun attemptApply() {
        val baudRateText = binding.inputBaudRate.text
        val baudRate: Int
        try {
            baudRate = baudRateText.toString().toInt()
        } catch (e: NumberFormatException) {
            displayParamsError("baudRateNotInt")
            return
        }

        val dataBitsText = binding.inputDataBits.text
        val dataBits: Int
        try {
            dataBits = dataBitsText.toString().toInt()
        } catch (e: Exception) {
            displayParamsError("dataBitsNotInt")
            return
        }
        if (dataBits != State.DATA_BITS_5 && dataBits != State.DATA_BITS_6 && dataBits != State.DATA_BITS_7 && dataBits != State.DATA_BITS_8) {
            displayParamsError("dataBitsInvalid")
            return
        }

        val stopBitsText = binding.inputStopBits.text
        var stopBits: Int?
        try {
            stopBits = stopBitsText.toString().toInt()
        } catch (e: Exception) {
            displayParamsError("stopBitsNotInt")
            return
        }
        stopBits = when (stopBits) {
            1 -> State.STOP_BITS_1
            2 -> State.STOP_BITS_2
            3 -> State.STOP_BITS_1_5
            else -> null
        }
        if (stopBits == null) {
            displayParamsError("stopBitsInvalid")
            return
        }

        val parityText = binding.inputParity.text.toString()
        val parity = when (parityText.lowercase()) {
            "none" -> State.PARITY_NONE
            "odd" -> State.PARITY_ODD
            "even" -> State.PARITY_EVEN
            "mark" -> State.PARITY_MARK
            "space" -> State.PARITY_SPACE
            else -> null
        }
        if (parity == null) {
            displayParamsError("parityInvalid")
            return
        }

        val ip = binding.inputIp.text.toString()
        if (!"""192\.168\.\d+\.\d{3}""".toRegex().matches(ip)) {
            displayParamsError("IP address invalid")
            return
        }

        State.baudRate = baudRate
        State.dataBits = dataBits
        State.stopBits = stopBits
        State.parity = parity
        State.ip = ip
        APIClient.disconnected = 0
        displayParamsError("success")
        val saved = FileUtil.saveState(contextSource)
        displayParamsError("saved_$saved")
    }

    private fun displayParamsError(param: String) {
        val messages = HashMap<String, String>()
        messages["baudRateNotInt"] = "Error: baud rate is not a number."
        messages["dataBitsNotInt"] = "Error: data bits is not a number."
        messages["dataBitsInvalid"] = "Error: data bits should be in [5, 6, 7, 8]."
        messages["stopBitsNotInt"] = "Error: stop bits is not a number."
        messages["stopBitsInvalid"] = "Error: stop bits should be in [1, 2, 3]."
        messages["parityInvalid"] =
            "Error: stop bits should be in ['none', 'odd', 'even', 'mark', 'space']."
        messages["success"] = "Successfully updated params."
        messages["saved_true"] = "Successfully saved default params."
        messages["saved_false"] = "Failed to save default params."

        val color = if (param == "success" || param == "saved_true") "#4BB543" else "#DF4759"
        console.log(listOf(Console.Message(messages[param] ?: "", color, 0)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        State.connections.forEach { connection -> connection.close() }
    }
}