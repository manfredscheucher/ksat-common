package org.bytefred.ksat

/**
 * Kotlin transcription of CaDiCaL 2.1.3's `FixedAssignmentListener` (cadical.hpp). Literals are
 * signed DIMACS.
 *
 * Upstream documentation: the connected listener gets notified whenever the truth value of a
 * variable is fixed (for example during inprocessing or due to some derived unit clauses).
 * Listeners are real-time: fixed assignments found before connecting are not replayed
 * (solver.cpp `connect_fixed_listener`).
 */
interface FixedAssignmentListener {
    fun notifyFixedAssignment(lit: Int)
}

/**
 * Optional capability of a solver that can host a [FixedAssignmentListener] (CaDiCaL's
 * `Solver::connect_fixed_listener` / `disconnect_fixed_listener`). Like [ExternalPropagation],
 * only ports that translated the upstream machinery implement it.
 */
interface FixedListening {
    /** Connecting replaces (disconnects) a previously connected listener. Not during solving. */
    fun connectFixedListener(listener: FixedAssignmentListener)

    /** No-op without a connected listener. Not during solving. */
    fun disconnectFixedListener()
}
