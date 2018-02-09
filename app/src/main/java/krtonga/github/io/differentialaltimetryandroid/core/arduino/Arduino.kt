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
import timber.log.Timber
import kotlin.text.Charsets.UTF_8


class Arduino(context: Context) {
    private val cntx: Context = context

    private val altitudesRelay = BehaviorRelay.create<List<Reading>>()
    private val altitudes = altitudesRelay.hide()

    var usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    var connectedDevice: UsbDevice? = null
    var connection: UsbDeviceConnection? = null
    var serialPort: UsbSerialDevice? = null

    private var serialPortConnected = false

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

    fun registerForBroadcasts() {
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(ACTION_USB_DETACHED)
        filter.addAction(ACTION_USB_ATTACHED)
        cntx.registerReceiver(usbReceiver, filter)
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
        if (serialPort != null) {
            if (serialPort!!.open()) {
                serialPortConnected = true
                serialPort!!.setBaudRate(BAUD_RATE)
                serialPort!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                serialPort!!.setDataBits(UsbSerialInterface.STOP_BITS_1)
                serialPort!!.setParity(UsbSerialInterface.PARITY_NONE)
                serialPort!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                serialPort!!.read { bytes ->
                    Timber.d("Bytes read! %s", String(bytes, UTF_8)) }
                serialPort!!.getCTS{ state ->
                    Timber.d("CTS State changed! Connected = %s", state)
                }
                serialPort!!.getDSR{ state ->
                    Timber.d("DSR State changed! Connected = %s", state)
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

    private fun sendBroadcast(action: String) {
        Timber.d("Sending broadcast... %s", action)
        val intent = Intent(action)
        cntx.sendBroadcast(intent)
    }

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

}

data class Reading(
        val pressure: Float,
        val temperature: Float,
        val altitude: Float
)