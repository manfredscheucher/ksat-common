package org.bytefred.ksat

/**
 * Optional capability of a solver with CaDiCaL's forced decision phases (`Solver::phase` /
 * `Solver::unphase`, cadical.hpp: "Force the default decision phase of a variable to a certain
 * value"). Allowed during solving, e.g. from a propagator callback.
 */
interface ForcedPhases {
    /** Force the decision phase of the variable of [lit] to the sign of [lit]. */
    fun phase(lit: Int)

    /** Clear the forced phase of the variable of [lit] (no-op if none is set). */
    fun unphase(lit: Int)
}
