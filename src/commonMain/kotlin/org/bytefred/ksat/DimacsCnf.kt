package org.bytefred.ksat

/**
 * A CNF formula parsed from DIMACS text.
 *
 * @property numVars the larger of the `p cnf <vars> <clauses>` variable count (the largest one if
 *   there are several headers; 0 without a header) and the largest variable used in a clause, so
 *   every literal of [clauses] is within `1..numVars`.
 * @property clauses each clause is an [IntArray] of non-zero signed DIMACS literals, in file order.
 */
class DimacsCnf(val numVars: Int, val clauses: List<IntArray>) {

    /** True iff every clause has at least one literal made true by [assignment]. */
    fun isSatisfiedBy(assignment: (Int) -> Boolean): Boolean =
        clauses.all { clause ->
            clause.any { lit ->
                val v = if (lit < 0) -lit else lit
                assignment(v) == (lit > 0)
            }
        }

    companion object {
        /**
         * Parse DIMACS CNF from [text]. Nothing is dropped and nothing malformed is guessed at: every
         * clause in the text is returned, and input neither upstream parser reads correctly is
         * rejected with a [DimacsParseException] that names the line.
         *
         * The grammar accepts what MiniSat's `parse_DIMACS` (non-strict, its default) or CaDiCaL's
         * `read_dimacs` (strict = 1, its default) accepts, as long as that reading loses nothing
         * (ksat-extra/doc/minisat-full-port.md, "DimacsCnf"):
         * - whitespace: the bytes 9..13 and 32 (MiniSat), so CR LF line ends work;
         * - `c` where a number could start, or right after a number's digits, starts a comment up to
         *   the end of the line (CaDiCaL; MiniSat only between clauses);
         * - header `p`, one or more blanks or tabs (CaDiCaL), `cnf`, then two integers separated by
         *   any whitespace (both; also across lines, so `p cnf 3` followed by a line `1 0` reads the
         *   `1` as the clause count). Optional, may appear more than once (MiniSat); the counts are
         *   not checked against the clauses (MiniSat non-strict), a negative count counts as 0;
         * - a number is an optional `+` or `-` and decimal digits (MiniSat); a clause ends at `0`
         *   (also `-0`); a number may directly follow another one (`1-2` is 1 and -2, MiniSat).
         *
         * Rejected: a `p` line that is not `p cnf <int> <int>` (e.g. `p wcnf`), a character where a
         * number must start (e.g. a SATLIB `%` tail, a letter, a lone `-`), a last clause without its
         * terminating `0` (both), and a number outside the Int range (CaDiCaL; MiniSat's int wraps
         * around silently, which would turn it into a different literal).
         */
        fun parse(text: String): DimacsCnf {
            var pos = 0
            var line = 1
            var numVars = 0
            val clauses = ArrayList<IntArray>()
            val current = ArrayList<Int>()

            fun cur(): Int = if (pos < text.length) text[pos].code else -1
            fun next() { if (text[pos] == '\n') line++; pos++ }
            fun isWs(c: Int) = (c in 9..13) || c == 32
            fun fail(what: String): Nothing = throw DimacsParseException(line, what)
            fun skipWs() { while (isWs(cur())) next() }
            fun skipLine() { while (cur() != -1 && cur() != '\n'.code) next() }
            fun describe(c: Int) = if (c == -1) "end of input" else "'${c.toChar()}'"

            /** Integer at [pos] (optional sign, digits); stops after the last digit. */
            fun parseInt(name: String): Int {
                val startLine = line
                var neg = false
                if (cur() == '-'.code) { neg = true; next() } else if (cur() == '+'.code) next()
                if (cur() !in '0'.code..'9'.code) fail("expected a digit ($name), found ${describe(cur())}")
                var v = 0L
                while (cur() in '0'.code..'9'.code) {
                    v = v * 10 + (cur() - '0'.code)
                    if (v > Int.MAX_VALUE) throw DimacsParseException(startLine, "$name too large (more than ${Int.MAX_VALUE})")
                    next()
                }
                return (if (neg) -v else v).toInt()
            }

            while (true) {
                skipWs()
                val c = cur()
                if (c == -1) break
                if (c == 'c'.code) { skipLine(); continue }
                if (c == 'p'.code) {
                    if (current.isNotEmpty()) fail("'p' inside a clause (the clause before it has no terminating 0)")
                    next()
                    if (cur() != ' '.code && cur() != '\t'.code) fail("expected a blank after 'p', found ${describe(cur())}")
                    while (cur() == ' '.code || cur() == '\t'.code) next()
                    for (ch in "cnf") {
                        if (cur() != ch.code) fail("expected 'p cnf', found ${describe(cur())} (only CNF is supported)")
                        next()
                    }
                    // Both upstreams accept header numbers spread over lines. If they are, the
                    // header must end its line (CaDiCaL: "expected new-line after 'p cnf ...'"):
                    // otherwise `p cnf 3` + `1 0` would read the unit's literal as the clause count.
                    val headerLine = line
                    skipWs()
                    val vars = parseInt("header variable count")
                    skipWs()
                    parseInt("header clause count")
                    if (line != headerLine) {
                        while (cur() == ' '.code || cur() == '\t'.code) next()
                        if (cur() != '\n'.code && cur() != '\r'.code && cur() != -1 && cur() != 'c'.code)
                            fail("expected end of line after a 'p cnf' header spread over lines, found ${describe(cur())}")
                    }
                    if (vars > numVars) numVars = vars
                    if (cur() == 'c'.code) skipLine()
                    continue
                }
                val lit = parseInt("literal")
                if (lit == 0) {
                    clauses.add(current.toIntArray())
                    current.clear()
                } else {
                    current.add(lit)
                    val v = if (lit < 0) -lit else lit
                    if (v > numVars) numVars = v
                }
                if (cur() == 'c'.code) skipLine()
            }
            if (current.isNotEmpty()) fail("last clause without terminating 0")
            return DimacsCnf(numVars, clauses)
        }
    }
}

/** Malformed DIMACS input ([DimacsCnf.parse]); [line] is 1-based. */
class DimacsParseException(val line: Int, val reason: String) :
    IllegalArgumentException("DIMACS parse error at line $line: $reason")
