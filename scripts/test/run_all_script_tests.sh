#!/usr/bin/env bash
#
# Discover and run all script unit tests in the scripts/test directory
#
# This script finds all .test files and runs them using bash.
# Exit code will be non-zero if any test fails.
#
# When CI_PROOF_BASH_REPORT names a file, it also records one
# `<status><TAB><seconds><TAB><repo-relative path>` line per test file, where
# status is `passed` or `failed`, for the CI proof summary.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_DIR="$SCRIPT_DIR"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FAILED_TESTS=0
TOTAL_TESTS=0

if [ -n "${CI_PROOF_BASH_REPORT:-}" ]; then
    : >"$CI_PROOF_BASH_REPORT"
fi

# Appends a test file's outcome and its time since `started` to the report.
record() {
    [ -n "${CI_PROOF_BASH_REPORT:-}" ] || return 0
    local elapsed=$((${EPOCHREALTIME/./} - started))
    printf '%s\t%d.%06d\t%s\n' "$1" $((elapsed / 1000000)) \
        $((elapsed % 1000000)) "${2#"$REPO_ROOT"/}" >>"$CI_PROOF_BASH_REPORT"
}

# Find all .test files in the test directory
while IFS= read -r -d '' test_file; do
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo "Running: $test_file"
    started=${EPOCHREALTIME/./}

    if bash "$test_file"; then
        record passed "$test_file"
        echo "✓ Passed: $test_file"
    else
        record failed "$test_file"
        echo "✗ Failed: $test_file"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo ""
done < <(find "$TEST_DIR" -maxdepth 1 -name "*.test" -type f -print0 | sort -z)

# Summary
echo "=========================================="
echo "Test Summary:"
echo "  Total tests: $TOTAL_TESTS"
echo "  Passed: $((TOTAL_TESTS - FAILED_TESTS))"
echo "  Failed: $FAILED_TESTS"
echo "=========================================="

if [ $FAILED_TESTS -gt 0 ]; then
    exit 1
fi

exit 0

