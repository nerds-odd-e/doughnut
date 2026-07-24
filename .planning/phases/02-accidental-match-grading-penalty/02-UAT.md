---
status: testing
phase: 02-accidental-match-grading-penalty
source: [02-VERIFICATION.md]
started: 2026-07-24T01:00:00Z
updated: 2026-07-24T01:00:00Z
---

## Current Test

number: 1
name: Blank/whitespace spelling answer stays plain wrong
expected: |
  Graded plain wrong — correct=false, outcome=null, matchedNoteId=null (no accidental match).
awaiting: user response

## Tests

### 1. Blank/whitespace spelling answer stays plain wrong
expected: Graded plain wrong — correct=false, outcome=null, matchedNoteId=null (no accidental match).
result: [pending]

### 2. Multiple same-title matches → lowest note id
expected: matchedNoteId equals the lower note id (OrderByIdAsc + first readable).
result: [pending]

### 3. Title preferred over alias when both match
expected: matchedNoteId=A (title preferred); alias leg not used when a readable title match exists.
result: [pending]

## Summary

total: 3
passed: 0
issues: 0
pending: 3
skipped: 0
blocked: 0

## Gaps
