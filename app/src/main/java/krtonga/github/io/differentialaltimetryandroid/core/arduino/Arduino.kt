package krtonga.github.io.differentialaltimetryandroid.core.arduino

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.text.TextUtils
import com.felhr.usbserial.CDCSerialDevice
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import krtonga.github.io.differentialaltimetryandroid.core.db.ArduinoEntry
import timber.log.Timber
import java.util.*


class Arduino(context: Context, db: AppDatabase) : UsbSerialInterface.UsbReadCallback {

    private val cntx: Context = context

    private var usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var connectedDevice: UsbDevice? = null

    private var connection: UsbDeviceConnection? = null

    private var serialPort: UsbSerialDevice? = null

    private var serialPortConnected = false

    private val builder: ReadingBuilder = ReadingBuilder(db)

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                val granted = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    // User accepted our USB connection. Try to open the device as a serial port
                    sendBroadcast(ACTION_USB_PERMISSION_GRANTED)
                    try {
                        connection = usbManager.openDevice(connectedDevice)
                        connectToDevice()
                    } catch (e: Exception) {
                        sendBroadcast(ACTION_USB_PERMISSION_NOT_GRANTED)
                    }

                } else {
                    // User not accepted our USB connection. Send an Intent to the Main Activity
                    sendBroadcast(ACTION_USB_PERMISSION_NOT_GRANTED)
                }
            } else if (intent.action == ACTION_USB_ATTACHED) {
                // A USB device has been attached. Try to open it as a Serial port
                if (!serialPortConnected)
                    findSerialPortDevice()
            } else if (intent.action == ACTION_USB_DETACHED) {
                // Usb device was disconnected. send an intent to the Main Activity
                sendBroadcast(ACTION_USB_DISCONNECTED)
                if (serialPortConnected) {
                    serialPort?.close()
                }
                serialPortConnected = false
            }
        }
    }

    companion object {
        const val ACTION_NO_USB = "krtonga.github.io.NO_USB_FOUND"
        const val ACTION_USB_ATTACHED = "krtonga.github.io.ACTION_USB_ATTACHED"
        const val ACTION_USB_DETACHED = "krtonga.github.io.ACTION_USB_DETACHED"
        const val ACTION_USB_DISCONNECTED = "krtonga.github.io.ACTION_USB_DISCONNECTED"
        const val ACTION_USB_READY = "krtonga.github.io.ACTION_USB_READY"
        const val ACTION_CDC_DRIVER_NOT_WORKING = "krtonga.github.io.ACTION_CDC_DRIVER_NOT_WORKING"

        const val ACTION_USB_DEVICE_NOT_WORKING = "krtonga.github.io.ACTION_USB_DEVICE_NOT_WORKING"
        const val ACTION_USB_NOT_SUPPORTED = "krtonga.github.io.ACTION_USB_NOT_SUPPORTED"
        const val ACTION_USB_PERMISSION = "krtonga.github.io.USB_PERMISSION"

        const val ACTION_USB_PERMISSION_GRANTED = "krtonga.github.io.ACTION_USB_PERMISSION_GRANTED"
        const val ACTION_USB_PERMISSION_NOT_GRANTED = "krtonga.github.io.ACTION_USB_PERMISSION_NOT_GRANTED"

        const val ACTION_BYTES_READ = "krtonga.github.io.ACTION_BYTES_READ"
        const val INTENT_READING = "krtonga.github.io.INTENT_READING"

        const val BAUD_RATE = 9600
    }

    fun start() {
        Timber.d("Starting Arduino Connection...")

        if (serialPortConnected) {
            return
        }
        registerForBroadcasts()
        findSerialPortDevice()
    }

    fun write(data: ByteArray) {
        serialPort?.write(data)
    }

    fun stop() {
        Timber.d("Stopping Arduino Connection...")
        serialPortConnected = false
        serialPort?.close()
    }

    fun isConnected() : Boolean {
        return serialPortConnected
    }

    fun registerForBroadcasts() {
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(ACTION_USB_DETACHED)
        filter.addAction(ACTION_USB_ATTACHED)
        cntx.registerReceiver(usbReceiver, filter)
    }

    private fun sendBroadcast(action: String) {
        sendBroadcast(action, "")
    }

    private fun sendBroadcast(action: String, extra: String = "") {
        Timber.d("Sending broadcast... %s", action)
        val intent = Intent(action)
        if (!TextUtils.isEmpty(extra)) {
            Timber.d("extra... %s", extra)
            intent.putExtra(INTENT_READING, extra)
        }
        cntx.sendBroadcast(intent)
    }

    private fun findSerialPortDevice() {
        val devices: HashMap<String, UsbDevice> = usbManager.deviceList
        if (!devices.isEmpty()) {
            var keep = true
            for ((_, device) in devices) {
                val deviceVID = device.vendorId
                val devicePID = device.productId

                if (deviceVID != 0x1d6b &&
                        (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
                    // A device is connected to the USB
                    connectedDevice = device
                    requestUserPermission()
                    keep = false
                } else {
                    connection = null
                }
                if (!keep) {
                    break
                }
            }
            if (!keep) {
                // There are no USB connected devices (but USB host were listed)
                sendBroadcast(ACTION_NO_USB)
            }
        } else {
            // There is no USB devices connected
            sendBroadcast(ACTION_NO_USB)
        }
    }

    private fun requestUserPermission() {
        val pendingIntent = PendingIntent.getBroadcast(
                cntx, 0, Intent(ACTION_USB_PERMISSION), 0)
        usbManager.requestPermission(connectedDevice, pendingIntent)
    }

    // should be run off main thread
    private fun connectToDevice() {
        serialPort = UsbSerialDevice.createUsbSerialDevice(connectedDevice, connection)
        Timber.d("SerialPort: %s", serialPort)
        val serialPort = this.serialPort
        if (serialPort != null) {
            Timber.d("isOpen: %s", serialPort.open())
            if (serialPort.open()) {
                serialPortConnected = true
                serialPort.setBaudRate(BAUD_RATE)
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8)
                serialPort.setDataBits(UsbSerialInterface.STOP_BITS_1)
                serialPort.setParity(UsbSerialInterface.PARITY_NONE)
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                serialPort.read(this)

                serialPort.getCTS { state ->
                    Timber.d("CTS State changed! Connected = %s", state)
                    sendBroadcast(ACTION_BYTES_READ, "State=" + state)
                }
                serialPort.getDSR { state ->
                    Timber.d("DSR State changed! Connected = %s", state)
                    sendBroadcast(ACTION_BYTES_READ, "State=" + state)
                }

                // Everything went as expected. Inform Main Thread
                sendBroadcast(ACTION_USB_READY)
            } else {
                // Serial port could not be opened
                if (serialPort is CDCSerialDevice) {
                    sendBroadcast(ACTION_CDC_DRIVER_NOT_WORKING)
                } else {
                    sendBroadcast(ACTION_USB_DEVICE_NOT_WORKING)
                }
            }
        } else {
            // No driver for given device
            sendBroadcast(ACTION_USB_NOT_SUPPORTED)
        }

    }

    override fun onReceivedData(bytes: ByteArray?) {
        if (bytes != null && bytes.isNotEmpty()) {
            builder.addBytes(bytes.copyOf())
        }
    }
}

class ReadingBuilder(db: AppDatabase) {
    var nextReading = StringBuilder()
    var database = db

    fun addBytes(bytes: ByteArray) {
        // TODO: Consider replacing with ByteStream, and reading new lines?
//        val outputStream = PipedOutputStream()
//        outputStream.write(bytes)
//
//        val inputStream = PipedInputStream(outputStream)
//        val reader = InputStreamReader(inputStream).readLines()
//        for (line in reader) {
//
//        }

        // OBSERVATBLE COMBINELATEST WITH LOCATION STREAM


        if (bytes.isNotEmpty()) {
            val byteString = String(bytes, Charsets.UTF_8)
            if (byteString.contains("\n")) {
                val splitAtNewline = byteString.split("\n")

                for ((i, value) in splitAtNewline.withIndex()) {
                    if (i < splitAtNewline.size - 1) {
                        nextReading.append(value)
                        createArduinoEntry()
                    } else {
                        nextReading.append(value)
                    }
                }
            } else {
                nextReading.append(byteString)
            }
        }
    }

    private fun createArduinoEntry() {
        val arduinoString = nextReading.toString()
        log(arduinoString + "    V:" + isValidReading(arduinoString))
        if (isValidReading(arduinoString)) {
            addDBEntry(arduinoString)
        }
        nextReading.setLength(0)
    }

    private fun isValidReading(reading: String): Boolean {
        // ensures that first part of line is +/- float
        return reading.trim().matches(Regex("-*\\d+.\\d{2}.+"))
    }

    private fun addDBEntry(reading: String): ArduinoEntry {
        val entryArray: List<String> = reading.split(",")

        val entry = ArduinoEntry(arTemperature = entryArray[0].toFloat(),
                arPressure = entryArray[1].toFloat(),
                arAltitude = entryArray[2].toFloat())

        database.entryDoa().insert(entry)
        return entry
    }

    private fun log(log: String) {
        Observable.just(log)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.d(it)
                })
    }
}
