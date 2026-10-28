package id.titiktolak.printer

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import android.graphics.Typeface

class MainActivity : Activity() {
    private lateinit var apiInput: EditText
    private lateinit var macInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            layoutParams = ViewGroup.LayoutParams(-1, -1)
        }
        root.addView(TextView(this).apply {
            text = "TitikTolak Printer"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "Cetak otomatis pesanan website ke printer Bluetooth."
            textSize = 14f
            setPadding(0, 8, 0, 20)
        })
        root.addView(TextView(this).apply { text = "URL API pesanan"; textSize = 14f })
        apiInput = EditText(this).apply {
            hint = "https://titiktolak.com/printer-test/api.php"
            setSingleLine(true)
            inputType =  android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(getPreferences(0).getString("api_url", "https://titiktolak.com/printer-test/api.php"))
        }
        root.addView(apiInput)
        root.addView(TextView(this).apply {
            text = "Alamat MAC printer Bluetooth"
            textSize = 14f
            setPadding(0, 12, 0, 0)
        })
        macInput = EditText(this).apply {
            hint = "Contoh: AA:BB:CC:11:22:33"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setText(getPreferences(0).getString("printer_mac", ""))
        }
        root.addView(macInput)
        val paired = Button(this).apply {
            text = "Lihat printer yang sudah dipasangkan"
            setOnClickListener { showPairedDevices() }
        }
        root.addView(paired)
        val start = Button(this).apply {
            text = "MULAI CETAK OTOMATIS"
            setOnClickListener { startPrinting() }
        }
        root.addView(start)
        val stop = Button(this).apply {
            text = "HENTIKAN"
            setOnClickListener {
                stopService(Intent(this@MainActivity, PrintService::class.java))
                status.text = "Layanan dihentikan."
            }
        }
        root.addView(stop)
        status = TextView(this).apply {
            text = "Status: belum berjalan"
            textSize = 15f
            setPadding(0, 18, 0, 0)
        }
        root.addView(status)
        root.addView(TextView(this).apply {
            text = "Catatan: versi awal memakai perintah TSPL dan ukuran label sementara 80 × 100 mm. Kita sesuaikan setelah tes pertama."
            textSize = 12f
            setPadding(0, 18, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()
    
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.BLUETOOTH_SCAN)
            }
    
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    
        if (needed.isNotEmpty()) {
            requestPermissions(needed.toTypedArray(), 101)
        }
    }

    private fun showPairedDevices() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                Toast.makeText(this, "Bluetooth tidak tersedia.", Toast.LENGTH_LONG).show()
                return
            }
            val devices = adapter.bondedDevices.toList()
            if (devices.isEmpty()) {
                Toast.makeText(this, "Pasangkan printer dahulu di Setelan Bluetooth Android.", Toast.LENGTH_LONG).show()
                return
            }
            val labels = devices.map { "${it.name ?: "Printer"} — ${it.address}" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Pilih perangkat")
                .setItems(labels) { _, which -> macInput.setText(devices[which].address) }
                .setNegativeButton("Tutup", null)
                .show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Izinkan akses perangkat Bluetooth lalu coba lagi.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startPrinting() {
        val api = apiInput.text.toString().trim()
        val mac = macInput.text.toString().trim()
        if (!api.startsWith("https://") && !api.startsWith("http://")) {
            Toast.makeText(this, "Masukkan URL API yang valid.", Toast.LENGTH_LONG).show(); return
        }
        if (!mac.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))) {
            Toast.makeText(this, "Pilih printer dari daftar perangkat yang dipasangkan.", Toast.LENGTH_LONG).show(); return
        }
        getPreferences(0).edit().putString("api_url", api).putString("printer_mac", mac).apply()
        val i = Intent(this, PrintService::class.java).putExtra("api_url", api).putExtra("printer_mac", mac)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        status.text = "Status: layanan dimulai. Lihat notifikasi untuk status."
        Toast.makeText(this, "Layanan cetak dimulai.", Toast.LENGTH_LONG).show()
    }
}
