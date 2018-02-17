package krtonga.github.io.differentialaltimetryandroid.core.arduino

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.felhr.usbserial.CDCSerialDevice
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.jakewharton.rxrelay2.BehaviorRelay
import krtonga.github.io.differentialaltimetryandroid.R
import krtonga.github.io.differentialaltimetryandroid.core.db.AppDatabase
import timber.log.Timber
import java.util.*


class Arduino(context: Context, db: AppDatabase) : UsbSerialInterface.UsbReadCallback {

    private val cntx: Context = context

    private val arduinoStateRelay = BehaviorRelay.createDefault(
            ArduinoState(false, false,""))
    val arduinoState = arduinoStateRelay.hide()

    private var usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var connectedDevice: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null

    private var serialPort: UsbSerialDevice? = null

    private val builder: ArduinoEntryBuilder = ArduinoEntryBuilder(db)

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                val granted = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    try {
                        connection = usbManager.openDevice(connectedDevice)
                        connectToDevice()
                    } catch (e: Exception) {
                        postDisconnect(cntx.getString(R.string.error_no_permission))
                    }
                } else {
                    postDisconnect(cntx.getString(R.string.error_no_permission))
                }
            }
            else if (intent.action == ACTION_USB_ATTACHED) {
                // A USB device has been attached. Try to open it as a Serial port
                if (!isConnected())
                    findSerialPortDevice()
            }
            else if (intent.action == ACTION_USB_DETACHED) {
                if (arduinoStateRelay.value.isConnected) {
                    serialPort?.close()
                }
                postDisconnect(cntx.getString(R.string.error_usb_disconnect))
            }
        }
    }

    companion object {
        const val ACTION_USB_ATTACHED = "krtonga.github.io.ACTION_USB_ATTACHED"
        const val ACTION_USB_DETACHED = "krtonga.github.io.ACTION_USB_DETACHED"
        const val ACTION_CDC_DRIVER_NOT_WORKING = "krtonga.github.io.ACTION_CDC_DRIVER_NOT_WORKING"
        const val ACTION_USB_PERMISSION = "krtonga.github.io.USB_PERMISSION"

        const val BAUD_RATE = 9600
    }

    fun start() {
        Timber.d("Starting Arduino Connection...")

        if (isConnected()) {
            return
        }
        registerForBroadcasts()

        postConnecting()
        findSerialPortDevice()
    }

    fun write(data: ByteArray) {
        serialPort?.write(data)
    }

    fun stop() {
        Timber.d("Stopping Arduino Connection...")
        serialPort?.close()
        postDisconnect(cntx.getString(R.string.arduino_stopped))
    }

    fun isConnected() : Boolean {
        return arduinoStateRelay.value.isConnected
    }

    private fun registerForBroadcasts() {
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(ACTION_USB_DETACHED)
        filter.addAction(ACTION_USB_ATTACHED)
        cntx.registerReceiver(usbReceiver, filter)
    }

    private fun sendBroadcast(action: String) {
        Timber.d("Sending broadcast... %s", action)
        val intent = Intent(action)
        cntx.sendBroadcast(intent)
    }

    private fun findSerialPortDevice() {
        val devices: HashMap<String, UsbDevice> = usbManager.deviceList
        if (!devices.isEmpty()) {
            for ((_, device) in devices) {
                val deviceVID = device.vendorId
                val devicePID = device.productId

                if (deviceVID != 0x1d6b &&
                        (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
                    // A device is connected to the USB
                    connectedDevice = device
                    requestUserPermission()
                    break
                } else {
                    connection = null
                }
            }
            if (connectedDevice == null) {
                // There are no USB connected devices (but USB host were listed)
                postDisconnect(cntx.getString(R.string.error_no_usb_connected))
            }
        } else {
            // There is no USB devices connected
            postDisconnect(cntx.getString(R.string.error_no_usb_connected))
        }
    }

    private fun requestUserPermission() {
        val pendingIntent = PendingIntent.getBroadcast(
                cntx, 0, Intent(ACTION_USB_PERMISSION), 0)
        usbManager.requestPermission(connectedDevice, pendingIntent)
    }

    // should be run off main thread??
    private fun connectToDevice() {
        serialPort = UsbSerialDevice.createUsbSerialDevice(connectedDevice, connection)
        Timber.d("SerialPort: %s", serialPort)
        val serialPort = this.serialPort
        if (serialPort != null) {
            Timber.d("isOpen: %s", serialPort.open())
            if (serialPort.open()) {
                postConnected()
                serialPort.setBaudRate(BAUD_RATE)
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8)
                serialPort.setDataBits(UsbSerialInterface.STOP_BITS_1)
                serialPort.setParity(UsbSerialInterface.PARITY_NONE)
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                serialPort.read(this)

                serialPort.getCTS { state ->
                    Timber.d("CTS State changed! Connected = %s", state)
                    if (state) postConnected()
                    else postDisconnect(cntx.getString(R.string.error_cts_disconnect))
                }
                serialPort.getDSR { state ->
                    Timber.d("DSR State changed! Connected = %s", state)
                    if (state) postConnected()
                    else postDisconnect(cntx.getString(R.string.error_dsr_disconnect))
                }

                // Everything went as expected. Inform Main Thread
                postConnected()
            } else {
                // Serial port could not be opened
                if (serialPort is CDCSerialDevice) {
                    postDisconnect(cntx.getString(R.string.error_cdc_driver_error))
                    sendBroadcast(ACTION_CDC_DRIVER_NOT_WORKING)
                } else {
                    postDisconnect(cntx.getString(R.string.error_device_error))
                }
            }
        } else {
            // No driver for given device
            postDisconnect(cntx.getString(R.string.error_usb_not_supported))
        }
    }

    private fun postConnecting() {
        arduinoStateRelay.accept(ArduinoState(false, true, ""))
    }

    private fun postConnected() {
        Timber.d("Arduino connection successful.\n")
        arduinoStateRelay.accept(ArduinoState(true, false, ""))
    }

    private fun postDisconnect(message: String) {
        Timber.d("%s\n",message)
        arduinoStateRelay.accept(ArduinoState(false, false, message))
    }

    override fun onReceivedData(bytes: ByteArray?) {
        if (bytes != null && bytes.isNotEmpty()) {
            builder.addBytes(bytes.copyOf())
        }
    }
}