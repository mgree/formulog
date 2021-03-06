# Language basics

Formulog is an extension of Datalog designed to support program analyses that
use logical formulas, such as symbolic execution and refinement type checking.

A Formulog program consists of three main components:

* type definitions;
* relation declarations and definitions; and
* function definitions.

## Types

Formulog has a strong, static type system. Formulog has five built-in primitive
types:

* booleans (`bool`), i.e., `true` and `false`;
* signed 32-bit integers (`i32` or equivalently `bv[32]`), as in `42`;
* signed 64-bit integers (`i64` or equivalently `bv[64]`), as in `42L`;
* 32-bit floating point numbers (`fp32` or equivalently `fp[32]`), as in
  `42.0F`;
* 64-bit floating point numbers (`fp64` or equivalently `fp[64]`), as in `42.0`
  or `42.0D`; and
* string constants (`string`), as in `"hello"`.

In addition to these primitive types, Formulog allows users to define their own
(polymorphic) algebraic data types. For instance, this defines the standard
`list` type:

```
type 'a list =
  | nil
  | cons('a, 'a list)
```

Like types, constructors must begin with a lowercase letter.

Formulog also has support for tuple types, such as the type `i32 * string`, and
users can also define type aliases, such as this one that defines a map to be
an association list:

```
type ('k, 'v) map = ('k * 'v) list
```

Formulog currently has these algebraic types built-in (plus the types that
represent logical formulas, to be presented later):

```
type 'a list =
  | nil
  | cons('a, 'a list)

type 'a option =
  | none
  | some('a)

type cmp =
  | cmp_lt
  | cmp_eq
  | cmp_gt
```

Mutually recursive types are written using `and`:

```
type foo =
  | foo1
  | foo2(bar)
and bar =
  | bar1(foo)
  | bar2(bar, bar)
```

You can also define records, as here:
```
type 'a linked_list = {
  val  : 'a;
  next : 'a linked_list option;
}
```

Labels must be valid identifiers and cannot be shared across other types. For
each label, Formulog will automatically generate a function with that name that
extracts the relevant value from the record. For example, in the case of
`linked_list`, Formulog will generate:

```
val  : 'a linked_list -> 'a
next : 'a linked_list -> 'a linked_list option
```
Formulog also supports OCaml-style functional record update, as in this example:
```
type point3d = { x : i32; y : i32; z : i32 }
fun foo : i32 =
  let X = { x = 1; y = 2; z = 3 } in
  let Y = { X with x = -1; z = 0 } in
  x(Y) + y(Y) + z(Y)
```
A call `foo` would evaluate to the value `1`.

## Relations

In Formulog, there are three types of Datalog-style relations. The first type is
an `input` relation (also known as an extensional database or EDB relation).
This type of relation is enumerated explicitly.

```
type node = string
input edge(node, node)
edge("a", "b").
edge("b", "c").
edge("c", "b").
```

On the other hand, an `output` relation is defined using rules. For instance,
this predicate computes transitive closure over the previously defined `edge`
predicate:

```
output tc(node, node)
tc(X, Y) :- edge(X, Y).
tc(X, Z) :- tc(X, Y), edge(Y, Z).
```

A Formulog rule consists of a list of head atoms (the atoms to the left of the
`:-`) and a list of body atoms (the atoms to the right of the `:-`). An atom is
either a nullary predicate symbol (i.e., a predicate that takes no arguments) or
a n-ary predicate symbol followed by a parenthesized, comma-separated list of
terms. Each term is either

* a primitive like `42`;
* a variable like `X`;
* a constructed term like `some(X :: [2, 3])`;
* a term of the form `t not C`, where `t` is a term and `C` is a constructor
  symbol (this evaluates to `true` if the outermost constructor of `t` is not
  `C`); or
* a function call to a user-defined or built-in function like `i32_to_i64(42)`
  (functions are described in the next section).

Additionally, atoms in the body of a rule can be negated, as in the atom `!tc(X,
"c")`. Restrictions on the use of negation will be described later in this
guide.

Formulog also has two built-in binary predicates, `=` and `!=`:

```
output ok
ok :- X = "hello", X != "goodbye".
```

The first of these predicate is true when its arguments unify to the same term,
and the second is true when its arguments cannot be unified.

Finally, any Formulog term of type `bool` can be used in place of an atom in
the rule body, as here:

```
input foo(bool)
output p
p :- foo(X), X.
```

where the rule is translated to

```
p :- foo(X), X = true.
```

### External input relations

It is possible to specify that an input relation is enumerated externally in
(tab-separated) CSV files by annotating an input relation declaration with
`@external`, as in

```
@external
input foo(i32, i32, string list)

@external
input bar(string)
```

The Formulog runtime will look in the current directory for files called
`foo.csv` and `bar.csv`. The former might look like:

```
42  0 ["x"]
24  1 ["", " "]
100 -1 []
```

The latter might look like:

```
"hello"
"goodbye"
"ciao"
"aloha"
```

You can specify alternate directories to look in using the
`-DfactDirs=dir_1,...,dir_N` command line option. Every fact directory must
have a CSV file for _every_ external input relation (the file can be empty).

## Functions

Formulog allows users to define ML-style functions, that can then be invoked
from within Datalog-style rules. These functions can be polymorphic, but cannot
be higher-order. The functions must have explicit type annotations. For example,
here is a function for finding the nth element of a list:

```
fun nth(Xs : 'a list, N : i32) : 'a option =
  match Xs with
  | [] => none
  | X :: Xs =>
    if N < 0 then none
    else if N = 0 then some(X)
    else nth(Xs, N - 1)
  end
```

No special syntax is required for defining recursive functions, although
mutually recursive functions must be defined with `and`, as here:

```
fun neg_abs(X: i32) = if X > 0 then -X else X

fun is_even(X: i32) : bool =
  let X = neg_abs(X) in 
  X = 0 || is_odd(X + 1)
and is_odd(X: i32) : bool =
  let X = neg_abs(X) in
  X != 0 && is_even(X + 1)
```

We support some of the basic ML syntax constructions, like `match` and `let`.
However, you will find Formulog's syntax to be less flexible than most ML
implementations; for example, `some(X)` is okay but `some X` is not.

Despite the fact that we do not support higher-order functions, we do support
nested functions that can locally capture variables and we also support a
special parameterized term `fold`:

```
fold[f] : ['a, 'b list] -> 'a
```

where `f` is the name of a function of type `['a, 'b] -> 'a`. Here's an example
using both nested functions and `fold`:

```
fun rev(Xs: 'a list) : 'a list =
  let fun cons_wrapper(Xs: 'a list, X: 'a) : 'a list = X :: Xs in
  fold[cons_wrapper]([], Xs)
```

### Lifted predicates and aggregation 

Formulog allows any predicate (i.e., input predicates, output predicates, and
the built-in predicates `!=` and `=`) to be lifted to a boolean-returning
function. For instance, we can write code like this:

```
output bar(i32)

fun foo(N:i32) : bool = bar(N + 1)
```

Here, the function `foo(n)` returns `true` whenever the `bar` relation contains
`n + 1`.

Formulog supports aggregation through the wild card term `??`, which can be used
as an argument when "invoking" a predicate as a function. For example, given the
predicate `p` that relates a `bool` to an `i32`, we have:

* `p(true, 42)` returns a boolean (whether `true` is related to `42`)
* `p(true, ??)` returns a list of `i32` terms (the ones that are related to `true`)
* `p(??, 42)` returns a list of `bool` terms (the ones that are related to `42`)
* `p(??, ??)` returns a list of pairs constituting the relation

The use of lifted predicates must be stratified, as described in the "Program
Safety" document.

### Built-in functions

Finally, Formulog already has a bunch of basic functions built-in (mostly to do
with manipulating primitives):

* functions for basic mathematical operations for types `i32`, `i64`, `fp32`,
  and `fp64`:
    * addition (`*_add`), as in `fp32_add`
    * subtraction (`*_sub`)
    * multiplication (`*_mul`)
    * division (`*_div`)
    * remainder (`*_rem`)
    * negation (`*_neg`);
* functions for bitwise "and" (`*_and`), "or" (`*_or`), and "exclusive or"
  (`*_xor`), for types `i32` and `i64`;
* comparison operations `*_lt`, `*_le`, `*_gt`, `*_ge` for types `i32`, `i64`,
  `fp32`, and `fp64`;
* comparison operation `*_eq` for types `fp32` and `fp64` (this is floating
  point equality, as opposed to structural equality via the predicate `=`);
* boolean operators `!`, `&&`, and `||`;
* numeric primitive conversion operations, in the form `*_to_*` (e.g.,
  `i32_to_fp64`).

Standard arithmetic notation can be used for `i32` operations. For example,
`38 + 12 / 3` is shorthand for `i32_add(38, i32_div(12, 3))`.

Formulog supplies some `string` manipulation functions:

```
string_concat      : [string, string] -> string
string_cmp         : [string, string] -> cmp
string_matches     : [string, string] -> bool
string_starts_with : [string, string] -> bool
to_string          : 'a -> string
```

The function `string_matches` returns `true` when its first argument matches its
second argument, which can be a regular expression. The function
`string_starts_with` returns `true` when its second argument is a prefix of its
first argument.

Finally, for the purposes of debugging, Formulog supplies a `print` function of
type `'a -> bool`; it always evaluates to `true`.
