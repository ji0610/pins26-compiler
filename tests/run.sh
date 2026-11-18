#!/usr/bin/env bash
#
# Builds the compiler, runs every program in examples/ and compares the result
# with the expected one stored next to it.
#
#   examples/NAME.pins26 + NAME.out          must run; its output must match
#   examples/errors/NAME.pins26 + NAME.err   must be rejected with this message
#
# NAME.out holds what the program prints, followed by the machine's EXIT CODE
# line. That line also shows the final stack pointer, so an unbalanced stack
# fails the test too.

set -u
cd "$(dirname "$0")/.."

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

if ! log=$(javac --enable-preview --release 21 -d "$work/classes" \
        $(find src -name '*.java') 2>&1); then
    echo "$log"
    echo "The compiler does not build."
    exit 2
fi

run() {
    timeout 30 java --enable-preview -cp "$work/classes" pins26.phase.Machine "$1" \
        </dev/null >"$work/stdout" 2>"$work/stderr"
}

passed=0
failed=0

pass() {
    echo "ok    $1"
    passed=$((passed + 1))
}

fail() {
    echo "FAIL  $1: $2"
    [ -n "${3:-}" ] && echo "$3" | sed 's/^/      /'
    failed=$((failed + 1))
}

# Compares a result with its expected file and records the outcome.
compare() {
    local name=$1 expected=$2 actual=$3 difference
    if [ ! -f "$expected" ]; then
        fail "$name" "missing $expected"
    elif difference=$(diff -u --label expected --label actual "$expected" "$actual"); then
        pass "$name"
    else
        fail "$name" "wrong result" "$difference"
    fi
}

for source in examples/*.pins26; do
    name=$(basename "$source" .pins26)
    run "$source"
    status=$?
    if [ "$status" -ne 0 ]; then
        fail "$name" "exit status $status" "$(cat "$work/stderr")"
        continue
    fi
    # Drops the two banner lines at the top and the closing "Done" line.
    tail -n +3 "$work/stdout" | sed '$d' >"$work/actual"
    compare "$name" "examples/$name.out" "$work/actual"
done

for source in examples/errors/*.pins26; do
    name=$(basename "$source" .pins26)
    run "$source"
    status=$?
    if [ "$status" -ne 1 ]; then
        fail "errors/$name" "expected exit status 1, got $status" "$(cat "$work/stderr")"
        continue
    fi
    compare "errors/$name" "examples/errors/$name.err" "$work/stderr"
done

echo
echo "$passed passed, $failed failed"
[ "$failed" -eq 0 ] && [ "$passed" -gt 0 ]
