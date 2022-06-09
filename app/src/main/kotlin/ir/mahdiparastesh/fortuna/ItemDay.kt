package ir.mahdiparastesh.fortuna

import android.annotation.SuppressLint
import android.content.Intent
import android.database.DataSetObserver
import android.graphics.Color
import android.os.Build
import android.provider.CalendarContract
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.get
import ir.mahdiparastesh.fortuna.Main.Companion.color
import ir.mahdiparastesh.fortuna.Vita.Companion.defPos
import ir.mahdiparastesh.fortuna.Vita.Companion.default
import ir.mahdiparastesh.fortuna.Vita.Companion.lunaMaxima
import ir.mahdiparastesh.fortuna.Vita.Companion.saveScore
import ir.mahdiparastesh.fortuna.Vita.Companion.showScore
import ir.mahdiparastesh.fortuna.Vita.Companion.z
import ir.mahdiparastesh.fortuna.databinding.ItemDayBinding
import ir.mahdiparastesh.fortuna.databinding.VariabilisBinding
import java.util.*

class ItemDay(private val c: Main) : ListAdapter {
    private val roman: Array<String> by lazy { c.resources.getStringArray(R.array.romanNumbers) }
    private val cp: Int by lazy { c.color(com.google.android.material.R.attr.colorPrimary) }
    private val cs: Int by lazy { c.color(com.google.android.material.R.attr.colorSecondary) }
    private val tc: Int by lazy { c.color(android.R.attr.textColor) }
    private val cpo: Int by lazy { c.color(com.google.android.material.R.attr.colorOnPrimary) }
    private val cso: Int by lazy { c.color(com.google.android.material.R.attr.colorOnSecondary) }
    val luna = c.m.thisLuna()

    override fun registerDataSetObserver(observer: DataSetObserver) {}
    override fun unregisterDataSetObserver(observer: DataSetObserver) {}

    override fun getCount(): Int = c.m.calendar.lunaMaxima()

    override fun getItem(i: Int): Float = 0f

    override fun getItemId(i: Int): Long = i.toLong()

    @SuppressLint("SetTextI18n", "ViewHolder")
    override fun getView(i: Int, convertView: View?, parent: ViewGroup): View =
        ItemDayBinding.inflate(c.layoutInflater, parent, false).apply {
            val score: Float? = luna[i] ?: luna.default
            val isEstimated = luna[i] == null && luna.default != null
            dies.text = roman[i]
            variabilis.text = (if (isEstimated) "c. " else "") + score.showScore()
            root.setBackgroundColor(
                when {
                    score != null && score > 0f -> {
                        dies.setTextColor(cpo)
                        variabilis.setTextColor(cpo)
                        Color.valueOf(
                            cp.red.toValue(), cp.green.toValue(), cp.blue.toValue(),
                            score / Vita.MAX_RANGE
                        ).toArgb()
                    }
                    score != null && score < 0f -> {
                        dies.setTextColor(cso)
                        variabilis.setTextColor(cso)
                        Color.valueOf(
                            cs.red.toValue(), cs.green.toValue(), cs.blue.toValue(),
                            -score / Vita.MAX_RANGE
                        ).toArgb()
                    }
                    else -> {
                        dies.setTextColor(tc)
                        variabilis.setTextColor(tc)
                        Color.TRANSPARENT
                    }
                }
            )
            highlight.setOnClickListener { luna.changeVar(c, i) }
            highlight.setOnLongClickListener {
                val cal = Main.calType.newInstance()
                    .apply { timeInMillis = c.m.calendar.timeInMillis }
                cal[Calendar.DAY_OF_MONTH] = i + 1
                c.startActivity(
                    Intent(Intent.ACTION_VIEW).setData(
                        CalendarContract.CONTENT_URI.buildUpon()
                            .appendPath("time")
                            .appendEncodedPath(cal.timeInMillis.toString()).build()
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            }
        }.root

    override fun hasStableIds(): Boolean = true
    override fun getItemViewType(i: Int): Int = 0
    override fun getViewTypeCount(): Int = 1
    override fun isEmpty(): Boolean = false
    override fun areAllItemsEnabled(): Boolean = true
    override fun isEnabled(i: Int): Boolean = true

    companion object {
        fun Int.toValue() = toFloat() / 256f

        private fun Int.toScore() = -(toFloat() - 6f) / 2f

        private fun Float.toVariabilis() = (-(this * 2f) + 6f).toInt()

        fun Luna.changeVar(c: Main, i: Int) {
            val bv = VariabilisBinding.inflate(c.layoutInflater)
            bv.picker.apply {
                maxValue = 12
                minValue = 0
                value = this@changeVar[i]?.toVariabilis() ?: lastOrNull()?.toVariabilis() ?: 6
                wrapSelectorWheel = false
                setFormatter { it.toScore().showScore() }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    textColor = c.color(android.R.attr.textColor)
                    textSize = c.resources.displayMetrics.density * 25f
                }
                (this@apply[0] as EditText).also { it.filters = arrayOf() }
            }
            AlertDialog.Builder(c).apply {
                setTitle(
                    c.getString(
                        R.string.variabilis,
                        if (i != c.m.calendar.defPos()) "${c.m.luna}.${z(i + 1)}"
                        else c.getString(R.string.defValue)
                    )
                )
                setView(bv.root)
                setNegativeButton(R.string.cancel, null)
                setPositiveButton(R.string.save) { _, _ ->
                    if (c.m.vita != null) saveScore(c, i, bv.picker.value.toScore())
                }
                setNeutralButton(R.string.clear) { _, _ ->
                    if (c.m.vita != null) saveScore(c, i, null)
                }
            }.show()
        }
    }
}
