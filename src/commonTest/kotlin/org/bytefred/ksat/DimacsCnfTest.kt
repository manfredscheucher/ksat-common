package org.bytefred.ksat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * [DimacsCnf.parse]: nothing silently dropped, malformed input rejected with the line. The cases
 * and the upstream behaviour they follow (MiniSat parse_DIMACS non-strict, CaDiCaL read_dimacs
 * strict = 1) are listed in ksat-extra/doc/minisat-full-port.md ("DimacsCnf").
 */
class DimacsCnfTest {

    private fun clauses(text: String): List<List<Int>> = DimacsCnf.parse(text).clauses.map { it.toList() }

    private fun rejects(text: String, line: Int, reasonPart: String) {
        val e = assertFailsWith<DimacsParseException> { DimacsCnf.parse(text) }
        assertEquals(line, e.line, "line of: ${e.message}")
        assertTrue(reasonPart in e.reason, "'$reasonPart' not in '${e.reason}'")
        assertTrue(e.message!!.startsWith("DIMACS parse error at line $line: "), e.message)
    }

    @Test
    fun wellFormed() {
        val cnf = DimacsCnf.parse("c comment\np cnf 3 2\n1 -2 0\n2 3 -1 0\n")
        assertEquals(3, cnf.numVars)
        assertEquals(listOf(listOf(1, -2), listOf(2, 3, -1)), cnf.clauses.map { it.toList() })
    }

    @Test
    fun clausesBeyondTheHeaderCountAreKept() {
        // The old parser stopped after the header count and returned SAT for this UNSAT formula.
        assertEquals(listOf(listOf(1), listOf(-1)), clauses("p cnf 1 1\n1 0\n-1 0\n"))
        assertEquals(listOf(listOf(1, -2), listOf(2, 3)), clauses("p cnf 3 5\n1 -2 0\n2 3 0\n"))
    }

    @Test
    fun numVarsCoversEveryLiteral() {
        assertEquals(5, DimacsCnf.parse("p cnf 1 1\n5 -3 0\n").numVars)
        assertEquals(7, DimacsCnf.parse("p cnf 7 1\n1 0\n").numVars)
        assertEquals(2, DimacsCnf.parse("1 -2 0\n").numVars)                   // no header
        assertEquals(4, DimacsCnf.parse("p cnf 2 1\n1 0\np cnf 4 1\n-1 0\n").numVars)
        assertEquals(0, DimacsCnf.parse("p cnf -3 0\n").numVars)
        assertEquals(0, DimacsCnf.parse("").numVars)
    }

    @Test
    fun comments() {
        assertEquals(listOf(listOf(1, 2), listOf(-1)), clauses("p cnf 2 2\n1 2 0 c a comment\n-1 0\n"))
        assertEquals(listOf(listOf(1, 2)), clauses("p cnf 2 1\n1 c inside\n2 0\n"))
        assertEquals(listOf(listOf(1, 2)), clauses("p cnf 2 1\n1 2c glued\n0\n"))
        assertEquals(listOf(listOf(1), listOf(-2)), clauses("c a\n  c b\np cnf 2 2 c after header\n1 0\nc\n-2 0\nc end"))
    }

    @Test
    fun headerVariants() {
        for (h in listOf("p cnf 1 1", "p  cnf 1 1", "p\tcnf 1 1", "p cnf3 1", "p cnf\n1\n1", "p cnf +1 1"))
            assertEquals(listOf(listOf(1)), clauses("$h\n1 0\n"), h)
        assertEquals(emptyList(), clauses("p cnf 0 0"))
        // header numbers may span lines (both upstreams), but then the header must end its line
        // (CaDiCaL): otherwise the unit's literal 1 would silently become the clause count
        rejects("p cnf 3\n1 0\n", 2, "expected end of line")
        // a clause on the header line (MiniSat)
        assertEquals(listOf(listOf(1), listOf(-2, 3)), clauses("p cnf 3 2 1 0 -2 3 0"))
    }

    @Test
    fun numbersAndWhitespace() {
        assertEquals(listOf(listOf(1, -2)), clauses("p cnf 2 1\n+1 -2 0\n"))
        assertEquals(listOf(listOf(1, 2), listOf(-1)), clauses("p cnf 2 2\n1 2 -0\n-1 0\n"))
        assertEquals(listOf(listOf(1), listOf(-1, 2)), clauses("p cnf 2 2\r\n1 0\r\n-1 2 0\r\n"))
        assertEquals(listOf(listOf(1, 2)), clauses("p cnf 2 1\n1\u000b2\u000c0\n"))
        assertEquals(listOf(listOf(1, -2)), clauses("p cnf 3 1\n1-2 0\n"))
        assertEquals(listOf(emptyList()), clauses("p cnf 1 1\n0\n"))
        assertEquals(listOf(listOf(Int.MAX_VALUE)), clauses("2147483647 0"))
        assertEquals(listOf(listOf(-Int.MAX_VALUE)), clauses("-2147483647 0"))
    }

    @Test
    fun malformedInputIsRejected() {
        rejects("p cnf 2 1\n1 2\n", 3, "last clause without terminating 0")
        rejects("p cnf 2 2\n1 0\n1 2", 3, "last clause without terminating 0")
        rejects("p wcnf 1 1\n1 0\n", 1, "expected 'p cnf'")
        rejects("p cnf 2 1\n1 2 0\n%\n0\n", 3, "expected a digit (literal), found '%'")
        rejects("p cnf 3 1 x\n1 0\n", 1, "found 'x'")
        rejects("p cnf 2 1\n1 - 2 0\n", 2, "found ' '")
        rejects("p cnf 12 1\n12x 0\n", 2, "found 'x'")
        rejects("p cnf 1 1\n99999999999 0\n", 2, "literal too large")
        rejects("p cnf 1 1\n-2147483648 0\n", 2, "literal too large")
        rejects("p cnf 99999999999 1\n1 0\n", 1, "header variable count too large")
        rejects("p cnf 1\n", 2, "header clause count")
        rejects("p\n", 1, "expected a blank after 'p'")
        rejects("1 2\np cnf 2 1\n", 2, "'p' inside a clause")
        rejects("p cnf 2 1\n1 ä 0\n", 2, "found 'ä'")
    }

    @Test
    fun isSatisfiedBy() {
        val cnf = DimacsCnf.parse("p cnf 2 2\n1 2 0\n-1 0\n")
        assertTrue(cnf.isSatisfiedBy { it == 2 })
        assertTrue(!cnf.isSatisfiedBy { it == 1 })
    }
}
