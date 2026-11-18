# PINS'26 compiler

[![tests](https://github.com/ji0610/pins26-compiler/actions/workflows/tests.yml/badge.svg)](https://github.com/ji0610/pins26-compiler/actions/workflows/tests.yml)

A compiler for a small imperative language, written in Java for the Compilers
and Virtual Machines course at UL FRI. It turns source code into code for a
stack machine and runs it.

## Run

Needs JDK 21.

```bash
javac --enable-preview --release 21 -d bin $(find src -name '*.java')
java --enable-preview -cp bin pins26.phase.Machine examples/fibonacci.pins26
```

```
This is PINS'26 compiler (pushdown machine):

0 1 1 2 3 5 8 13 21 34 55 89 144 233 377
EXIT CODE (SP=65536): 0
:-) Done.
```

More programs are in `examples/`. `./tests/run.sh` runs them all and checks the results.

## Structure

Each phase is a class in `src/pins26/phase/`:
`LexAn` → `SynAn` → `Abstr` → `SemAn` → `Memory` → `CodeGen` → `Machine`.

## Credits

The project skeleton comes from the course, by Prof. Boštjan Slivnik: the
overall structure, the shared data structures and the outline of each phase.
The implementation of the phases is my coursework.
