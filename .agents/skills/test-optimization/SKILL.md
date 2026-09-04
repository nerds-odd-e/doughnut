---
name: test-optimization
description: >-
  Profile a test suite, optimize the slowest top 10% in grouped slices, and
  close with a re-profile. Works for unit tests (JUnit/Vitest/…; "small test" style)
  in any sub-project (backend, frontend, cli, mcp-server) or Cypress E2E. Use when
  the developer asks to optimize, speed up, or profile slow tests, top 10% slowest,
  or test performance. With `--resolve`, run resolve-only mode: triage the blacklist
  Candidates (tag / plan / ask) with no profiling or optimization.
---

<objective>
Profile a test suite, optimize the slowest top 10% in grouped slices, and close
with a re-profile — faster, deterministic tests, not more tests.

Purpose: Systematic test-performance workflow for any sub-project (backend,
frontend, cli, mcp-server, E2E).

Output: Optimized tests with per-slice commits + summary ending with
`## TEST OPTIMIZATION COMPLETE`.
</objective>

<modes>
| Invocation | Mode | What runs |
|------------|------|-----------|
| `/test-optimization` (default) | **Optimize** | Full `<process>`: profile → select top 10% → plan → execute-plan → re-profile. |
| `/test-optimization --resolve` | **Resolve-only** | Run **only** the `resolve_candidates` step against `.planning/test-optimization-blacklist.md`. **No profiling, no top-10% selection, no optimization.** |

When `--resolve` is given, skip every other step and go straight to
`resolve_candidates`. Read
[resolving-candidates.md](references/resolving-candidates.md) in full first.
</modes>

<context>
**Mandatory first read:** `.cursor/agent-map.md` (sub-project test commands).

**Git does not use the Nix prefix.** All other repo tooling does:
`CURSOR_DEV=true nix develop -c …`

**Non-negotiable rules (every optimization pass):**

1. **Remove or simplify redundant tests first** — merge overlapping scenarios,
   drop duplicate setup, delete tests that only repeat coverage elsewhere.
2. **No fixed-time waits** — no `sleep`, no `cy.wait(ms)` without an assertion,
   no arbitrary `setTimeout` / debounce-timeout polling in unit tests. Use
   assertions, intercept aliases, fake timers, or API/testability setup.
3. **Flaky is failure** — re-run touched tests until stable; fix root cause, do
   not mask with retries.

**Execution model:** After writing the plan, **always** use **execute-plan**
(`.agents/skills/execute-plan/SKILL.md`). Coordinator delegates each group to a
fresh sub-agent and applies execute-plan's coordinator-owned wrap-up. Do not
accumulate context across slices in one agent.

**E2E skip tag:** `@skipOptimizationDueToKnownNecessarySlowness` on a Scenario
or Feature marks known-necessary slowness. Profile runs exclude it via
`--expose tags=…` (see `profile`). Adding the tag is a developer decision (Jidoka)
— propose only; do not add it yourself.

**Candidates:** `.planning/test-optimization-blacklist.md` holds **Candidates**
from optimization runs (proposals only). Keep that section; do not invent a
Skip list there.

**Do not commit** raw profile JSON (large, machine-specific). Gitignored paths:
`e2e_test/reports/`, `.planning/*-profile-results.json`,
`.planning/quick/*-profile-results.json`, `ongoing/*-profile-results.json`.
</context>

<process>
<step name="resolve_candidates">
Follow [resolving-candidates.md](references/resolving-candidates.md), then stop;
do not continue to `profile`.
</step>

<step name="profile">
Run the **full** suite for the target scope once. Capture per-test durations.

| Scope | Profile command | Parse durations from |
|-------|-----------------|----------------------|
| **E2E** | `CURSOR_DEV=true nix develop -c pnpm cy:run-on-sut --reporter json --expose tags='not @ignore and not @skipOptimizationDueToKnownNecessarySlowness'` (SUT up; `pnpm sut:healthcheck`) | JSON blocks in stdout — tee to `/tmp/e2e-profile.log` |
| **Frontend** | `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --reporter=json` | Vitest JSON `testResults[].assertionResults[].duration` |
| **CLI** | `cd cli && CURSOR_DEV=true nix develop -c pnpm exec vitest run --reporter=json` | Same as Vitest |
| **Backend** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` then parse | `backend/build/test-results/test/TEST-*.xml` → `testcase@time` |
| **MCP server** | `CURSOR_DEV=true nix develop -c pnpm -C mcp-server exec vitest run --reporter=json` | Vitest JSON |

**E2E tags:** Always pass `--expose tags='not @ignore and not @skipOptimizationDueToKnownNecessarySlowness'` for profile (and re-profile) so tagged scenarios/features are not run and do not enter the top 10%. CI default tags still apply for normal runs; this override is profile-only. In CI, also keep excluding `@wip` if you mirror CI: `not @ignore and not @wip and not @skipOptimizationDueToKnownNecessarySlowness`. Use `--expose` / `-x`, not `--env` — preprocessor v27 on Cypress >= 15.17 ignores `env.tags`.

**Frontend note:** `frontend:test` runs Vitest **browser mode** (`--browser=chromium`).
Profile uses plain `vitest run` for `duration` data; verify changes with
`frontend:test`.

**E2E JSON:** Cypress `--reporter-options output=…` may not write a file; **tee
stdout** and parse `{ "tests": [ { "title", "duration" } ] }` blocks between
spec runs.

Store baseline locally (e.g. `.planning/quick/<scope>-profile-results.json` with
"do not commit" note). Record baseline wall time and test count in the plan.
</step>

<step name="eligible_set">
Eligible tests = all profiled tests (E2E skip-tagged scenarios were already
excluded by the profile tag filter). Optionally read
`.planning/test-optimization-blacklist.md` **Candidates** for context only — do
not auto-exclude Candidates from the top 10%.
</step>

<step name="select_top_10_percent">
```text
n_slow = max(1, ceil(eligible_tests * 0.10))
```

Sort eligible by `duration` descending; take first `n_slow`. Record in plan:
rank, seconds/ms, file/spec, test/scenario name.
</step>

<step name="grouping">
Compute group counts for two strategies only:

| Strategy | How |
|----------|-----|
| **By file** | One group per file containing ≥1 slow test |
| **Batches of 3** | Consecutive slow tests in rank order, 3 per group (last may be smaller) |

**Choose the strategy with fewer groups.** Tie-break: prefer **by file**.
</step>

<step name="write_plan">
Copy [plan-template.md](plan-template.md) to
`.planning/quick/NNN-<scope>-test-optimization/PLAN.md` (or `phases/NN-slug/` PLAN).
Fill baseline, skip-tag note, top-10% table, grouping choice, one slice per
group, and a final re-profile slice.

Read sub-project rules when editing tests: `frontend.mdc` / `frontend-testing.mdc`,
`backend.mdc` / `backend-testing.mdc`, `e2e-authoring.mdc`, `cli.mdc`.
</step>

<step name="execute_via_execute_plan">
Hand plan to **execute-plan**. Do not optimize groups in the coordinator agent.

Each group slice (sub-agent):

1. Optimize only tests in that group (see `optimize_tactics`).
2. Verify with focused commands (see `verify`).
3. Return control for execute-plan's required refactor, formatting, plan update,
   `perf(<scope>): …` commit, and push sequence.

**Hard-to-improve → Candidates / skip tag:** If no meaningful speedup after serious
attempt, or would need product/design trade-off:

1. Do **not** force a weak change.
2. Append under **Candidates** in `.planning/test-optimization-blacklist.md`: file,
   test/scenario, duration, why hard, date (`YYYY-MM-DD`).
3. Mark slice done (or Jidoka-stop if value decision required).

**Promoting** a Candidate to permanent skip is a developer decision (Jidoka) —
propose tagging the Scenario or Feature with
`@skipOptimizationDueToKnownNecessarySlowness`; do not add the tag yourself.
</step>

<step name="optimize_tactics">
Read and follow [optimization-tactics.md](references/optimization-tactics.md).
</step>

<step name="verify">Read and follow
[verification.md](references/verification.md).
</step>

<step name="reprofile_and_close">
After all group slices (via execute-plan):

- Re-run same profile command as baseline (same `--expose tags=…` for E2E).
- Record: test count, suite wall, top-10 table, top-10% **total CPU** (Vitest) or
  sum of slow scenarios (E2E).
- Note any new **Candidates** proposed.
- If full E2E re-profile is red (e.g. `Bad Gateway` on `cleanDB`), document that;
  use per-spec timings + CI for authoritative "after" — do not fake a green wall time.

Set plan **Status: done**; **clean up spent plan history** (see `planning_cleanup`).
Keep the Candidates blacklist file.
</step>

<step name="planning_cleanup">
When optimization pass is **done**:

1. **Do not keep two plans for the same scope** — merge or delete duplicates.
2. Optionally keep one-line note in STATE/ROADMAP; otherwise delete spent `quick/`
   PLAN and SUMMARY noise (see `planning.mdc` history cleanup).
3. **Never commit** profile JSON.
4. Leave **`.planning/test-optimization-blacklist.md`** and active GSD milestone
   artifacts untouched.

If user asks only to clean up: remove completed test-opt plans; do not delete
blacklist or unrelated GSD dirs in progress.
</step>

<step name="parse_e2e_profile">
For E2E, follow [e2e-profile-parsing.md](references/e2e-profile-parsing.md).
</step>

</process>

<success_criteria>
**Optimize mode:**
- Full-suite profile captured with E2E skip tag excluded via `--expose tags`
- Top 10% selected from eligible (profiled) tests
- Plan written and executed via execute-plan (commit + push per group)
- Non-negotiable rules applied (no redundant tests left, no fixed waits, no flaky)
- Re-profile recorded; plan marked done; spent history cleaned
- Final output includes `## TEST OPTIMIZATION COMPLETE`

**Resolve mode (`--resolve`):**
- Every Candidate resolved by exactly one of tag / plan / ask
- At most one plan created for all replacements
- Resolved Candidates deleted from the blacklist (no "Resolved" archive kept)
- No profiling or optimization performed
- Final output includes `## CANDIDATES RESOLVED`
</success_criteria>

<output>
**Optimize mode** — report:

1. Scope and baseline vs after metrics.
2. Groups optimized and commits made.
3. Candidates proposed (if any).
4. Planning cleanup performed.

```
## TEST OPTIMIZATION COMPLETE
```

**Resolve mode (`--resolve`)** — report per Candidate: the decision (tag / plan /
ask) and its one-line rationale; the plan location if one was created; confirm the
blacklist Candidates list was pruned.

```
## CANDIDATES RESOLVED
```
</output>

<out_of_scope>
- Do not optimize in the coordinator agent after plan is written.
- Do not commit profile JSON.
- In optimize mode, do not add `@skipOptimizationDueToKnownNecessarySlowness` without developer Jidoka (in `--resolve` mode you may tag directly per the `resolve_candidates` step).
- Do not add `@focus` / `@only` in committed code.
- Do not run full E2E suite for per-slice verify unless shared helpers require it.
</out_of_scope>
