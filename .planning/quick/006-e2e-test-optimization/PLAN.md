# E2E test optimization (top 5%)

Status: in-progress

**Execution:** run via **execute-plan** (commit + push per slice). Sequential — slices share this PLAN file; batches 1–2 share `accidental_match_scheduling.feature`; batches 2 and 5 share `spaced_repetition.feature`.

**Scope override:** user asked for the slowest **5%** only (`n_slow = ceil(E × 0.05)`), not the skill default 10%.

## Profiling baseline (2026-08-17)

Command:

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run-on-sut --reporter json --env tags='not @ignore and not @wip and not @skipOptimizationDueToKnownNecessarySlowness'
```

- **251 tests**, suite wall ~**631s** (Cypress 10:31; process ~695s including startup)
- Eligible: **251** (profiled with skip-tag filter)
- Raw profile: `.planning/quick/006-e2e-test-optimization/e2e-profile-results.json` — **do not commit**

### Top 5% slowest (n = ceil(251 × 0.05) = 13)

| # | ms | file / spec | test / scenario |
|---|-----|-------------|-----------------|
| 1 | 7252 | `e2e_test/features/folder_organization/folder_organization.feature` | Moving a folder into another notebook merges same-name folder when confirmed |
| 2 | 5600 | `e2e_test/features/recall/accidental_match_scheduling.feature` | Ambiguous matches leave both tracked notes unchanged |
| 3 | 5509 | `e2e_test/features/users/account_control.feature` | Only admins can open failure reports (example #3) |
| 4 | 4968 | `e2e_test/features/recall/accidental_match_scheduling.feature` | Unique matched spelling tracker is brought forward without recall credit |
| 5 | 4960 | `e2e_test/features/note_topology/note_move.feature` | Move a note under a folder and undo |
| 6 | 4917 | `e2e_test/features/recall/spaced_repetition.feature` | Memory Tracker shows Stability and Again Difficulty after incorrect just-review |
| 7 | 4767 | `e2e_test/features/bazaar/bazaar_subscription.feature` | Assimilate notes from a Bazaar subscription |
| 8 | 4082 | `e2e_test/features/learning_session/commissioned_learning_session.feature` | Recording the tutor's report writes Feedback and schedules each tracker |
| 9 | 4077 | `e2e_test/features/testability/show_failure_report.feature` | Admin clears a failure report item |
| 10 | 3984 | `e2e_test/features/note_topology/note_tree_view.feature` | Open sidebar on a narrow window to see the note tree |
| 11 | 3973 | `e2e_test/features/note_creation_and_update/note_edit.feature` | Undo title edit restores previous title |
| 12 | 3959 | `e2e_test/features/recall/overlap_try_again.feature` | Shared non-distinguishing answer shows overlap try-again without credit |
| 13 | 3946 | `e2e_test/features/recall/spaced_repetition.feature` | Same-hour Good after first success grows Stability to 25 |

Top-5% total: **61994ms**.

### Grouping

- By file: **11** groups
- Batches of 3: **5** groups
- **Chosen:** batches of 3 (fewer groups)

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits.
3. Flaky = failure. Re-run touched specs **3+ consecutive green** before closing a slice.

Hard-to-improve tests: propose under **Candidates** in
`ongoing/test-optimization-blacklist.md`. Permanent skip (developer Jidoka only):
tag Scenario or Feature `@skipOptimizationDueToKnownNecessarySlowness`.

---

### 1. Folder merge, ambiguous accidental match, admin failure reports
Type: Structure
Status: done

**Learnings:** Folder merge now opens the source folder by injected id (~7.3s → ~2.4s). Ambiguous match injects credited spelling recalls and schedule checks (~5.6s → ~2.2s). Non-admin failure-report access skips the notebooks visit (~5.5s → ~4.7s). Spelling/recall inject helpers live in `e2e_test/start/testabilityRecall.ts` — reuse in slice 2.

**Tests:**
- `e2e_test/features/folder_organization/folder_organization.feature` — "Moving a folder into another notebook merges same-name folder when confirmed" (~7252ms)
- `e2e_test/features/recall/accidental_match_scheduling.feature` — "Ambiguous matches leave both tracked notes unchanged" (~5600ms)
- `e2e_test/features/users/account_control.feature` — "Only admins can open failure reports (example #3)" (~5509ms)

**Goals:**

- Prefer API/testability inject over extra UI login/navigation; drop redundant reloads and catalog hops.
- Ambiguous-match scenario has a long multi-assimilate / visit-tracker preamble — inject schedules and skip UI that sibling scenarios already cover.
- Account-control outline: keep one canonical access-denied path; avoid extra user-creation UI if inject exists. Example #3 is `non_admin` → access denied.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/folder_organization/folder_organization.feature,e2e_test/features/recall/accidental_match_scheduling.feature,e2e_test/features/users/account_control.feature
```

3+ consecutive green runs on those specs.

---

### 2. Unique spelling match, note move undo, incorrect just-review
Type: Structure
Status: planned

**Tests:**
- `e2e_test/features/recall/accidental_match_scheduling.feature` — "Unique matched spelling tracker is brought forward without recall credit" (~4968ms)
- `e2e_test/features/note_topology/note_move.feature` — "Move a note under a folder and undo" (~4960ms)
- `e2e_test/features/recall/spaced_repetition.feature` — "Memory Tracker shows Stability and Again Difficulty after incorrect just-review" (~4917ms)

**Goals:**

- Unique-match: inject the first credited recall rather than performing it in UI if a sibling already covers that path.
- Note move: direct route / inject destination; avoid extra notebook catalog navigation.
- Incorrect just-review: inject first Good if the GOOD RecallLog scenario already covers that UI; keep only the Again Stability/Difficulty assertions unique to this scenario.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/accidental_match_scheduling.feature,e2e_test/features/note_topology/note_move.feature,e2e_test/features/recall/spaced_repetition.feature
```

3+ consecutive green runs on those specs.

---

### 3. Bazaar assimilate, tutor report, clear failure report
Type: Structure
Status: planned

**Tests:**
- `e2e_test/features/bazaar/bazaar_subscription.feature` — "Assimilate notes from a Bazaar subscription" (~4767ms)
- `e2e_test/features/learning_session/commissioned_learning_session.feature` — "Recording the tutor's report writes Feedback and schedules each tracker" (~4082ms)
- `e2e_test/features/testability/show_failure_report.feature` — "Admin clears a failure report item" (~4077ms)

**Goals:**

- Bazaar: subscribe via API/inject if subscribe UI is already covered (sibling is skip-tagged); keep the two-day assimilate/recall sequence only as far as it uniquely proves subscription notes enter the daily plan.
- Tutor report: later scenarios inject recorded sessions — reuse that for schedule/Feedback assertions that do not need the report UI; keep one UI path for writing the report.
- Failure-report clear: inject the exception without extra admin UI hops; share login with the sibling if possible.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/bazaar/bazaar_subscription.feature,e2e_test/features/learning_session/commissioned_learning_session.feature,e2e_test/features/testability/show_failure_report.feature
```

3+ consecutive green runs on those specs.

---

### 4. Narrow sidebar, undo title, overlap try-again
Type: Structure
Status: planned

**Tests:**
- `e2e_test/features/note_topology/note_tree_view.feature` — "Open sidebar on a narrow window to see the note tree" (~3984ms)
- `e2e_test/features/note_creation_and_update/note_edit.feature` — "Undo title edit restores previous title" (~3973ms)
- `e2e_test/features/recall/overlap_try_again.feature` — "Shared non-distinguishing answer shows overlap try-again without credit" (~3959ms)

**Goals:**

- Narrow-window tree: keep viewport-specific open-sidebar; drop duplicate expand/assert if another scenario already checks the same tree shape.
- Undo title: do not repeat content-edit+undo already covered by "Undo content edit restores previous content"; unique claim is title undo emptying the stack.
- Overlap: inject Partner's schedule instead of visiting the tracker UI when the unique claim is try-again without credit.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature,e2e_test/features/note_creation_and_update/note_edit.feature,e2e_test/features/recall/overlap_try_again.feature
```

3+ consecutive green runs on those specs.

---

### 5. Same-hour Good Stability
Type: Structure
Status: planned

**Tests:**
- `e2e_test/features/recall/spaced_repetition.feature` — "Same-hour Good after first success grows Stability to 25" (~3946ms)

**Goals:**

- Inject first success / first Stability if the GOOD RecallLog scenario already covers just-review Yes; keep only the same-hour second Good → Stability 25 assertion.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/recall/spaced_repetition.feature
```

3+ consecutive green runs on that spec.

---

### 6. Re-profile and close
Type: Structure
Status: planned

Re-run the same profile command as baseline (same `--env tags=`). Record test count, suite wall, top-5% table, top-5% total time. Note any new Candidates. If full re-profile is red, document that and use per-spec timings — do not fake a green wall time.

Then mark this plan **done** and clean spent planning history (`planning.mdc`). Do not commit profile JSON. Leave `ongoing/test-optimization-blacklist.md` in place.

| Metric | Before | After |
|--------|--------|-------|
| Test count | 251 | |
| Suite wall | ~631s | |
| Top 5% total time | 61994ms | |

**Candidates proposed this run:** (none / list)

**Commits:**
