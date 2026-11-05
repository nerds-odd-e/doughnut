---
id: SEED-105
status: active
planted: 2026-11-06
planted_during: wrap-up of SEED-104#clear-ci-docs-of-local-proof after story-branch CI stayed undiscovered for paths-ignored pushes
trigger_when: A Dough execution waits on CI for a revision that only changes paths-ignored files
scope: story
---

# SEED-105: Paths-ignored CI inherits ancestor status

<a id="paths-ignored-ci-ancestor-status"></a>

### Treat paths-ignored CI as the first non-ignored ancestor

**Identity:** SEED-105#paths-ignored-ci-ancestor-status
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A Dough execute-plan coordinator waiting on CI for a published
revision that only changes `paths-ignore` files receives that revision's CI
outcome as the status of the nearest ancestor that was not paths-ignored, so
the wait completes as a normal CI build instead of ending unobserved.

**Scope:** Change the installed Open Dough CI observation scripts (including
`ci-runs.mjs` and the path-applicability / coverage path that feeds
`complete-revision`) so a paths-ignored registered revision reuses the first
non-ignored ancestor attempt even when that attempt ran on another branch
(for example trunk). Agents keep using the ordinary mailbox completion path;
do not teach them a special "docs-only skip" handling. Do not change GitHub
`paths-ignore` policy itself.

**Key examples:**

1. A story branch whose Take and increments touch only ignored paths (for
   example `.planning/**` and `docs/**`) → registered SHAs finish observation
   with the CI status of the nearest non-ignored ancestor, not
   `undiscovered` / unobserved timeout.
2. Boundary: a revision that changes a non-ignored path still waits on its own
   exact-SHA run as today.

**Provenance:** SEED-104#clear-ci-docs-of-local-proof completion on
`cursor/clear-ci-proof-docs-of-local-proof-wording` at tip `34a142da59`;
observer `/tmp/dough-ci-501/watch-QowdUk` left both published SHAs
`undiscovered` because same-branch Actions history was empty while
`discoverApplicabilityCandidateRuns` only listed that branch.
