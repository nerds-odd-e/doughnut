---
id: SEED-109
status: active
planted: 2026-11-08
scope: story
---

# See CLI tests and timed scripts and infrastructure results in Intent

<a id="complete-test-inventory-and-script-proof"></a>

### See CLI tests and timed scripts and infrastructure results in Intent

**Identity:** SEED-109#complete-test-inventory-and-script-proof
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/085-unify-tooling-test-proof/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"6f8c04d1b9b4a01a1d35daba8e390ef10dd9000150a71cc66288ca6ff557e9eb","plan":"a0fe1507a0a6a0ab21d605485a016253d5f37c28464cd6a5181cc37d7e4cc80f"}}
```

**Goal** A Doughnut developer can find the CLI Vitest suite in Intent and use
one Scripts and Infrastructure group to see which Bash and Node.js checks a
selected CI attempt ran, how they ended, and how much test time they recorded.

**Scope**

- Register the existing CLI Vitest suite in `.planning/open-dough.json`,
  covering both TypeScript and TSX test files.
- Place the infrastructure test files under the same physical parent as
  the scripts tests. Use `scripts/` as that parent, with the three
  infrastructure tests under `scripts/test/path-routing/`. Keep routing
  and deployment implementation and configuration under `infra/`.
- Discover Bash `scripts/test/*.test` and Node.js `*.test.mjs` beneath the
  shared parent in one Scripts and Infrastructure group. Use the actual
  post-move repository paths for browsing and CI proof.
- Use the [existing attempt-specific CI proof summary](../../docs/ci-proof-summary.md)
  for the group's counts and cumulative recorded test execution time, at
  group and file level. Report checks run by the existing CI jobs. Count
  each Bash file invocation as one check, including the work it delegates,
  and use per-case outcomes from the directly selected Node.js suites.
  Include available failure results and durations. If a producer report
  is missing, show the gap alongside any reported proof.
- CLI CI proof, CLI Python inventory, and local proof collection are outside
  this story's delivery scope.

**Key examples**

1. On a published revision with CLI tests, opening Intent lists both
   `cli/tests/version.test.ts` and `cli/tests/InteractiveCliApp.test.tsx`
   under the CLI location.
2. Opening Scripts and Infrastructure shows
   `scripts/test/worktree_setup.sh.test`,
   `scripts/ci/ci-proof-outcomes.test.mjs`, and
   `scripts/test/path-routing/path-routing-validation.test.mjs` together.
   After a CI attempt reports those checks, Intent and the artifact for
   that attempt agree on the group and file counts and the sum of the
   recorded durations.
3. When a Bash file or Node.js case fails and reports a duration, that
   outcome and duration contribute to its file and the group. When an
   expected report is unavailable, the attempt shows missing proof for that
   part and keeps the results from available reports.

**Discovery contract** The common parent fits the current CI proof
contract. Open Dough 0.3.92 lets one Intent location declare `sources`
entries, each with `include` patterns and an optional `syntax` hint, so the
`scripts` location lists and opens Bash `.test` and Node.js `.test.mjs`
files together. That capability belongs to Open Dough; this story only
declares the location.
