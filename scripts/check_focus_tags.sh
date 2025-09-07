#!/bin/bash

# Script to check for @focus tags in e2e test feature files
# This prevents accidental commits of @focus tags that would skip other scenarios

set -e

FEATURE_DIR="e2e_test/features"
EXIT_CODE=0

echo "🔍 Checking for @focus tags in e2e test feature files..."

# Find all .feature files and search for @focus tags
FOCUS_FILES=$(find "$FEATURE_DIR" -name "*.feature" -exec grep -l "@focus" {} \; 2>/dev/null || true)

if [ -n "$FOCUS_FILES" ]; then
    echo ""
    echo "❌ ERROR: Found @focus tags in the following feature files:"
    echo ""
    
    for file in $FOCUS_FILES; do
        echo "📁 File: $file"
        # Show the specific lines with @focus tags
        grep -n "@focus" "$file" | while read -r line; do
            echo "   Line: $line"
        done
        echo ""
    done
    
    echo "🚫 @focus tags should only be used for local development and debugging."
    echo "   Please remove all @focus tags before committing to prevent other scenarios from being skipped in CI."
    echo ""
    echo "💡 To fix this:"
    echo "   1. Remove the @focus tags from the files listed above"
    echo "   2. Commit the changes"
    echo "   3. Push again"
    
    EXIT_CODE=1
else
    echo "✅ No @focus tags found in feature files. All good!"
fi

exit $EXIT_CODE
