---
id: SEED-127
status: active
planted: 2026-11-12
scope: story
---

# Verify testing guidance links

<a id="verify-test-guidance-links"></a>

### Verify testing guidance links

**Identity:** SEED-127#verify-test-guidance-links
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/095-verify-test-guidance-links/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"f52c77ddc56534dfcae1968a1387fe69adec25836124804c18f0525857f375f1","plan":"9151d68fd3a5962154f325e6108a61716eaf43841f06c91646b0d3b09e7bd0e7"}}
```

#### Goal

A Doughnut test author or refactorer can rely on maintained positive proof that
local testing guidance reaches the installed shared convention entrypoints and
retained local references.

#### Scope

Add one focused local link-resolution check for the five local testing skill
entrypoints and their current authoring route: E2E running/mock references,
shared MakeMe/page-object entrypoints, and shared test-entry selection guidance.
Validate their relative Markdown link destinations and local heading anchors,
with actionable source/destination diagnostics. Include the check in normal
lint validation; preserve independent test-entry membership selection.

This is routine proof strengthening from Intent Coverage, not a delivered
product defect. Keep it separate from the one-time duplication/removal reading
sweep. Do not assert prose wording or retired content, traverse all repository
documentation, fetch external links, convert application tests, or change
builders, page objects, application behavior or shared conventions.

#### Key examples

- Current MakeMe and page-object links and the E2E running-reference link resolve
  to real guidance; the focused check succeeds.
- A local shared-entrypoint destination is broken in scratch; the same command
  fails and identifies the referring source and destination. Restoring the
  link restores success.
- A local heading link resolves to its intended guidance section; a broken
  heading receives the same useful source/destination diagnostic.
- Selector membership proof retains its own purpose; link reachability is
  observed directly by the focused guidance check.

#### Plan and provenance

[Executable plan](../slice-plans/095-verify-test-guidance-links/PLAN.md).
Coverage-sourced correction from SEED-126 at
`924d9293c767d15f7247402d387ebb7fc8654f85`:
`.planning/seeds/SEED-126-use-shared-test-guidance.md#use-shared-test-guidance`
and `.planning/slice-plans/094-use-shared-test-guidance/PLAN.md` are recoverable
with `git show <revision>:<path>`. The predecessor's Done record retains the
full Intent Coverage report and wrap-up disposition.
