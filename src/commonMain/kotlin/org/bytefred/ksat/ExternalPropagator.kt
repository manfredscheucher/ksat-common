package org.bytefred.ksat

/**
 * IPASIR-UP user propagator, a Kotlin transcription of CaDiCaL 2.1.3's `ExternalPropagator`
 * (cadical.hpp). Method names are the upstream ones in camelCase; literals are signed DIMACS.
 *
 * Only C++ shapes that Kotlin cannot express are adapted:
 *  - `const std::vector<int>&` parameters are [IntArray]s;
 *  - the `bool &is_forgettable` out-parameter of `cb_has_external_clause` is an [OutBoolean].
 *
 * Upstream documentation, condensed:
 *  - [notifyAssignment] / [notifyNewDecisionLevel] / [notifyBacktrack]: assignments to observed
 *    variables and decision-level changes. Not necessarily eager; usually delivered right before
 *    the propagator callbacks are asked.
 *  - [cbCheckFoundModel]: check a complete model; return false to reject it, then provide an
 *    external clause on the next callback (or observe new variables).
 *  - [cbDecide]: next decision literal, or 0 to let the solver choose.
 *  - [cbPropagate]: a literal to propagate under the current assignment, or 0.
 *  - [cbAddReasonClauseLit]: the reason clause of an earlier [cbPropagate] literal, one literal
 *    per call, closed by 0; it must contain the propagated literal.
 *  - [cbHasExternalClause] / [cbAddExternalClauseLit]: add a clause during search, one literal per
 *    call closed by 0. Learned as irredundant unless marked forgettable.
 */
interface ExternalPropagator {
    /** Lazy propagator: only checks complete assignments (upstream `is_lazy`). */
    val isLazy: Boolean get() = false

    /** Reason clauses may be deleted by the solver (upstream `are_reasons_forgettable`). */
    val areReasonsForgettable: Boolean get() = false

    fun notifyAssignment(lits: IntArray)
    fun notifyNewDecisionLevel()
    fun notifyBacktrack(newLevel: Int)

    fun cbCheckFoundModel(model: IntArray): Boolean

    fun cbDecide(): Int = 0
    fun cbPropagate(): Int = 0
    fun cbAddReasonClauseLit(propagatedLit: Int): Int = 0

    fun cbHasExternalClause(isForgettable: OutBoolean): Boolean
    fun cbAddExternalClauseLit(): Int
}

/** Out-parameter holder for upstream `bool &` arguments. */
class OutBoolean(var value: Boolean = false)

/**
 * Optional capability of a solver that can host an [ExternalPropagator] (the IPASIR-UP block of
 * CaDiCaL's `Solver` API). Like [Traceable], only ports that translated the upstream machinery
 * implement it.
 */
interface ExternalPropagation {
    /** Only one propagator can be connected; all variables it cares about must be observed. */
    fun connectExternalPropagator(propagator: ExternalPropagator)

    /** Disconnecting also resets all observed variables. */
    fun disconnectExternalPropagator()

    /** Observe variable [v]; allowed for fresh variables and during solving. */
    fun addObservedVar(v: Int)

    /** Un-observe [v]; only between solve calls. */
    fun removeObservedVar(v: Int)

    fun resetObservedVars()

    /** True iff [lit] is an observed variable assigned by a decision in the CDCL loop. */
    fun isDecision(lit: Int): Boolean

    /** Backtrack to [newLevel]; only has an effect when called during [ExternalPropagator.cbDecide]. */
    fun forceBacktrack(newLevel: Int)
}
