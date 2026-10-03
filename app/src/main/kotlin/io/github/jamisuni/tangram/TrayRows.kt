package io.github.jamisuni.tangram

import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId

/** REQ-035 / REQ-036 rows: the full-set rows per layout class; `play` drops absent pieces and empty rows (REQ-045). */
object TrayRows {
    fun forClass(layoutClass: LayoutClass): List<List<PieceId>> = when (layoutClass) {
        LayoutClass.PHONE -> listOf(
            listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
            listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
        )
        LayoutClass.TABLET -> listOf(
            listOf(PieceId.LT1, PieceId.LT2, PieceId.MT, PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
        )
    }
}
