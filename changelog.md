# Changelog
All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
- Eager semi-naive evaluation algorithm.
- SMT manager that uses push and pop.
- Naive SMT manager that does not do any form of caching.

### Fixed
- Concurrency bug in the memoization of parameterized constructor symbols.
- Bug in the recording of rule evaluation diagnostics.
- Bug in the "freshening" of `let fun` expressions.

## [0.3.0] - 2020-02-03
### Added
- Support for `fold` terms.
- Options to restrict which results are printed after evaluation. 
- Nested, variable-capturing functions (i.e., `let fun` expressions).
- Added generic serialization function `to_string`.
- Preliminary (and undocumented) option to compile Formulog program to C++
  instead of interpreting it.

### Changed
- Do not require parentheses around tuple types.
- Do not reorder literals in rules (in order to preserve soundness of
  flow-sensitive type checking).
- Changed the names of some string-related built-in functions (`strcat` is now
  `string_concat` and `strcmp` is now `string_cmp`).
- Removed built-in function `string_of_i32`.

### Fixed
- Made Antlr parser faster by simplifying grammar.
- Changed precedence of infix cons operator (i.e., `::`).
- Added parameterized constructor symbols to fix type soundness issues arising
  from the non-determinate type signatures of some formula constructors.
- Made type checking flow-sensitive to soundly handle destruction of
  user-defined constructors in formulas. 

## [0.2.0] - 2019-11-25
### Added
- Support wild card term `??` when "invoking" predicates as functions.
- Constant array constructor `array_const` (from Z3's theory of arrays).
- Ability to do partial magic set rewriting with annotations `@bottomup` and
  `@topdown`.
- Demand transformation simplification for magic set rewriting (following Tekle
  and Liu [2010]).
- Support for record types. 
- Support external input facts via annotation `@external`.
- Support sequential runtime (for debugging) via `sequential` system property.
- Support existential anonymous variables in negated atoms.

### Changed
- Increased the amount of information printed with the `debugMst` option.
- Allow ML-style expressions to occur as logic programming terms.
- Prefix names of automatically-generated ADT testers and getters with `#`.
- Removed syntax highlighting for solver variables.
- Don't require periods after declarations and function definitions.
- Print thread name during SMT debugging.
- Make sure that the same SMT call is never made twice (with the same timeout).

### Fixed
- Fixed bug with applying type substitutions that contain mappings to (possibly
  nested) type variables.
- Updated name of formula type in Vim syntax file.
- Fixed a couple bugs in SMT-LIB parser.
- Fixed bug with missing case in unification algorithm.
- Boolean operators now short circuit.
- Reject programs that use top-down rewriting in combination with IDB
  predicates in the ML fragment.
- Make sure that EDB relations are maintained during top-down rewriting, even
  when they are only referenced in the ML fragment.

## [0.1.0] - 2019-04-21
### Added
- Everything (initial release).
