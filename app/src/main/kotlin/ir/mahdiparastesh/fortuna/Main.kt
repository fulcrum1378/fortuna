package ir.mahdiparastesh.fortuna

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModel
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import ir.mahdiparastesh.fortuna.ItemDay.Companion.changeVar
import ir.mahdiparastesh.fortuna.Vita.Companion.lunaMaxima
import ir.mahdiparastesh.fortuna.Vita.Companion.mean
import ir.mahdiparastesh.fortuna.Vita.Companion.showScore
import ir.mahdiparastesh.fortuna.Vita.Companion.toKey
import ir.mahdiparastesh.fortuna.Vita.Companion.toPersianCalendar
import ir.mahdiparastesh.fortuna.Vita.Companion.z
import ir.mahdiparastesh.fortuna.databinding.MainBinding
import java.io.FileInputStream
import java.io.FileOutputStream

@SuppressLint("InvalidFragmentVersionForActivityResult")
class Main : ComponentActivity(), NavigationView.OnNavigationItemSelectedListener {
    lateinit var c: Context
    lateinit var b: MainBinding
    val m: Model by viewModels()
    private lateinit var toggleNav: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        c = applicationContext
        b = MainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Toolbar & Navigation
        toggleNav = ActionBarDrawerToggle(
            this, b.root, b.toolbar, R.string.navOpen, R.string.navClose
        ).apply {
            b.root.addDrawerListener(this)
            isDrawerIndicatorEnabled = true
            syncState()
        }
        b.nav.setNavigationItemSelectedListener(this)
        b.toolbar.navigationIcon?.colorFilter =
            pdcf(color(com.google.android.material.R.attr.colorOnPrimary))

        // Header
        b.luna.adapter = ArrayAdapter(
            this, R.layout.spinner, resources.getStringArray(R.array.luna)
        ).apply { setDropDownViewResource(R.layout.spinner_dd) }
        b.luna.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, i: Int, id: Long) {
                m.lunaChanged = true
                m.luna = "${z(b.annus.text, 4)}.${z(i + 1)}"
                m.calendar = m.luna.toPersianCalendar()
                updateGrid()
            }
        }
        b.annus.addTextChangedListener {
            if (it.toString().length != 4) return@addTextChangedListener
            m.luna = "${z(it, 4)}.${z(b.luna.selectedItemPosition + 1)}"
            m.calendar = m.luna.toPersianCalendar()
            updateGrid()
        }
        b.defaultVar.setOnClickListener {
            m.thisLuna().changeVar(this@Main, 31)
        }
    }

    private var firstResume = false
    override fun onResume() {
        super.onResume()
        m.vita = Vita.load(c)
        if (!m.lunaChanged) {
            m.calendar = PersianCalendar()
            m.luna = m.calendar.toKey()
            updateHeader(m.calendar)
            if (!Vita.Stored(c).exists()) {
                m.vita!![m.luna] = Vita.emptyLuna()
                m.vita!!.save(c)
            }
        }
        updateGrid()
        if (!firstResume) m.vita?.reform(c)
        else b.annus.clearFocus()
        firstResume = true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navAverage -> AlertDialog.Builder(this).apply {
                setTitle(R.string.navAverage)
                setMessage(m.vita?.mean().toString())
                setNeutralButton(R.string.ok, null)
            }.show().stylise(this)
            R.id.navExport -> exportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = Vita.MIME_TYPE
                putExtra(Intent.EXTRA_TITLE, "fortuna.json")
            })
            R.id.navImport -> importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = Vita.MIME_TYPE
            })
        }
        return true
    }

    private val exportLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            m.vita?.reform(c)
            val bExp = try {
                c.contentResolver.openFileDescriptor(it.data!!.data!!, "w")?.use { des ->
                    FileOutputStream(des.fileDescriptor).use { fos ->
                        fos.write(Gson().toJson(m.vita?.toSortedMap()).encodeToByteArray())
                    }
                }
                true
            } catch (ignored: Exception) {
                false
            }
            Toast.makeText(
                c, if (bExp) R.string.exportDone else R.string.exportUndone, Toast.LENGTH_LONG
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
                }
                setNegativeButton(R.string.no, null)
            }.show().stylise(this@Main)
        }

    private fun updateHeader(cal: PersianCalendar) {
        b.annus.setText(cal[Calendar.YEAR].toString())
        b.luna.setSelection(cal[Calendar.MONTH])
    }

    fun updateGrid() {
        b.grid.adapter = ItemDay(this).also {
            b.defaultVar.text = it.luna.last().showScore()
            b.lunaMean.text = it.luna.mean(m.calendar.lunaMaxima()).toString()
        }
    }

    private fun pdcf(@ColorInt color: Int) = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)

    companion object {
        @ColorInt
        fun ContextThemeWrapper.color(@AttrRes attr: Int) = TypedValue().apply {
            theme.resolveAttribute(attr, this, true)
        }.data

        fun AlertDialog.stylise(c: Main): AlertDialog {
            val tc = c.color(android.R.attr.textColor)
            window?.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.apply {
                typeface = c.resources.getFont(R.font.quattrocento_bold)
            }
            window?.findViewById<TextView>(android.R.id.message)?.apply {
                typeface = c.resources.getFont(R.font.quattrocento_regular)
                setTextColor(tc)
            }
            getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                typeface = c.resources.getFont(R.font.quattrocento_bold)
                setTextColor(tc)
            }
            getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                typeface = c.resources.getFont(R.font.quattrocento_bold)
                setTextColor(tc)
            }
            getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
                typeface = c.resources.getFont(R.font.quattrocento_bold)
                setTextColor(tc)
            }
            return this
        }
    }

    class Model : ViewModel() {
        var vita: Vita? = null
        lateinit var luna: String
        lateinit var calendar: PersianCalendar
        var lunaChanged = false

        fun thisLuna() = vita?.find(luna) ?: Vita.emptyLuna()
    }
}
