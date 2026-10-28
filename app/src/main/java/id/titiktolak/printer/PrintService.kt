package id.titiktolak.printer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.UUID
import java.io.OutputStream

class PrintService : Service() {
    private val tag = "TitikTolakPrinter"
    private val channelId = "printer_service"
    private val worker = ThreadLocal<Boolean>()
    @Volatile private var running = false
    @Volatile private var apiUrl = ""
    @Volatile private var printerMac = ""
    private var loopThread: Thread? = null
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(7, notification("Menyiapkan layanan cetak…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        apiUrl = intent?.getStringExtra("api_url") ?: getSharedPreferences("unused", 0).getString("api_url", "") ?: ""
        printerMac = intent?.getStringExtra("printer_mac") ?: ""
        if (!running) {
            running = true
            loopThread = Thread { pollLoop() }.apply { name = "OrderPoller"; start() }
        }
        return START_STICKY
    }

    private fun pollLoop() {
        while (running) {
            try {
                updateNotification("Memeriksa pesanan…")
                val orders = fetchOrders()
                for (order in orders) {
                    if (!running) break
                    val id = orderId(order)
                    if (id.isBlank() || alreadyPrinted(id)) continue
                    updateNotification("Mencetak pesanan #$id")
                    printOrder(order, id)
                    rememberPrinted(id)
                    updateNotification("Berhasil mencetak #$id")
                }
            } catch (e: Exception) {
                Log.e(tag, "Polling/cetak gagal", e)
                updateNotification("Gagal: ${e.message ?: "periksa koneksi"}")
            }
            try { Thread.sleep(5000) } catch (_: InterruptedException) { }
        }
    }

    private fun fetchOrders(): List<JSONObject> {
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection)
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (code !in 200..299) throw Exception("API HTTP $code: ${body.take(120)}")
            val trimmed = body.trim()
            val array = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val obj = JSONObject(trimmed)
                    when {
                        obj.opt("orders") is JSONArray -> obj.getJSONArray("orders")
                        obj.opt("data") is JSONArray -> obj.getJSONArray("data")
                        else -> throw Exception("Format API harus array atau {orders:[...]}/{data:[...]}")
                    }
                }
                else -> throw Exception("Respons API bukan JSON")
            }
            val out = mutableListOf<JSONObject>()
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val status = o.optString("status", "pending").lowercase()
                if (status == "pending" || status == "new" || status == "unprinted" || !o.has("status"))
                    out.add(o)
            }
            return out
        } finally { conn.disconnect() }
    }

    private fun orderId(o: JSONObject): String =
        o.optString("id", "").ifBlank { o.optString("order_id", "") }
            .ifBlank { o.optString("number", "") }

    private fun alreadyPrinted(id: String): Boolean =
        getSharedPreferences("printed_orders", 0).getBoolean("id_$id", false)

    private fun rememberPrinted(id: String) {
        getSharedPreferences("printed_orders", 0).edit().putBoolean("id_$id", true).apply()
    }

    private fun printOrder(o: JSONObject, id: String) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: throw Exception("Bluetooth tidak tersedia")
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            throw Exception("Izin Bluetooth belum diberikan")
        if (!adapter.isEnabled) throw Exception("Aktifkan Bluetooth Android")
        val device: BluetoothDevice = adapter.getRemoteDevice(printerMac)
        val socket = device.createRfcommSocketToServiceRecord(sppUuid)
        adapter.cancelDiscovery()
        socket.connect()
        try {
            val output = socket.outputStream
            output.write(buildTspl(o, id).toByteArray(Charset.forName("GB18030")))
            output.flush()
            Thread.sleep(350)
        } finally { socket.close() }
    }

    private fun buildTspl(o: JSONObject, id: String): String {
        val customer = firstString(o, "customer", "customer_name", "name", "pelanggan").ifBlank { "Pelanggan" }
        val created = firstString(o, "created_at", "date", "time")
        val items = when {
            o.opt("items") is JSONArray -> o.getJSONArray("items")
            o.opt("details") is JSONArray -> o.getJSONArray("details")
            else -> JSONArray()
        }
        val lines = mutableListOf<String>()
        lines += "SIZE 80 mm,100 mm"
        lines += "GAP 2 mm,0 mm"
        lines += "DIRECTION 1"
        lines += "CLS"
        lines += """TEXT 30,25,"3",0,1,1,"TITIKTOLAK""""
        lines += """TEXT 30,70,"3",0,1,1,"PESANAN #${safe(id)}""""
        lines += """TEXT 30,110,"2",0,1,1,"Nama: ${safe(customer)}""""
        if (created.isNotBlank()) lines += """TEXT 30,145,"2",0,1,1,"${safe(created)}""""
        lines += """TEXT 30,185,"2",0,1,1,"------------------------------""""
        var y = 220
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val name = firstString(item, "name", "product", "item", "nama").ifBlank { "Item" }
            val qty = firstString(item, "qty", "quantity", "jumlah").ifBlank { "1" }
            val price = firstString(item, "price", "subtotal", "harga")
            lines += """TEXT 30,$y,"2",0,1,1,"${safe(name).take(26)} x${safe(qty)}""""
            y += 32
            if (price.isNotBlank()) {
                lines += """TEXT 50,$y,"1",0,1,1,"${safe(price)}""""
                y += 27
            }
            if (y > 850) break
        }
        val total = firstString(o, "total", "grand_total", "amount")
        if (total.isNotBlank()) {
            lines += """TEXT 30,$y,"2",0,1,1,"TOTAL: ${safe(total)}""""
            y += 40
        }
        lines += """TEXT 30,$y,"2",0,1,1,"Terima kasih""""
        lines += "PRINT 1,1"
        return lines.joinToString("\r\n") + "\r\n"
    }

    private fun firstString(o: JSONObject, vararg keys: String): String {
        for (key in keys) {
            if (o.has(key) && !o.isNull(key)) {
                val v = o.opt(key)
                if (v is String && v.isNotBlank()) return v
                if (v is Number) return v.toString()
            }
        }
        return ""
    }

    private fun safe(s: String): String =
        s.replace("\"", "'").replace("\r", " ").replace("\n", " ")
            .replace(",", " ").filter { it.code in 32..126 || it.code >= 160 }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(channelId, "Cetak pesanan", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notification(message: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channelId) else Notification.Builder(this)
        return builder.setContentTitle("TitikTolak Printer")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true).build()
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java).notify(7, notification(message))
    }

    override fun onDestroy() {
        running = false
        loopThread?.interrupt()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
