package id.titiktolak.printer

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var apiInput: EditText
    private lateinit var macInput: EditText
    private lateinit var status: TextView
    private lateinit var statusCard: LinearLayout

    private val ink = Color.rgb(25, 35, 52)
    private val muted = Color.rgb(102, 115, 133)
    private val blue = Color.rgb(37, 99, 235)
    private val page = Color.rgb(245, 247, 251)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNeededPermissions()

        val pageRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(page)
            layoutParams = ViewGroup.LayoutParams(-1, -1)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(26), dp(24), dp(26))
            background = headerBackground(blue, 24)
        }
        header.addView(TextView(this).apply {
            text = "PRINTER THERMAL BRIDGE"
            textSize = 11f
            letterSpacing = .12f
            setTextColor(Color.rgb(219, 232, 255))
            typeface = Typeface.DEFAULT_BOLD
        })
        header.addView(TextView(this).apply {
            text = "Cetak pesanan,\nlebih praktis."
            textSize = 27f
            setLineSpacing(dp(2).toFloat(), 1f)
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, dp(10), 0, dp(6))
        })
        header.addView(TextView(this).apply {
            text = "Hubungkan API toko ke printer thermal Bluetooth secara otomatis."
            textSize = 14f
            setTextColor(Color.rgb(232, 239, 255))
        })
        pageRoot.addView(header, LinearLayout.LayoutParams(-1, -2))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
        }

        val connectionCard = card()
        connectionCard.addView(sectionTitle("Koneksi perangkat", "Atur sumber pesanan dan printer yang digunakan."))
        connectionCard.addView(fieldLabel("URL API PESANAN"))
        apiInput = EditText(this).apply {
            hint = "https://alamat-toko.com/api.php"
            textSize = 14f
            singleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(getPreferences(0).getString("api_url", "https://titiktolak.com/printer-test/api.php"))
            setTextColor(ink)
            setHintTextColor(Color.rgb(151, 161, 176))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(248, 250, 253), Color.rgb(222, 228, 237), 10)
        }
        connectionCard.addView(apiInput, matchWrap())

        connectionCard.addView(fieldLabel("PRINTER BLUETOOTH"))
        macInput = EditText(this).apply {
            hint = "Pilih printer dari daftar perangkat"
            textSize = 14f
            singleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setText(getPreferences(0).getString("printer_mac", ""))
            setTextColor(ink)
            setHintTextColor(Color.rgb(151, 161, 176))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(248, 250, 253), Color.rgb(222, 228, 237), 10)
        }
        connectionCard.addView(macInput, matchWrap())
        val paired = actionButton("Pilih perangkat yang dipasangkan", Color.rgb(236, 242, 255), blue) {
            showPairedDevices()
        }
        connectionCard.addView(paired, buttonParams(top = 14))
        content.addView(connectionCard, matchWrap(bottom = 14))

        statusCard = card().apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(Color.rgb(239, 246, 255), Color.rgb(207, 225, 255), 16)
        }
        statusCard.addView(TextView(this).apply {
            text = "STATUS LAYANAN"
            textSize = 11f
            letterSpacing = .08f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(blue)
        })
        status = TextView(this).apply {
            text = "Belum berjalan"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            setPadding(0, dp(7), 0, dp(3))
        }
        statusCard.addView(status)
        statusCard.addView(TextView(this).apply {
            text = "Polling target 1 detik. API dijeda jika kertas habis atau status printer tidak terbaca."
            textSize = 12f
            setTextColor(muted)
        })
        content.addView(statusCard, matchWrap(bottom = 14))

        val start = actionButton("▶   MULAI CETAK OTOMATIS", blue, Color.WHITE) { startPrinting() }
        content.addView(start, buttonParams(top = 0, bottom = 10, height = 54))
        val stop = actionButton("Hentikan layanan", Color.WHITE, ink, Color.rgb(218, 224, 233)) {
            stopService(Intent(this@MainActivity, PrintService::class.java))
            setStatus("Layanan dihentikan.", false)
        }
        content.addView(stop, buttonParams(height = 48))

        content.addView(TextView(this).apply {
            text = "INFO CETAK"
            textSize = 11f
            letterSpacing = .08f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(muted)
            setPadding(dp(2), dp(22), 0, dp(7))
        })
        content.addView(TextView(this).apply {
            text = "Format printer: TSPL  •  Ukuran label sementara: 80 × 100 mm\nPastikan printer sudah dipasangkan melalui Setelan Bluetooth Android. Jika status printer tidak didukung, polling API akan dijeda demi mencegah pesanan terambil saat kondisi printer tidak diketahui."
            textSize = 12f
            setTextColor(muted)
            setLineSpacing(dp(3).toFloat(), 1f)
            background = rounded(Color.WHITE, Color.rgb(229, 233, 240), 12)
            setPadding(dp(14), dp(13), dp(14), dp(13))
        }, matchWrap())

        pageRoot.addView(ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(pageRoot)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(Color.WHITE, Color.rgb(231, 235, 242), 16)
    }

    private fun sectionTitle(title: String, subtitle: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(this@MainActivity).apply {
            text = title
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        })
        addView(TextView(this@MainActivity).apply {
            text = subtitle
            textSize = 12f
            setTextColor(muted)
            setPadding(0, dp(5), 0, dp(18))
        })
    }

    private fun fieldLabel(value: String): TextView = TextView(this).apply {
        text = value
        textSize = 10f
        letterSpacing = .07f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(muted)
        setPadding(dp(1), dp(8), 0, dp(7))
    }

    private fun actionButton(
        label: String,
        fill: Int,
        textColor: Int,
        stroke: Int = fill,
        action: () -> Unit
    ): TextView = TextView(this).apply {
        text = label
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(textColor)
        background = rounded(fill, stroke, 12)
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun rounded(fill: Int, stroke: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (fill != stroke) setStroke(dp(1), stroke)
    }

    private fun headerBackground(fill: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, dp(radius).toFloat(), dp(radius).toFloat(), dp(radius).toFloat(), dp(radius).toFloat())
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun matchWrap(bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply {
        if (bottom > 0) bottomMargin = dp(bottom)
    }
    private fun buttonParams(top: Int = 0, bottom: Int = 0, height: Int = 48) = LinearLayout.LayoutParams(-1, dp(height)).apply {
        topMargin = dp(top)
        bottomMargin = dp(bottom)
    }

    private fun requestNeededPermissions() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.BLUETOOTH_CONNECT)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 101)
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
                .setTitle("Pilih perangkat Bluetooth")
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
            Toast.makeText(this, "Masukkan URL API yang valid.", Toast.LENGTH_LONG).show()
            return
        }
        if (!mac.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))) {
            Toast.makeText(this, "Pilih printer dari daftar perangkat yang dipasangkan.", Toast.LENGTH_LONG).show()
            return
        }
        getPreferences(0).edit().putString("api_url", api).putString("printer_mac", mac).apply()
        val i = Intent(this, PrintService::class.java).putExtra("api_url", api).putExtra("printer_mac", mac)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        setStatus("Layanan cetak sedang berjalan.", true)
        Toast.makeText(this, "Layanan cetak dimulai.", Toast.LENGTH_LONG).show()
    }

    private fun setStatus(message: String, running: Boolean) {
        status.text = message
        statusCard.background = rounded(
            if (running) Color.rgb(236, 253, 245) else Color.rgb(248, 250, 252),
            if (running) Color.rgb(167, 243, 208) else Color.rgb(226, 232, 240), 16
        )
        status.setTextColor(if (running) Color.rgb(5, 122, 85) else ink)
    }
}
