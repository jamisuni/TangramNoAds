package io.github.jamisuni.tangram.acceptance.promise

// guardrail G-01 / decision DA-106: the forbidden-word lists of the promise checks (no ad, price, purchase, tip, donation or
// rating prompt text, REQ-001 A1 and REQ-008 as read by DA-106). SHARED CONSTANT: this JVM copy and the device copy under
// app/src/androidTest (acceptance/promise/PromiseWords.kt) must be equal; PromiseWordListEqualityTest compares the two files after
// dropping the package line and the comments, so keep this file free of imports and keep the two copies identical.
//
// Match semantics (design WO-006 section 6 item 4, rev 2 E3): every entry is marked WHOLE (a whole word) or PREFIX (a word-start
// prefix), case-insensitive. A "word" is a run of Unicode letters; digits, spaces, hyphens, punctuation and a format specifier such
// as %1$d all end it. A WHOLE entry does not hit add, adjust, address, feel, feed, payload (English) or maksimi, kaupunki
// (Finnish, which is why the Finnish pay and shop roots below are long enough to miss them). A PREFIX entry hits every word that
// starts with it, which is how an inflected Finnish form is caught (mainoksia, ostoksia, hinnat, tilaus, arvostelu).
// A stem that hits a legitimate word is handled by adding that exact word to ALLOWED_WORDS with its reason, never by loosening it.
// The Finnish stems are AI-written and are on the owner's review list (DA-106, task X-1).

enum class Match { WHOLE, PREFIX }

data class PromiseWord(val text: String, val match: Match)

object PromiseWords {
    private fun whole(vararg w: String) = w.map { PromiseWord(it, Match.WHOLE) }
    private fun prefix(vararg w: String) = w.map { PromiseWord(it, Match.PREFIX) }

    val EN: List<PromiseWord> =
        whole(
            "ad", "ads", "tip", "tips", "fee", "rate", "stars", "pay",
            // inflected forms (CR-5 S3): a whole-word entry does not catch them by itself
            "payment", "payments", "paying", "pays", "fees", "rating", "ratings", "rated", "tipping", "tipped",
        ) +
            prefix(
                "advert", "buy", "purchas", "pric", "donat", "subscri", "premium", "upgrad", "unlock", "review",
                "free trial", "in-app", "paid", "money", "cash", "shop", "cost", "coin",
            )

    val FI: List<PromiseWord> =
        prefix(
            "mainok", "mainos", "ostok", "ostos", "osta", "hinn", "hinta", "tilau", "arvostel", "arvio", "lahjoi", "lahjoit",
            "raha", "euro",
            // pay roots: maksu (maksun, maksut), maksa (maksaa), maksul (maksullinen), makse (maksetaan), makso (maksoi);
            // NOT "maks" alone, it would hit maksimi
            "maksu", "maksa", "maksul", "makse", "makso",
            // shop roots: kauppa (kauppaa), kaupa (kaupan, kaupat, kaupassa), kaupp; NOT "kaup" alone, it would hit kaupunki
            "kaupp", "kauppa", "kaupa", "kaupan",
        )

    /**
     * REQ-prescribed texts, exempt by KEY (design WO-006 section 6 item 4, F6): the REQ-009 free note and the REQ-049 privacy text.
     * Empty until WO-007 creates those keys; each key added there carries a comment citing REQ-009 or REQ-049. Nothing else is exempt.
     */
    val PROMISE_TEXT_KEYS: Set<String> = emptySet()

    /** Exact words a stem hits legitimately, each with its reason (empty today). */
    val ALLOWED_WORDS: Map<String, String> = emptyMap()

    /** No currency sign or amount (design WO-006 section 6 item 4). */
    val CURRENCY: List<Regex> = listOf(
        Regex("[€$£]"),
        Regex("\\d+[.,]\\d{2}\\s?(eur|€|usd|\\$)", RegexOption.IGNORE_CASE),
        // a currency code as a whole word: "5 USD", "EUR 3" (CR-5 S3)
        Regex("(?<!\\p{L})(usd|eur)(?!\\p{L})", RegexOption.IGNORE_CASE),
    )

    private val FORMAT_SPECIFIER = Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z%]")

    private val SPACES = Regex("[\\s\\u00A0\\u202F]+")

    /**
     * A format specifier is not text: it is blanked first (its dollar sign is not a currency sign). Every run of white space, a no-break
     * space included, becomes ONE space, so a phrase entry such as "free trial" matches whatever spacing the text uses (CR-5 N16).
     * The stems stay ASCII: IGNORE_CASE is ASCII-only.
     */
    fun visibleText(s: String): String = SPACES.replace(FORMAT_SPECIFIER.replace(s, " "), " ")

    private fun regexFor(w: PromiseWord): Regex {
        val body = Regex.escape(w.text)
        return when (w.match) {
            Match.WHOLE -> Regex("(?<!\\p{L})$body(?!\\p{L})", RegexOption.IGNORE_CASE)
            Match.PREFIX -> Regex("(?<!\\p{L})$body", RegexOption.IGNORE_CASE)
        }
    }

    private val compiled: List<Pair<PromiseWord, Regex>> = (EN + FI).map { it to regexFor(it) }

    /** The findings in [text], each "word (kind)" or "currency"; empty when the text is clean. */
    fun hits(text: String): List<String> {
        val t = visibleText(text)
        val out = ArrayList<String>()
        for ((w, re) in compiled) {
            for (m in re.findAll(t)) {
                val whole = Regex("\\p{L}+").find(t, m.range.first)?.value ?: m.value
                if (whole.lowercase() in ALLOWED_WORDS) continue
                out += "${w.text} (${w.match.name.lowercase()}) in \"$whole\""
            }
        }
        for (c in CURRENCY) if (c.containsMatchIn(t)) out += "currency (${c.pattern})"
        return out
    }
}
