# PINS'26 compiler

[![tests](https://github.com/ji0610/pins26-compiler/actions/workflows/tests.yml/badge.svg)](https://github.com/ji0610/pins26-compiler/actions/workflows/tests.yml)

A compiler for PINS'26, a small imperative language, written in Java for the
Compilers and Virtual Machines course at UL FRI (2026). It takes a source
file through every phase from characters to code for a stack machine, and
then runs that code.

```
fun putint(x)

fun main() =
    let
        var x = 6
    in
        putint(x * 7);
    end;
```

```
$ make test PHASE=Machine
This is PINS'26 compiler (pushdown machine):

42EXIT CODE (SP=65536): 1
:-) Done.
```

## Phases

Each phase is a class in `src/pins26/phase/` with its own `main`, so any
stage can be run and inspected on its own.

| Phase | Class | What it does |
|---|---|---|
| Lexical analysis | `LexAn` | hand-written scanner; tokens carry exact source positions |
| Syntax analysis | `SynAn` | recursive-descent parser that builds the abstract syntax tree |
| Abstract syntax | `Abstr` | prints the tree and attaches source locations to its nodes |
| Semantic analysis | `SemAn` | name resolution with nested scopes, checks on how names are used, l-value checks |
| Memory layout | `Memory` | frame sizes, offsets and static depth for functions, parameters and variables |
| Code generation | `CodeGen` | instructions for a pushdown (stack) machine |
| Execution | `Machine` | runs the generated code |

Shared data structures (tokens, AST, memory descriptors, instructions, error
reporting) are in `src/pins26/common/`.

## Build and run

Needs JDK 21.

```bash
javac --enable-preview --release 21 -d bin $(find src -name '*.java')
cd prg
make test PHASE=Machine     # compile and run prg/test.pins26
make test PHASE=CodeGen     # or stop after any phase: LexAn, SynAn, Abstr, SemAn, Memory
```

`make name PHASE=X` works for any `prg/name.pins26`.

## Examples and tests

`examples/` has small programs: recursion, nested functions, a sieve on a
global array, a linked list on the heap and string handling. Programs the
compiler has to reject are in `examples/errors/`. To run one after building:

```bash
java --enable-preview -cp bin pins26.phase.Machine examples/fibonacci.pins26
```

`./tests/run.sh` builds the compiler, runs all of them and compares each result
with the expected one stored next to the program. GitHub Actions runs the same
script on every push.

## Credits

The project skeleton comes from the course, by Prof. Boštjan Slivnik: the
overall structure, the shared data structures and the outline of each phase.
The implementation of the phases is my coursework.
