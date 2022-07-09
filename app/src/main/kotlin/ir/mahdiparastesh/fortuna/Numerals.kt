package ir.mahdiparastesh.fortuna

import androidx.annotation.IdRes
import androidx.annotation.StringRes

abstract class BaseNumeral(num: Int) {
    abstract val chars: Array<String>
    open val defaultStr: String = "NaN"
    protected val n: String = num.toString()

    companion object {
        val all = arrayOf(
            NumeralType(null, R.string.numArabic, R.id.numArabic),
            NumeralType(RomanNumeral::class.java, R.string.numRoman, R.id.numRoman),
            NumeralType(EtruscanNumeral::class.java, R.string.numEtruscan, R.id.numEtruscan),
            NumeralType(AtticNumeral::class.java, R.string.numAttic, R.id.numAttic),
            NumeralType(
                HieroglyphNumeral::class.java, R.string.numHieroglyph, R.id.numHieroglyph, true
            ),
        )

        fun find(jc: String): Class<*> = Class.forName(jc)
    }
    // https://en.wikipedia.org/wiki/List_of_numeral_systems
}

data class NumeralType(
    val jClass: Class<*>?, @StringRes val name: Int, @IdRes val id: Int,
    val enlarge: Boolean = false
)

abstract class AtticBasedNumeral(num: Int) : BaseNumeral(num) {
    abstract val fourthChar: Boolean

    override fun toString(): String = try {
        val ln = n.length
        val s = StringBuilder()
        for (ii in n.indices) {
            val i = n[ii].digitToInt()
            val base = chars[((ln - ii) - 1) * 2]
            val half = chars[(((ln - ii) - 1) * 2) + 1]
            when {
                i in 0..3 || (fourthChar && i == 4) ->
                    s.append(base.repeat(i))
                !fourthChar && i == 4 ->
                    s.append(base + half)
                i in 5..8 || (fourthChar && i == 9) ->
                    s.append(half + (base.repeat(i - 5)))
                !fourthChar && i == 9 ->
                    s.append(base + chars[(((ln - ii) - 1) * 2) + 2])
            }
        }
        s.toString()
    } catch (ignored: Exception) {
        defaultStr
    }
}

class AtticNumeral(num: Int) : AtticBasedNumeral(num) {
    override val fourthChar = true
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
    override val fourthChar = true
    override val chars = arrayOf(
        "\uD800\udf20", "\uD800\uDF21", "\uD800\uDF22", "\uD800\uDF23", "\uD800\uDF1F"
    )
}

class RomanNumeral(num: Int) : AtticBasedNumeral(num) {
    override val fourthChar = false
    override val chars = arrayOf(
        "I", "V", "X", "L", "C", "D", "M",
        "I\u0305", "V\u0305", "X\u0305", "L\u0305", "C\u0305", "D\u0305", "M\u0305"
        // An overline on a Roman numeral means you are multiplying that Roman numeral by 1,000.
    )
}


abstract class GematriaLikeNumeral(num: Int) : BaseNumeral(num) {
    override fun toString(): String = try {
        val ln = n.length
        val s = StringBuilder()
        for (ii in n.indices) {
            val i = n[ii].digitToInt()
            if (i == 0) continue
            s.append(chars[((((ln - ii) - 1) * 9) - 1) + i])
        }
        s.toString()
    } catch (ignored: Exception) {
        defaultStr
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

// abstract class GematriaBasedNumeral(num: Int) : GematriaLikeNumeral(num)
// Abjad + Greek + Hebrew + Cyrillic + Glagolitic + ~Phoenician + Armenian
