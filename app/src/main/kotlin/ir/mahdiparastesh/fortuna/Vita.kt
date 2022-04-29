package ir.mahdiparastesh.fortuna

import android.content.Context
import android.icu.util.Calendar
import android.os.Build
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class Vita : HashMap<String, Luna>() {

    fun find(key: String): Luna? = getOrElse(key) { null }

    fun save(c: Context) {
        save(c, this)
    }

    fun reform(c: Context) {
        val removal = arrayListOf<String>()
        forEach { key, luna -> if (luna.all { it == null }) removal.add(key) }
        removal.forEach { remove(it) }
        save(c)
    }

    fun mean(): Float {
        val scores = arrayListOf<Float>()
        forEach { key, luna ->
            for (v in 0 until key.toCalendar(Main.calType).lunaMaxima())
                (luna[v] ?: luna.default)?.let { scores.add(it) }
        }
        return if (scores.isEmpty()) 0f else scores.sum() / scores.size
    }

    companion object {
        const val MAX_RANGE = 3f
        val MIME_TYPE =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) "application/octet-stream"
            else "application/json"

        fun load(c: Context): Vita = if (Stored(c).exists()) {
            val data: ByteArray
            FileInputStream(Stored(c)).use { data = it.readBytes() }
            Gson().fromJson(String(data), Vita::class.java)
        } else Vita()

        fun save(c: Context, vita: Vita) {
            FileOutputStream(Stored(c)).use { fos ->
                fos.write(
                    GsonBuilder().setPrettyPrinting().create().toJson(vita.toSortedMap())
                        .encodeToByteArray()
                )
            }
        }

        fun Calendar.emptyLuna() = Array<Float?>(defPos() + 1) { null }

        fun Calendar.toKey(): String =
            "${z(this[Calendar.YEAR], 4)}.${z(this[Calendar.MONTH] + 1)}"

        fun <CAL> String.toCalendar(klass: Class<CAL>): CAL where CAL : Calendar {
            val spl = split(".")
            return klass.newInstance().apply {
                this[Calendar.YEAR] = spl[0].toInt()
                this[Calendar.MONTH] = spl[1].toInt() - 1
                this[Calendar.DAY_OF_MONTH] = 1
            }
        }

        fun Float?.showScore(): String = if (this != 0f) this?.toString() ?: "_" else "0"

        fun z(n: Any?, ideal: Int = 2): String {
            var s = n.toString()
            while (s.length < ideal) s = "0$s"
            return s
        }

        fun Calendar.lunaMaxima() = getActualMaximum(Calendar.DAY_OF_MONTH)

        fun Calendar.defPos() = getMaximum(Calendar.DAY_OF_MONTH)

        fun Luna.saveScore(c: Main, i: Int, score: Float?) {
            this[i] = score
            c.m.vita!![c.m.luna] = this
            c.m.vita!!.save(c.c)
            c.updateGrid()
        }

        fun Luna.mean(maxDays: Int): Float {
            val scores = arrayListOf<Float>()
            for (v in 0 until maxDays) (this[v] ?: this.default)?.let { scores.add(it) }
            return if (scores.isEmpty()) 0f else scores.sum() / scores.size
        }

        var Luna.default: Float?
            get() = last()
            set(f) {
                this[size - 1] = f
            }
    }

    class Stored(c: Context) : File(c.filesDir, "vita.json")
}

typealias Luna = Array<Float?>
