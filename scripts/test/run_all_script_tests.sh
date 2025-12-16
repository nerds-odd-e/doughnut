#!/usr/bin/env bash
#
# Discover and run all script unit tests in the scripts/test directory
#
# This script finds all .test files and runs them using bash.
# Exit code will be non-zero if any test fails.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_DIR="$SCRIPT_DIR"
FAILED_TESTS=0
TOTAL_TESTS=0

# Find all .test files in the test directory
while IFS= read -r -d '' test_file; do
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo "Running: $test_file"
    
    if bash "$test_file"; then
        echo "✓ Passed: $test_file"
    else
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

