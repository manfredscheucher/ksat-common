package org.bytefred.ksat

/**
 * Optional capability of a solver with CaDiCaL's `Solver::copy` (cadical.hpp: "Copy 'this' into a
 * fresh 'other'"). NOT a deep clone: like upstream, the copy gets the irredundant clauses and units,
 * the options and the variable flags, but no learned clauses and no assumptions. Both solvers have
 * the same models, and afterwards they are independent (clauses added to one do not reach the other).
 */
interface Copying {
    /** A fresh, independent solver over the same formula (upstream: requires the READY state). */
    fun copy(): SatSolver
}
