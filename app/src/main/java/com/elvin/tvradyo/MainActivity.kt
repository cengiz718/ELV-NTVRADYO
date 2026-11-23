package com.elvin.tvradyo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : AppCompatActivity() {
    private val channels = mutableListOf<Channel>()
    private val shown = mutableListOf<Channel>()
    private lateinit var list: ListView
    private lateinit var status: TextView
    private lateinit var search: EditText
    private var section = "TV"
    private val prefs by lazy { getSharedPreferences("elvin", MODE_PRIVATE) }
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readLocal(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8,11,18)
        window.navigationBarColor = android.graphics.Color.rgb(8,11,18)
        buildUi()
        val saved = prefs.getString("playlist_url", null)
        if (!saved.isNullOrBlank()) loadUrl(saved, false)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18); setBackgroundColor(0xFF080B12.toInt()) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
        val brand = TextView(this).apply { text = "ELVİN  TV RADYO"; textSize = 23f; setTextColor(0xFFFFD27A.toInt()); setPadding(8) }
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        val add = button("+ LİSTE EKLE") { showAddDialog() }
        header.addView(add)
        root.addView(header)
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("TV", "RADYO", "FAVORİLER").forEach { tab -> tabs.addView(button(tab) { section = tab; refresh() }, LinearLayout.LayoutParams(0, -2, 1f)) }
        root.addView(tabs)
        search = EditText(this).apply { hint = "Kanal veya kategori ara"; setSingleLine(); setTextColor(-1); setHintTextColor(0xFF8B93A5.toInt()); setPadding(14) }
        search.addTextChangedListener(SimpleTextWatcher { refresh() })
        root.addView(search)
        status = TextView(this).apply { text = "Yasal yayın listenizi ekleyin"; setTextColor(0xFFB6BDCC.toInt()); setPadding(10) }
        root.addView(status)
        list = ListView(this).apply { dividerHeight = 1; divider = android.graphics.drawable.ColorDrawable(0xFF252C3A.toInt()); isFocusable = true }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        list.setOnItemClickListener { _, _, position, _ ->
            shown.getOrNull(position)?.let { ch -> startActivity(Intent(this, PlayerActivity::class.java).putExtra("name", ch.name).putExtra("url", ch.url)) }
        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            shown.getOrNull(position)?.let { ch ->
                val key = ch.url
                val set = prefs.getStringSet("favorites", emptySet())!!.toMutableSet()
                if (!set.add(key)) set.remove(key)
                prefs.edit().putStringSet("favorites", set).apply()
                refresh()
            }; true
        }
        setContentView(root)
        refresh()
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 12f
        setTextColor(0xFFFFD27A.toInt()); setBackgroundColor(0xFF172033.toInt())
        setOnClickListener { action() }
    }

    private fun showAddDialog() {
        val input = EditText(this).apply { hint = "https://.../playlist.m3u"; setTextColor(-1); setHintTextColor(0xFF888888.toInt()) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24); addView(input) }
        AlertDialog.Builder(this).setTitle("Yayın listesi ekle")
            .setView(box)
            .setPositiveButton("URL YÜKLE") { _, _ -> loadUrl(input.text.toString().trim(), true) }
            .setNeutralButton("DOSYADAN SEÇ") { _, _ -> picker.launch(arrayOf("audio/*", "text/*", "application/octet-stream")) }
            .setNegativeButton("İPTAL", null).show()
    }

    private fun loadUrl(url: String, save: Boolean) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) { status.text = "Geçerli bir http/https adresi girin"; return }
        status.text = "Liste yükleniyor…"
        lifecycleScope.launch {
            try {
                val body = withContext(Dispatchers.IO) { URL(url).openConnection().apply { connectTimeout=12000; readTimeout=20000 }.getInputStream().bufferedReader().use { it.readText() } }
                val parsed = M3uParser.parse(body)
                channels.clear(); channels.addAll(parsed)
                if (save) prefs.edit().putString("playlist_url", url).apply()
                status.text = "${parsed.size} kanal yüklendi"
                refresh()
            } catch (e: Exception) { status.text = "Liste alınamadı: ${e.localizedMessage ?: "Bağlantı hatası"}" }
        }
    }

    private fun readLocal(uri: Uri) {
        lifecycleScope.launch {
            try {
                val body = withContext(Dispatchers.IO) { contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }
                channels.clear(); channels.addAll(M3uParser.parse(body))
                status.text = "${channels.size} kanal yüklendi"
                refresh()
            } catch (e: Exception) { status.text = "Dosya okunamadı" }
        }
    }

    private fun refresh() {
        if (!::list.isInitialized) return
        val favs = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
        val q = search.text?.toString()?.trim()?.lowercase().orEmpty()
        shown.clear()
        shown.addAll(channels.filter {
            val typeOk = when(section) { "RADYO" -> it.isRadio; "FAVORİLER" -> favs.contains(it.url); else -> !it.isRadio }
            typeOk && (q.isBlank() || it.name.lowercase().contains(q) || it.group.lowercase().contains(q))
        })
        list.adapter = object : android.widget.BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(position: Int) = shown[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val c = getItem(position)
                return (convertView as? TextView ?: TextView(this@MainActivity).apply { setPadding(18); textSize=16f; setTextColor(-1); isFocusable=true }).apply {
                    text = "${c.name}\n${c.group}   •   Uzun bas: favori"
                    setBackgroundColor(if (position % 2 == 0) 0xFF111827.toInt() else 0xFF0D1420.toInt())
                }
            }
        }
    }
}
