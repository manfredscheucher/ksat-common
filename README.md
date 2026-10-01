# ksat-common

The shared base of the [ksat](https://github.com/manfredscheucher/sat-solvers-kotlin)
SAT-solver ports: the `SatSolver` interface, `SatResult`, the `Traceable` trace hook,
and the DIMACS parser. Package namespace is `org.bytefred.ksat`.

Every solver port (microSAT, MiniSat, CaDiCaL, kissat) implements `SatSolver`, so this
lives in its own repo and is pulled in as a git submodule (mounted as `common/`) by each
solver repo and by the main repo.

## Contents

- `SatSolver` — the common interface every port implements (`addClause`, `solve`,
  `solve(assumptions)`, `valueOf`, `numVars`) plus `SatResult { SAT, UNSAT }`.
- `Traceable` — the optional `setTraceSink` hook a port uses to emit its step-by-step
  decision trace (used to compare a port's run against its C original).
- `DimacsCnf` — a DIMACS CNF parser (`DimacsCnf.parse`) and a model check
  (`isSatisfiedBy`).

## Where the rest is

This repo is just the shared contract. The solver ports, the trace-comparison tests, the
C references, the benchmarks, the `Ksat` facade and the docs all live in the main project:
**[sat-solvers-kotlin](https://github.com/manfredscheucher/sat-solvers-kotlin)**.

## License

MIT, see [LICENSE](LICENSE).
