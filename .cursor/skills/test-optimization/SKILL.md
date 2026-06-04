---
name: test-optimization
description: >-
  Profile a test suite, optimize the slowest top 10% in phased groups, and
  close with a re-profile. Works for unit/integration tests in any sub-project
  (backend, frontend, cli, mcp-server) or Cypress E2E. Use when the developer
  asks to optimize, speed up, or profile slow tests, top 10% slowest, or test
  performance.
---

# Test Optimization

Systematic workflow distilled from repeated backend, frontend, CLI, and E2E
optimization runs. **Goal:** faster, deterministic tests — not more tests.

Start with `.cursor/agent-map.md` for sub-project test commands.

## Non-negotiable rules

Apply on every optimization pass:

1. **Remove or simplify redundant tests first** — merge overlapping scenarios,
   drop duplicate setup, delete tests that only repeat coverage elsewhere.
2. **No fixed-time waits** — no `sleep`, no `cy.wait(ms)` without an assertion,
   no arbitrary `setTimeout` / debounce-timeout polling in unit tests. Use
   assertions, intercept aliases, fake timers, or API/testability setup.
3. **Flaky is failure** — re-run touched tests until stable; fix root cause, do
   not mask with retries.

## When to use

- Full-suite or sub-project suite is slow; developer wants targeted wins.
- After a large feature landed and tests grew heavy.
- Before CI budget work — optimize the slow tail, not random files.

## Workflow overview

```
Profile full suite → rank tests by duration → take top 10%
→ choose smallest group count (see Grouping) → write plan in ongoing/
→ optimize one group per phase → verify → refactor → commit (if asked)
→ final re-profile → mark plan done
```

For **many phases**, use the **execute-plan** skill: coordinator delegates each
phase to a **fresh sub-agent**; each phase runs **post-change-refactor** before
commit. Do not accumulate context across phases in one agent.

## Step 1 — Profile

Run the **full** suite for the target scope once. Capture per-test durations.

| Scope | Profile command | Parse durations from |
|-------|-----------------|----------------------|
| **E2E** | `CURSOR_DEV=true nix develop -c pnpm cy:run-on-sut --reporter json` (SUT up; `pnpm sut:healthcheck`) | JSON blocks in stdout — tee to `/tmp/e2e-profile.log` |
| **Frontend** | `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vitest run --reporter=json` | Vitest JSON `testResults[].assertionResults[].duration` |
| **CLI** | `cd cli && CURSOR_DEV=true nix develop -c pnpm exec vitest run --reporter=json` | Same as Vitest |
| **Backend** | `CURSOR_DEV=true nix develop -c pnpm backend:test_only` then parse | `backend/build/test-results/test/TEST-*.xml` → `testcase@time` |
| **MCP server** | `CURSOR_DEV=true nix develop -c pnpm -C mcp-server exec vitest run --reporter=json` | Vitest JSON |

**Frontend note:** the `frontend:test` script runs Vitest **browser mode**
(`--browser=chromium`). The profile command above uses plain `vitest run` for
per-test `duration` data (matches prior optimization runs); verify changes with
`frontend:test` so browser-mode behavior is exercised.

**E2E JSON file output:** Cypress `--reporter-options output=…` may not write a
file; **tee stdout** and parse `{ "tests": [ { "title", "duration" } ] }` blocks
between spec runs.

**Do not commit** raw profile JSON (large, machine-specific). Store locally
(e.g. `ongoing/<scope>-profile-results.json` with a “do not commit” note, or
`e2e_test/reports/`). Record baseline wall time and test count in the plan.

## Step 2 — Select top 10%

```text
n_slow = max(1, ceil(total_tests * 0.10))
```

Sort all tests by `duration` descending; take the first `n_slow`.

Record in the plan: rank, seconds/ms, file/spec, test/scenario name.

## Step 3 — Grouping (smallest group count wins)

Compute group counts for these strategies:

| Strategy | How | Typical use |
|----------|-----|-------------|
| **By file** | One phase per file that contains ≥1 slow test | Few slow files, many tests/file |
| **Pairs of 2** | Slowest + 2nd, 3rd + 4th, … | E2E (~20 slow → 10 groups) |
| **Batches of 10** | Next 10 slow tests per phase | Unit tests (frontend had 137 slow → 14 groups; CLI 28 → 3) |

**Choose the strategy with the fewest groups.** Tie-break: prefer **by file** when
slow tests cluster in few files (less context switching).

Cap **~10 tests per phase** for unit/integration work so commits stay reviewable.
E2E often uses **pairs of 2** when that beats by-file (example: 20 slow across
13 files → 10 pair groups vs 13 file groups).

## Step 4 — Plan file

Copy [plan-template.md](plan-template.md) to `ongoing/<scope>-test-optimization.md`
and fill in the baseline, top-10% table, grouping, and one phase per group.

Read sub-project rules when editing tests: `e2e-authoring.mdc`, `frontend-testing.mdc`,
`backend-testing.mdc`, `cli.mdc`, etc.

## Step 5 — Optimize (tactics by layer)

Work **only** the tests in the current group. Prefer the first applicable tactic.

### All layers

- Delete or merge redundant tests; hoist shared setup to `beforeEach` / Background.
- Replace broad integration with narrower entry (pure helper, slimmer fixture).
- Parameterize (`it.each` / `@ParameterizedTest`) instead of copy-paste cases.

### Unit / component (Vitest, JUnit)

- **Frontend:** avoid `getByRole` / `findByRole` (slow visibility); use
  `data-testid`, `getByText`, `querySelector`. Replace `vi.waitUntil` / long
  `vi.waitFor` with `flushPromises`, `nextTick`, fake timers for debounce.
- **CLI:** share Ink helpers (`inkTestHelpers`, `recallInteractiveShared`-style);
  observable frame waits, not `frames.join` polls; `test.each` for escape/confirm variants.
- **Backend:** slimmer `makeMe` / fixtures; lighter multipart/OpenAPI setup;
  merge redundant `@Test` methods; avoid full-stack when controller slice suffices.

### E2E (Cypress + Cucumber)

- **Testability inject** instead of UI flows already covered elsewhere.
- **API setup** for recall/assimilation loops (wrong answers via API, one UI pass).
- **Direct routes** (`cy.visit` / `I route to the note`) vs catalog navigation.
- **Intercept waits** (`GET **/api/...`) vs `cy.reload()` or extra relogin.
- **Drop redundant steps** (extra reloads, duplicate rich-content checks, OCR when unnecessary).
- **Cache expensive prep** (e.g. skip MCP rebundle if artifact exists).
- **`invoke('val')` + `input`** instead of `cy.type()` on long markdown.

Never add `@focus` / `@only` in committed code.

## Step 6 — Verify

Run **focused** tests for the current group first, then widen if shared helpers changed.

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

For E2E groups, **3+ consecutive green runs** on touched specs before closing a phase.

## Step 7 — Close phase

1. **post-change-refactor** skill on the uncommitted diff (no commit from that sub-agent).
2. Lint/format for touched areas (`pnpm lint:all`, `pnpm format:all` when repo-wide).
3. Mark phase **done** in the plan; note before/after ms if re-profiled locally.
4. Commit when the developer asked for commits (message: `perf(<scope>): …`).

**Pre-commit scope:** ensure unrelated WIP is not staged (hooks may `git add -u`).

## Step 8 — Re-profile and close plan

After all phases:

- Re-run the same profile command as baseline.
- Record: test count, suite wall, top-10 table, top-10% **total CPU** (Vitest) or sum of slow scenarios (E2E).
- If full E2E re-profile is red (e.g. `Bad Gateway` on `cleanDB`), document that;
  use per-spec timings + CI for authoritative “after” — do not fake a green wall time.

Set plan **Status: done**, then **clean up `ongoing/`** (see below).

## Ongoing folder cleanup

When an optimization pass is **done**:

1. **Do not keep two plans for the same scope** — merge outcomes into one archive entry or delete.
2. **Move** a short summary to `ongoing/archive/<scope>-test-optimization-history.md` (see existing
   `ongoing/archive/test-optimization-history.md` for format).
3. **Delete** the working plan at `ongoing/<scope>-test-optimization.md` (or `e2e-slow-*.md` style names).
4. **Never commit** profile JSON; paths are gitignored (`e2e_test/reports/`, `ongoing/*-profile-results.json`).
5. Leave **active** product plans (features, roadmaps) in `ongoing/` root untouched.

If the user asks only to “clean up ongoing” after optimization: archive summary + delete completed
test plans; do not delete unrelated roadmaps or investigations.

## Jidoka — stop without committing

- Product/design fork (which scenario to drop).
- Credentials or external services you cannot control.
- Failures unrelated to your changes (SUT down, DB dirty).
- Ambiguous grouping when slow tests are tightly coupled across files.

Report what you measured, what blocked you, and what decision is needed.

## Experience summary (what worked)

| Area | Typical win |
|------|-------------|
| Frontend unit | −25% top-10% CPU; −5s wall on ~1370 tests — kill `getByRole`/polls, shared mount helpers |
| CLI unit | Shared recall mocks/waits; merge redundant interactive cases; ~280→278 tests |
| Backend unit | Slim fixtures; parameterized tests; merge duplicate advice/controller cases |
| E2E | Often **40–60%** per scenario — inject/API paths, drop reloads, remove duplicate scenarios |

**Common mistakes:** trusting a polluted full-suite re-profile; committing profile JSON;
one giant commit spanning unrelated files; optimizing UI when testability/API is enough;
keeping fixed waits “because it’s stable”.

## Parsing the E2E profile log

The JSON reporter prints one `{ "stats": …, "tests": [ { "title", "duration" } ] }`
block per spec to stdout (interleaved with the run table). After
`tee /tmp/e2e-profile.log`, parse it in Node:

- Track the current spec from `Running:  <name>.feature` lines.
- For each line that is a lone `{`, accumulate until braces balance and the
  buffer contains `"stats"`, then `JSON.parse` it.
- Collect `tests[].title` + `tests[].duration`, tag with the current spec, then
  sort descending and slice the top 10%.

Write a reusable `scripts/` helper only if the team will run this repeatedly;
otherwise a one-off inline Node script is enough.

## Related skills

- **execute-plan** — multi-phase coordinator + sub-agents + push per phase.
- **post-change-refactor** — cleanup before each commit.
- **phased-planning** — split an oversized phase.
