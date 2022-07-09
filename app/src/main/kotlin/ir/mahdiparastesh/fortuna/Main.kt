package ir.mahdiparastesh.fortuna

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.icu.util.Calendar
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.forEachIndexed
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModel
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import ir.mahdiparastesh.fortuna.ItemDay.Companion.changeVar
import ir.mahdiparastesh.fortuna.Vita.Companion.defPos
import ir.mahdiparastesh.fortuna.Vita.Companion.default
import ir.mahdiparastesh.fortuna.Vita.Companion.emptyLuna
import ir.mahdiparastesh.fortuna.Vita.Companion.lunaMaxima
import ir.mahdiparastesh.fortuna.Vita.Companion.mean
import ir.mahdiparastesh.fortuna.Vita.Companion.showScore
import ir.mahdiparastesh.fortuna.Vita.Companion.toCalendar
import ir.mahdiparastesh.fortuna.Vita.Companion.toKey
import ir.mahdiparastesh.fortuna.Vita.Companion.z
import ir.mahdiparastesh.fortuna.databinding.MainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@SuppressLint("InvalidFragmentVersionForActivityResult")
class Main : ComponentActivity(), NavigationView.OnNavigationItemSelectedListener {
    val c: Context get() = applicationContext
    lateinit var b: MainBinding
    val m: Model by viewModels()
    val sp: SharedPreferences by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = MainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Toolbar & Navigation
        ActionBarDrawerToggle(
            this, b.root, b.toolbar, R.string.navOpen, R.string.navClose
        ).apply {
            b.root.addDrawerListener(this)
            isDrawerIndicatorEnabled = true
            syncState()
        }
        b.nav.setNavigationItemSelectedListener(this)
        b.toolbar.navigationIcon?.colorFilter =
            pdcf(color(com.google.android.material.R.attr.colorOnPrimary))
        for (n in BaseNumeral.all.indices) {
            val nt = BaseNumeral.all[n]
            b.toolbar.menu.add(
                0, nt.id, n, "${getString(nt.name)} ${getString(R.string.numerals)}"
            ).apply {
                isCheckable = true
                isChecked = sp.getString(SP_NUMERAL_TYPE, arNumType) ==
                        (nt.jClass?.canonicalName ?: arNumType)
            }
        }
        b.toolbar.setOnMenuItemClickListener { mItem ->
            sp.edit().putString(
                SP_NUMERAL_TYPE,
                BaseNumeral.all.find { it.id == mItem.itemId }?.jClass?.canonicalName ?: arNumType
            ).apply()
            updateGrid(); updateOverflow(); true
        }

        // Panel
        b.luna.adapter = ArrayAdapter(
            this, R.layout.spinner, resources.getStringArray(R.array.luna)
        ).apply { setDropDownViewResource(R.layout.spinner_dd) }
        b.luna.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, i: Int, id: Long) {
                if (firstResume) return
                if (rollingLuna) {
                    rollingLuna = false
                    return; }
                m.luna = "${z(b.annus.text, 4)}.${z(i + 1)}"
                m.calendar = m.luna!!.toCalendar(calType)
                updateGrid()
            }
        }
        b.annus.addTextChangedListener {
            if (it.toString().length != 4 || firstResume) return@addTextChangedListener
            if (rollingAnnus) {
                rollingAnnus = false
                return@addTextChangedListener; }
            m.luna = "${z(it, 4)}.${z(b.luna.selectedItemPosition + 1)}"
            m.calendar = m.luna!!.toCalendar(calType)
            updateGrid()
        }
        b.next.setOnClickListener { rollCalendar(true) }
        b.prev.setOnClickListener { rollCalendar(false) }
        b.defaultVar.setOnClickListener {
            m.thisLuna().changeVar(this@Main, m.calendar.defPos())
        }

        // Miscellaneous
        (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel(
                Reminder.REMIND, c.getString(R.string.ntfReminderDesc),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.ntfReminderDesc) }
        )
        Reminder.alarm(c)
        if (sp.contains(SP_ARABIC_NUMERALS)) sp.edit().remove(SP_ARABIC_NUMERALS).apply()
    }

    private var firstResume = true
    override fun onResume() {
        super.onResume()
        m.vita = Vita.load(c)
        if (m.luna == null) {
            val extraLuna = intent.getStringExtra(EXTRA_LUNA)
            if (firstResume && extraLuna != null) {
                m.luna = extraLuna
                m.calendar = extraLuna.toCalendar(calType)
            } else {
                m.calendar = calType.newInstance()
                m.luna = m.calendar.toKey()
            }
            updatePanel()
            if (!Vita.Stored(c).exists()) {
                m.vita!![m.luna!!] = m.calendar.emptyLuna()
                m.vita!!.save(c)
            }
        }
        updateGrid()
        if (firstResume) m.vita?.reform(c)
        else b.annus.clearFocus()
        firstResume = false
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navAverage -> AlertDialog.Builder(this).apply {
                val scores = arrayListOf<Float>()
                m.vita?.forEach { key, luna ->
                    for (v in 0 until key.toCalendar(calType).lunaMaxima())
                        (luna[v] ?: luna.default)?.let { scores.add(it) }
                }
                val sum = scores.sum()
                val text = getString(
                    R.string.statText,
                    (if (scores.isEmpty()) 0f else sum / scores.size.toFloat()).toString(),
                    sum.toString()
                )
                setTitle(R.string.navStat)
                setMessage(text)
                setPositiveButton(R.string.ok, null)
                setNeutralButton(R.string.copy) { _, _ ->
                    (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.setPrimaryClip(
                        ClipData.newPlainText(getString(R.string.fortunaStat), text)
                    )
                    Toast.makeText(c, R.string.done, Toast.LENGTH_SHORT).show()
                }
            }.show()
            R.id.navExport -> exportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = Vita.MIME_TYPE
                putExtra(Intent.EXTRA_TITLE, Vita.EXPORT_NAME)
            })
            R.id.navImport -> importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = Vita.MIME_TYPE
            })
            R.id.navSend -> {
                val binary = m.vita?.export(c) ?: return false
                val exported = File(cacheDir, Vita.EXPORT_NAME)
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        FileOutputStream(exported).use { it.write(binary) }
                    }.onSuccess {
                        withContext(Dispatchers.Main) {
                            Intent(Intent.ACTION_SEND).apply {
                                type = Vita.MIME_TYPE
                                putExtra(
                                    Intent.EXTRA_STREAM,
                                    FileProvider.getUriForFile(c, packageName, exported)
                                )
                            }.also { startActivity(it) }
                        }
                    }
                }
            }
            R.id.navHelp -> AlertDialog.Builder(this).apply {
                setTitle(R.string.navHelp)
                setMessage(R.string.help)
                setPositiveButton(R.string.ok, null)
            }.show()
        }
        return true
    }

    private val exportLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val bExp = try {
                c.contentResolver.openFileDescriptor(it.data!!.data!!, "w")?.use { des ->
                    FileOutputStream(des.fileDescriptor).use { fos ->
                        fos.write(m.vita?.export(c))
                    }
                }
                true
            } catch (ignored: Exception) {
                false
            }
            Toast.makeText(
                c, if (bExp) R.string.done else R.string.exportUndone, Toast.LENGTH_LONG
            ).show()
        }

    private val importLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            var data: String? = null
            try {
                c.contentResolver.openFileDescriptor(it.data!!.data!!, "r")?.use { des ->
                    val sb = StringBuffer()
                    FileInputStream(des.fileDescriptor).use { fis ->
                        var i: Int
                        while (fis.read().also { r -> i = r } != -1) sb.append(i.toChar())
                    }
                    data = sb.toString()
                }
                data!!
            } catch (e: Exception) {
                Toast.makeText(c, R.string.importOpenError, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            val imported: Vita
            try {
                imported = Gson().fromJson(data, Vita::class.java)
            } catch (e: Exception) {
                Toast.makeText(c, R.string.importReadError, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            AlertDialog.Builder(this@Main).apply {
                setTitle(c.resources.getString(R.string.navImport))
                setMessage(c.resources.getString(R.string.askImport))
                setPositiveButton(R.string.yes) { _, _ ->
                    m.vita = imported.also { vita -> vita.save(c) }
                    updateGrid()
                    Toast.makeText(c, R.string.done, Toast.LENGTH_LONG).show()
                }
                setNegativeButton(R.string.no, null)
            }.show()
        }

    private fun updatePanel() {
        b.annus.setText(m.calendar[Calendar.YEAR].toString())
        b.luna.setSelection(m.calendar[Calendar.MONTH])
    }

    fun updateGrid() {
        b.grid.adapter = ItemDay(this).also {
            b.defaultVar.text = it.luna.last().showScore()
            b.lunaMean.text = it.luna.mean(m.calendar.lunaMaxima()).toString()
        }
    }

    private fun updateOverflow() {
        b.toolbar.menu.forEachIndexed { i, item ->
            item.isChecked = sp.getString(SP_NUMERAL_TYPE, arNumType) ==
                    (BaseNumeral.all[i].jClass?.canonicalName ?: arNumType)
        }
    }

    private var rollingAnnus = false
    private var rollingLuna = false
    private fun rollCalendar(up: Boolean) {
        m.calendar.roll(Calendar.MONTH, up)
        if ((up && m.calendar[Calendar.MONTH] == 0) ||
            (!up && m.calendar[Calendar.MONTH] == m.calendar.getActualMaximum(Calendar.MONTH))
        ) m.calendar.roll(Calendar.YEAR, up)
        m.luna = m.calendar.toKey()
        rollingAnnus = true
        rollingLuna = true
        updatePanel()
        updateGrid()
    }

    companion object {
        const val EXTRA_LUNA = "luna"
        const val SP_ARABIC_NUMERALS = "arabic_numerals"
        const val SP_NUMERAL_TYPE = "numeral_type"
        const val arNumType = "0"
        val calType = when (BuildConfig.FLAVOR) {
            "gregorian" -> android.icu.util.GregorianCalendar::class.java
            /*"persian"*/ else -> PersianCalendar::class.java
        }

        @ColorInt
        fun ContextThemeWrapper.color(@AttrRes attr: Int) = TypedValue().apply {
            theme.resolveAttribute(attr, this, true)
        }.data

        fun pdcf(@ColorInt color: Int, mode: PorterDuff.Mode = PorterDuff.Mode.SRC_IN) =
            PorterDuffColorFilter(color, mode)
    }

    class Model : ViewModel() {
        var vita: Vita? = null
        var luna: String? = null
        lateinit var calendar: Calendar

        fun thisLuna() = vita?.find(luna!!) ?: calendar.emptyLuna()
    }
}
