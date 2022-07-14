package ir.mahdiparastesh.fortuna

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import kotlin.math.pow

abstract class BaseNumeral(
    protected val num: Int,
    private val supportsZero: Boolean = false, private val supportsNegative: Boolean = false
) { // wanted to use UInt instead but it gets problematic in Class.forName -> getConstructor!
    abstract val chars: Array<String>
    open val defaultStr: String = "NaN"
    abstract fun parse(): String

    override fun toString(): String =
        if ((num > 0 || supportsZero) && (num >= 0 || supportsNegative)) try {
            parse()
        } catch (e: Exception) {
            defaultStr
        } else defaultStr

    companion object {
        val all = arrayOf(
            NumeralType(null, R.string.numArabic, R.id.numArabic),
            NumeralType(
                RomanNumeral::class.java, R.string.numRoman, R.id.numRoman
            ), // -~0..+1400 preceded Arabic (sources claiming -900 mistake/mix it with Etruscan)
            NumeralType(
                BrahmiNumeral::class.java, R.string.numBrahmi, R.id.numBrahmi
            ), // -300..+500 preceded Gupta script.
            NumeralType(
                OldPersianNumeral::class.java, R.string.numOldPersian, R.id.numOldPersian
            ), // -525..-330 annihilated! (presumably Avestan and Middle Persian had no numerals.)
            NumeralType(
                EtruscanNumeral::class.java, R.string.numEtruscan, R.id.numEtruscan
            ), // -700..+50 inspired and was replaced by Roman numerals.
            NumeralType(
                AtticNumeral::class.java, R.string.numAttic, R.id.numAttic
            ), // -700..-300 preceded Greek numerals.
            NumeralType(
                HieroglyphNumeral::class.java, R.string.numHieroglyph, R.id.numHieroglyph, true
            ), // -3200..+400 preceded Coptic script.
        )

        fun find(jc: String): Class<*>? = try {
            Class.forName(jc)
        } catch (e: ClassNotFoundException) {
            null
        }
        // Putting this in ItemDay::getView caused a nasty build time error.
    }
    // https://en.wikipedia.org/wiki/List_of_numeral_systems
}

data class NumeralType(
    val jClass: Class<*>?, @StringRes val name: Int, @IdRes val id: Int,
    val enlarge: Boolean = false
)

abstract class AtticBasedNumeral(num: Int) : BaseNumeral(num) {
    private val n: String = num.toString()
    abstract val subtract4th: Boolean
    open val rtl: Boolean = false

    override fun parse(): String {
        val ln = n.length
        val s = StringBuilder()
        for (ii in n.indices) {
            val i = n[ii].digitToInt()
            val base = chars[((ln - ii) - 1) * 2]
            val half = chars[(((ln - ii) - 1) * 2) + 1]
            when {
                i in 0..3 || (subtract4th && i == 4) ->
                    s.append(base.repeat(i))
                !subtract4th && i == 4 ->
                    s.append(base + half)
                i in 5..8 || (subtract4th && i == 9) ->
                    s.append(half + (base.repeat(i - 5)))
                !subtract4th && i == 9 ->
                    s.append(base + chars[(((ln - ii) - 1) * 2) + 2])
            }
        }
        var ret = s.toString()
        if (rtl) ret = ret.reversed()
        return ret
    }
}

class AtticNumeral(num: Int) : AtticBasedNumeral(num) {
    override val subtract4th = true
    override val chars = arrayOf(
        "I", "\ud800\udd43", // 1, 5
        "Δ", "\ud800\udd44", // 10, 50
        "Η", "\ud800\udd45", // 100, 500
        "Χ", "\ud800\udd46", // 1,000, 5,000
        "M", "\ud800\udd47"  // 10,000, 50,000
    )
    // https://charbase.com/10144-unicode-greek-acrophonic-attic-fifty
    // https://unicode-table.com/en/blocks/ancient-greek-numbers/
}

class EtruscanNumeral(num: Int) : AtticBasedNumeral(num) {
    override val subtract4th = true
    override val rtl = true
    override val chars = arrayOf(
        "\uD800\udf20", "\uD800\uDF21", "\uD800\uDF22", "\uD800\uDF23", "\uD800\uDF1F"
    )
}

class RomanNumeral(num: Int) : AtticBasedNumeral(num) {
    override val subtract4th = false
    override val chars = arrayOf(
        "I", "V", "X", "L", "C", "D", "M",
        "I\u0305", "V\u0305", "X\u0305", "L\u0305", "C\u0305", "D\u0305", "M\u0305"
        // An overline on a Roman numeral means you are multiplying that Roman numeral by 1,000.
    )
}


abstract class GematriaLikeNumeral(num: Int) : BaseNumeral(num) {
    private val n: String = num.toString()
    override fun parse(): String {
        val ln = n.length
        val s = StringBuilder()
        for (ii in n.indices) {
            val i = n[ii].digitToInt()
            if (i == 0) continue
            s.append(chars[((((ln - ii) - 1) * 9) - 1) + i])
        }
        return s.toString()
    }
}

class HieroglyphNumeral(num: Int) : GematriaLikeNumeral(num) {
    override val chars = arrayOf(
        // 1..9
        "\uD80C\uDFFA", "\uD80C\uDFFB", "\uD80C\uDFFC", "\uD80C\uDFFD", "\uD80C\uDFFE",
        "\uD80C\uDFFF", "\uD80D\uDC00", "\uD80D\uDC01", "\uD80D\uDC02",
        // 10..90
        "\uD80C\uDF86", "\uD80C\uDF8F", "\uD80C\uDF88", "\uD80C\uDF89", "\uD80C\uDF8A",
        "\uD80C\uDF8B", "\uD80C\uDF8C", "\uD80C\uDF8D", "\uD80C\uDF8E",
        // 100..900
        "\uD80C\uDF62", "\uD80C\uDF63", "\uD80C\uDF64", "\uD80C\uDF65", "\uD80C\uDF66",
        "\uD80C\uDF67", "\uD80C\uDF68", "\uD80C\uDF69", "\uD80C\uDF6A",
        // 1,000..9,000
        "\uD80C\uDDBC", "\uD80C\uDDBD", "\uD80C\uDDBE", "\uD80C\uDDBF", "\uD80C\uDDC0",
        "\uD80C\uDDC1", "\uD80C\uDDC2", "\uD80C\uDDC3", "\uD80C\uDDC4",
        // 10,000..100,000
        "\uD80C\uDCAD", "\uD80C\uDCAE", "\uD80C\uDCAF", "\uD80C\uDCB0", "\uD80C\uDCB1",
        "\uD80C\uDCB2", "\uD80C\uDCB3", "\uD80C\uDCB4", "\uD80C\uDCB5", "\uD80C\uDD90"
    )
    override val defaultStr = "\uD80C\uDC4F"
    // https://unicode-table.com/en/blocks/egyptian-hieroglyphs/
}

class BrahmiNumeral(num: Int) : GematriaLikeNumeral(num) {
    override val chars = arrayOf(
        // 1..9
        "\uD804\uDC52", "\uD804\uDC53", "\uD804\uDC54", "\uD804\uDC55", "\uD804\uDC56",
        "\uD804\uDC57", "\uD804\uDC58", "\uD804\uDC59", "\uD804\uDC5A",
        // 10..90
        "\uD804\uDC5B", "\uD804\uDC5C", "\uD804\uDC5D", "\uD804\uDC5E", "\uD804\uDC5F",
        "\uD804\uDC60", "\uD804\uDC61", "\uD804\uDC62", "\uD804\uDC63",
        // 100 (the rest are not available in unicode!)
        "\uD804\uDC64"
        // except 1,000: "\uD804\uDC65" which is useless without the previous chars!
    )
    // https://unicode-table.com/en/blocks/brahmi/
    // Magadhi Prakrit was Mahavira and Buddha's language.
    // Presumably they used Brahmi numerals or maybe Kharosthi.
}


class OldPersianNumeral(num: Int) : BaseNumeral(num) {
    override val chars = arrayOf(
        "\uD800\uDFD1", "\uD800\uDFD2", // 1, 2
        "\uD800\uDFD3", "\uD800\uDFD4", // 10, 20
        "\uD800\uDFD5" // 100
    )
    // https://unicode-table.com/en/blocks/old-persian/

    private fun charToInt(i: Int): Int {
        var ii = i.toDouble()
        if (i % 2 == 1) ii -= 1.0
        ii /= 2
        return 10.0.pow(ii).toInt() * (if (i % 2 == 0) 1 else 2)
    }

    override fun parse(): String {
        val s = StringBuilder()
        var n = num
        while (n > 0) {
            var subVal = 0
            var subChar = 0
            for (ch in chars.indices) {
                val charVal = charToInt(ch)
                if (charVal > n) break
                subChar = ch
                subVal = charVal
            }
            n -= subVal
            s.append(chars[subChar])
        }
        return s.toString()
    }
    // https://en.wikipedia.org/wiki/Old_Persian_cuneiform#Signs
    // Babylonian cuneiforms weren't available in unicode.
}
