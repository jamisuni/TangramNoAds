package io.github.jamisuni.tangram.play

// WO-003 design section 2 "Path data" (DA-21, WO-002 N9): the picture path `d` grammar. Pure Kotlin.

internal sealed interface PathSeg {
    data class MoveTo(val x: Double, val y: Double) : PathSeg
    data class LineTo(val x: Double, val y: Double) : PathSeg
    data class QuadTo(val x1: Double, val y1: Double, val x: Double, val y: Double) : PathSeg
    data class CubicTo(
        val x1: Double, val y1: Double, val x2: Double, val y2: Double, val x: Double, val y: Double,
    ) : PathSeg
    data object Close : PathSeg
}

internal object PathData {
    /**
     * Absolute `M L Q C Z` only, first command `M`. Letters may touch numbers; each command takes exactly its
     * arity of numbers (implicit repetition is rejected); a number is `-?(digits(.digits)?|.digits)` (no exponent,
     * no `+`); separators are runs of spaces and/or one comma; every value finite. Anything else returns null.
     */
    fun parse(d: String): List<PathSeg>? {
        val out = ArrayList<PathSeg>()
        var i = 0
        val n = d.length
        fun skipSpaces() {
            while (i < n && d[i] == ' ') i++
        }
        skipSpaces()
        if (i >= n || d[i] != 'M') return null
        while (true) {
            skipSpaces()
            if (i >= n) break
            val c = d[i]
            val arity = when (c) {
                'M', 'L' -> 2
                'Q' -> 4
                'C' -> 6
                'Z' -> 0
                else -> return null
            }
            i++
            val v = DoubleArray(arity)
            for (k in 0 until arity) {
                val before = i
                skipSpaces()
                var sep = i > before
                if (k > 0 && i < n && d[i] == ',') {
                    i++
                    skipSpaces()
                    sep = true
                }
                if (k > 0 && !sep && !(i < n && d[i] == '-')) return null // a minus always starts a number
                val start = i
                if (i < n && d[i] == '-') i++
                val intStart = i
                while (i < n && d[i] in '0'..'9') i++
                val intDigits = i - intStart
                var fracDigits = 0
                if (i < n && d[i] == '.') {
                    i++
                    val fs = i
                    while (i < n && d[i] in '0'..'9') i++
                    fracDigits = i - fs
                    if (fracDigits == 0) return null
                }
                if (intDigits == 0 && fracDigits == 0) return null
                val x = d.substring(start, i).toDoubleOrNull() ?: return null
                if (!x.isFinite()) return null
                v[k] = x
            }
            // After the last number only spaces or a command letter may follow (no further number, no comma).
            if (i < n && d[i] != ' ' && d[i] !in "MLQCZ") return null
            out.add(
                when (c) {
                    'M' -> PathSeg.MoveTo(v[0], v[1])
                    'L' -> PathSeg.LineTo(v[0], v[1])
                    'Q' -> PathSeg.QuadTo(v[0], v[1], v[2], v[3])
                    'C' -> PathSeg.CubicTo(v[0], v[1], v[2], v[3], v[4], v[5])
                    else -> PathSeg.Close
                },
            )
        }
        return out
    }
}
