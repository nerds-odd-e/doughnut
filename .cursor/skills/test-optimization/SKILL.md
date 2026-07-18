---
name: test-optimization
description: >-
  Profile a test suite, optimize the slowest top 10% in phased groups, and
  close with a re-profile. Works for unit/integration tests in any sub-project
  (backend, frontend, cli, mcp-server) or Cypress E2E. Use when the developer
  asks to optimize, speed up, or profile slow tests, top 10% slowest, or test
  performance.
---

<objective>
Profile a test suite, optimize the slowest top 10% in phased groups, and close
with a re-profile — faster, deterministic tests, not more tests.

Purpose: Systematic test-performance workflow for any sub-project (backend,
frontend, cli, mcp-server, E2E).

Output: Optimized tests with per-phase commits + summary ending with
`## TEST OPTIMIZATION COMPLETE`.
</objective>

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

**Blacklist:** `ongoing/test-optimization-blacklist.md` (legacy path; read explicitly).
Skip entries under **Skip test optimization** (not **Candidates**). Match by
file path + test/scenario name (exact preferred).

**Do not commit** raw profile JSON (large, machine-specific). Gitignored paths:
`e2e_test/reports/`, `.planning/*-profile-results.json`,
`.planning/quick/*-profile-results.json`, `ongoing/*-profile-results.json`.
</context>

<process>

<step name="profile">
Run the **full** suite for the target scope once. Capture per-test durations.

| Scope | Profile command | Parse durations from |
|-------|-----------------|----------------------|
| **E2E** | `CURSOR_DEV=true nix develop -c pnpm cy:run-on-sut --reporter json` (SUT up; `pnpm sut:healthcheck`) | JSON blocks in stdout — tee to `/tmp/e2e-profile.log` |
| **Frontend** | `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --reporter=json` | Vitest JSON `testResults[].assertionResults[].duration` |
| **CLI** | `cd cli && CURSOR_DEV=true nix develop -c pnpm exec vitest run --reporter=json` | Same as Vitest |
| **Backend** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` then parse | `backend/build/test-results/test/TEST-*.xml` → `testcase@time` |
| **MCP server** | `CURSOR_DEV=true nix develop -c pnpm -C mcp-server exec vitest run --reporter=json` | Vitest JSON |

**Frontend note:** `frontend:test` runs Vitest **browser mode** (`--browser=chromium`).
Profile uses plain `vitest run` for `duration` data; verify changes with
`frontend:test`.

**E2E JSON:** Cypress `--reporter-options output=…` may not write a file; **tee
stdout** and parse `{ "tests": [ { "title", "duration" } ] }` blocks between
spec runs.

Store baseline locally (e.g. `.planning/quick/<scope>-profile-results.json` with
"do not commit" note). Record baseline wall time and test count in the plan.
</step>

<step name="blacklist_filter">
1. Read `ongoing/test-optimization-blacklist.md`.
2. Skip every test under **Skip test optimization**.
3. Eligible tests = profiled minus Skip entries.
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
Fill baseline, blacklist note, top-10% table, grouping choice, one phase per
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

**Hard-to-improve → blacklist candidates:** If no meaningful speedup after serious
attempt, or would need product/design trade-off:

1. Do **not** force a weak change.
2. Append under **Candidates** in `ongoing/test-optimization-blacklist.md`: file,
   test/scenario, duration, why hard, date (`YYYY-MM-DD`).
3. Mark phase done (or Jidoka-stop if value decision required).

**Moving** a candidate to **Skip test optimization** is a developer decision
(Jidoka) — propose only; do not move entries yourself.
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

- Re-run same profile command as baseline.
- Record: test count, suite wall, top-10 table, top-10% **total CPU** (Vitest) or
  sum of slow scenarios (E2E).
- Note any new **Candidates** proposed.
- If full E2E re-profile is red (e.g. `Bad Gateway` on `cleanDB`), document that;
  use per-spec timings + CI for authoritative "after" — do not fake a green wall time.

Set plan **Status: done**; **clean up spent plan history** (see `planning_cleanup`).
Keep the blacklist file.
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
- Collect `tests[].title` + `tests[].duration`, tag with current spec, drop Skip
  entries, sort descending, slice top 10%.

Write reusable `scripts/` helper only if team will run repeatedly; otherwise
one-off inline Node script is enough.
</step>

</process>

<success_criteria>
- Full-suite profile captured; top 10% selected after blacklist filter
- Plan written and executed via execute-plan (commit + push per group)
- Non-negotiable rules applied (no redundant tests left, no fixed waits, no flaky)
- Re-profile recorded; plan marked done; spent history cleaned
- Final output includes `## TEST OPTIMIZATION COMPLETE`
</success_criteria>

<output>
Report:

1. Scope and baseline vs after metrics.
2. Groups optimized and commits made.
3. Candidates proposed (if any).
4. Planning cleanup performed.

```
## TEST OPTIMIZATION COMPLETE
```
</output>

<out_of_scope>
- Do not optimize in the coordinator agent after plan is written.
- Do not commit profile JSON.
- Do not move blacklist entries to Skip without developer (Jidoka).
- Do not add `@focus` / `@only` in committed code.
- Do not run full E2E suite for per-phase verify unless shared helpers require it.
</out_of_scope>
