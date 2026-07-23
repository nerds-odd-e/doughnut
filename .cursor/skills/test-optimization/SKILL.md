---
name: test-optimization
description: >-
  Profile a test suite, optimize the slowest top 10% in phased groups, and
  close with a re-profile. Works for unit/integration tests in any sub-project
  (backend, frontend, cli, mcp-server) or Cypress E2E. Use when the developer
  asks to optimize, speed up, or profile slow tests, top 10% slowest, or test
  performance. With `--resolve`, run resolve-only mode: triage the blacklist
  Candidates (tag / plan / ask) with no profiling or optimization.
---

<objective>
Profile a test suite, optimize the slowest top 10% in phased groups, and close
with a re-profile — faster, deterministic tests, not more tests.

Purpose: Systematic test-performance workflow for any sub-project (backend,
frontend, cli, mcp-server, E2E).

Output: Optimized tests with per-phase commits + summary ending with
`## TEST OPTIMIZATION COMPLETE`.
</objective>

<modes>
| Invocation | Mode | What runs |
|------------|------|-----------|
| `/test-optimization` (default) | **Optimize** | Full `<process>`: profile → select top 10% → plan → execute-plan → re-profile. |
| `/test-optimization --resolve` | **Resolve-only** | Run **only** the `resolve_candidates` step against `ongoing/test-optimization-blacklist.md`. **No profiling, no top-10% selection, no optimization.** |

When `--resolve` is given, skip every other step and go straight to
`resolve_candidates`.
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
(`.cursor/skills/execute-plan/SKILL.md`). Coordinator delegates each group to a
fresh sub-agent; each phase runs post-change-refactor, then commits and pushes.
Do not accumulate context across phases in one agent.

**E2E skip tag:** `@skipOptimizationDueToKnownNecessarySlowness` on a Scenario
or Feature marks known-necessary slowness. Profile runs exclude it via
`--env tags=…` (see `profile`). Adding the tag is a developer decision (Jidoka)
— propose only; do not add it yourself.

**Candidates:** `ongoing/test-optimization-blacklist.md` holds **Candidates**
from optimization runs (proposals only). Keep that section; do not invent a
Skip list there.

**Do not commit** raw profile JSON (large, machine-specific). Gitignored paths:
`e2e_test/reports/`, `.planning/*-profile-results.json`,
`.planning/quick/*-profile-results.json`, `ongoing/*-profile-results.json`.
</context>

<process>

<step name="resolve_candidates">
**Only runs in `--resolve` mode** (see `<modes>`). Triage every entry under
**Candidates** in `ongoing/test-optimization-blacklist.md`. Do **not** profile,
select a top 10%, or optimize any test here.

**Goal:** for each Candidate, decide whether the slow test earns its cost, or
whether a cheaper test gives the same protection.

For **each** Candidate:

1. **Read the actual test** (feature/scenario or unit test) plus the sibling
   scenarios in the same file and any unit/backend tests the blacklist note
   references. Confirm what unique behavior it actually protects.
2. **Weigh the slow test against alternatives.** Ask whether one or more **unit
   tests** (or a **mocked** E2E scenario) could give the **same coverage,
   behavioral protection, and external user-value clarity**. Remember: unit tests
   usually **cannot** reproduce the external user-value clarity of a genuine
   multi-step UI/PTY journey — so inherent-cost journeys stay as E2E.
3. **Distinguish inherent vs avoidable slowness.** Cost from genuine product
   behavior (full page load, PDF/canvas render, PTY/Ink startup, frontend session
   state not replicable via API) is *inherent*. Cost from a live network call,
   redundant setup, or coverage duplicated elsewhere is *avoidable* — do not label
   avoidable cost as "necessary".

**Resolve each Candidate with exactly one of:**

| Option | When | Action |
|--------|------|--------|
| **1. Tag** | Slowness is inherent to the behavior under test and no cheaper test matches its coverage + user-value clarity. | Add `@skipOptimizationDueToKnownNecessarySlowness` to that Scenario / Scenario Outline / Feature (tag the specific slow scenario, not the whole feature unless all of it is slow). |
| **2. Plan** | A unit test (or mocked scenario) can give the same coverage + behavioral protection + user-value clarity, so the slow test should be replaced/removed. | Add it to a phased plan via the **phased-planning** skill (`.planning/quick/NNN-slug/`) to remove the test and replace with the cheaper test(s). |
| **3. Ask** | No obviously logical decision (e.g. a genuine product / network / value trade-off). | Use `AskQuestion` to let the developer decide; then apply their choice. |

**Constraints:**

- **Zero or one plan total** across the whole resolve pass — batch replacements
  into a single phased plan if more than one Candidate needs option 2.
- Tagging is a **developer decision (Jidoka)** normally proposed, not auto-applied;
  in `--resolve` mode the developer has invoked resolve explicitly, so you **may
  apply option-1 tags directly** when the decision is obviously logical, and fall
  back to option 3 (ask) whenever it is not.

**After resolving:** for every Candidate you tag (option 1) or fold into a plan
(option 2), **delete its entry** from the Candidates list — do **not** keep a
"Resolved" archive in the blacklist file. Leave the Candidates header and the
`_(none)_` placeholder when the list is empty.

**Then stop** — report per the resolve output below; do not continue to `profile`.
</step>

<step name="profile">
Run the **full** suite for the target scope once. Capture per-test durations.

| Scope | Profile command | Parse durations from |
|-------|-----------------|----------------------|
| **E2E** | `CURSOR_DEV=true nix develop -c pnpm cy:run-on-sut --reporter json --env tags='not @ignore and not @skipOptimizationDueToKnownNecessarySlowness'` (SUT up; `pnpm sut:healthcheck`) | JSON blocks in stdout — tee to `/tmp/e2e-profile.log` |
| **Frontend** | `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --reporter=json` | Vitest JSON `testResults[].assertionResults[].duration` |
| **CLI** | `cd cli && CURSOR_DEV=true nix develop -c pnpm exec vitest run --reporter=json` | Same as Vitest |
| **Backend** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` then parse | `backend/build/test-results/test/TEST-*.xml` → `testcase@time` |
| **MCP server** | `CURSOR_DEV=true nix develop -c pnpm -C mcp-server exec vitest run --reporter=json` | Vitest JSON |

**E2E tags:** Always pass `--env tags='not @ignore and not @skipOptimizationDueToKnownNecessarySlowness'` for profile (and re-profile) so tagged scenarios/features are not run and do not enter the top 10%. CI default tags still apply for normal runs; this override is profile-only. In CI, also keep excluding `@wip` if you mirror CI: `not @ignore and not @wip and not @skipOptimizationDueToKnownNecessarySlowness`.

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
`ongoing/test-optimization-blacklist.md` **Candidates** for context only — do
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
Fill baseline, skip-tag note, top-10% table, grouping choice, one phase per
group, and a final re-profile phase.

Read sub-project rules when editing tests: `e2e-authoring.mdc`, `frontend-testing.mdc`,
`backend-testing.mdc`, `cli.mdc`, etc.
</step>

<step name="execute_via_execute_plan">
Hand plan to **execute-plan**. Do not optimize groups in the coordinator agent.

Each group phase (sub-agent):

1. Optimize only tests in that group (see `optimize_tactics`).
2. Verify with focused commands (see `verify`).
3. Run **post-change-refactor** (no commit from refactor sub-agent).
4. Lint/format per execute-plan wrap-up.
5. Mark phase **done** in plan.
6. **Commit** (`perf(<scope>): …`) and **push**.

**Hard-to-improve → Candidates / skip tag:** If no meaningful speedup after serious
attempt, or would need product/design trade-off:

1. Do **not** force a weak change.
2. Append under **Candidates** in `ongoing/test-optimization-blacklist.md`: file,
   test/scenario, duration, why hard, date (`YYYY-MM-DD`).
3. Mark phase done (or Jidoka-stop if value decision required).

**Promoting** a Candidate to permanent skip is a developer decision (Jidoka) —
propose tagging the Scenario or Feature with
`@skipOptimizationDueToKnownNecessarySlowness`; do not add the tag yourself.
</step>

<step name="optimize_tactics">
Work **only** tests in the current group. Prefer first applicable tactic.

### All layers

- Delete or merge redundant tests; hoist shared setup to `beforeEach` / Background.
- Replace broad integration with narrower entry (pure helper, slimmer fixture).
- Parameterize (`it.each` / `@ParameterizedTest`) instead of copy-paste cases.

### Unit / component (Vitest, JUnit)

- **Frontend:** avoid `getByRole` / `findByRole`; use `data-testid`, `getByText`,
  `querySelector`. Replace `vi.waitUntil` / long `vi.waitFor` with `flushPromises`,
  `nextTick`, fake timers.
- **CLI:** share Ink helpers (`inkTestHelpers`, `recallInteractiveShared`-style);
  observable frame waits, not `frames.join` polls; `test.each` for variants.
- **Backend:** slimmer `makeMe` / fixtures; lighter multipart/OpenAPI setup;
  merge redundant `@Test` methods; avoid full-stack when controller slice suffices.

### E2E (Cypress + Cucumber)

- **Testability inject** instead of UI flows covered elsewhere.
- **API setup** for recall/assimilation loops.
- **Direct routes** vs catalog navigation.
- **Intercept waits** (`GET **/api/...`) vs `cy.reload()` or extra relogin.
- **Drop redundant steps** (extra reloads, duplicate rich-content checks, OCR when unnecessary).
- **Cache expensive prep** (e.g. skip MCP rebundle if artifact exists).
- **`invoke('val')` + `input`** instead of `cy.type()` on long markdown.

Never add `@focus` / `@only` in committed code.
Never add `@skipOptimizationDueToKnownNecessarySlowness` without developer Jidoka.
</step>

<step name="verify">
Run **focused** tests for the current group first; widen if shared helpers changed.

```bash
# E2E (example)
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/a.feature,e2e_test/features/b.feature

# Frontend (example)
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/SomePage.spec.ts

# CLI
CURSOR_DEV=true nix develop -c pnpm cli:test

# Backend class
CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut....ClassName"
```

E2E groups: **3+ consecutive green runs** on touched specs before closing a phase.

**Pre-commit scope:** ensure unrelated WIP is not staged (hooks may `git add -u`).
</step>

<step name="reprofile_and_close">
After all group phases (via execute-plan):

- Re-run same profile command as baseline (same `--env tags=…` for E2E).
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
   PLAN and SUMMARY noise. See `ongoing/archive/test-optimization-history.md` for
   archive format.
3. **Never commit** profile JSON.
4. Leave **`ongoing/test-optimization-blacklist.md`** and active GSD milestone
   artifacts untouched.

If user asks only to clean up: remove completed test-opt plans; do not delete
blacklist or unrelated GSD dirs in progress.
</step>

<step name="parse_e2e_profile">
JSON reporter prints one `{ "stats": …, "tests": [ { "title", "duration" } ] }`
block per spec to stdout. After `tee /tmp/e2e-profile.log`, parse in Node:

- Track current spec from `Running:  <name>.feature` lines.
- For each lone `{`, accumulate until braces balance and buffer contains `"stats"`,
  then `JSON.parse`.
- Collect `tests[].title` + `tests[].duration`, tag with current spec, sort
  descending, slice top 10%. (Skip-tagged scenarios should already be absent
  when profile used the tag filter.)

Write reusable `scripts/` helper only if team will run repeatedly; otherwise
one-off inline Node script is enough.
</step>

</process>

<success_criteria>
**Optimize mode:**
- Full-suite profile captured with E2E skip tag excluded via `--env tags`
- Top 10% selected from eligible (profiled) tests
- Plan written and executed via execute-plan (commit + push per group)
- Non-negotiable rules applied (no redundant tests left, no fixed waits, no flaky)
- Re-profile recorded; plan marked done; spent history cleaned
- Final output includes `## TEST OPTIMIZATION COMPLETE`

**Resolve mode (`--resolve`):**
- Every Candidate resolved by exactly one of tag / plan / ask
- At most one phased plan created for all replacements
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
- Do not run full E2E suite for per-phase verify unless shared helpers require it.
</out_of_scope>
