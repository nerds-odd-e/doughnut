# Phase 14: Class-ready hygiene verify - Pattern Map

**Mapped:** 2026-08-03
**Files analyzed:** 9 (3 trash + 3 planning close + 2 verify artifacts + 1 protected no-touch) + invocation patterns
**Analogs found:** 9 / 9

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `docs/plans/2026-07-30-cli-push-dry-run-known-issues.md` | docs (DELETE) | transform (spent plan → gone) | Phase 13 trash of `cli_push.feature` | exact (trash + absence proof) |
| `docs/plans/2026-07-28-cli-export-notebook.md` | docs (DELETE) | transform | Phase 13 trash pattern | exact |
| `docs/plans/2026-07-28-export-notebook-markdown-zip.md` | docs (DELETE) | transform | Phase 13 trash pattern | exact |
| `.planning/REQUIREMENTS.md` | config / planning | transform (checkbox flip) | Phase 13 PUSH-02 close (`13-01-SUMMARY` + `13-PATTERNS`) | exact |
| `.planning/ROADMAP.md` | config / planning | transform | Phase 13 ROADMAP Phase-complete flip | exact |
| `.planning/STATE.md` | config / planning | transform | Phase 13 STATE handoff after PUSH-02 | exact |
| `14-01-SUMMARY.md` (create) | docs / verify | transform | `13-01-SUMMARY.md` coverage + Verification Evidence | exact |
| `14-VERIFICATION.md` (create) | docs / verify | transform | `07-VERIFICATION.md` goal truths + Requirements Coverage | role-match |
| `cli/src/sync/previewPullActions.ts` | utility (NO TOUCH) | — | Phases 10–13 HYG-02 “not in diff” | exact (constraint) |

**Keep unchanged (do not delete):**

| File / surface | Why |
|----------------|-----|
| `.planning/notes/2026-07-24-portable-notebook-workspace.md` | Living oracle (D-03) |
| `.planning/phases/07–13/**` including `TRIAGE.md` | Milestone diaries — archive deferred (D-03) |
| Five retained CLI features + dry-run `/push` code | HYG-03 proof surfaces |

**Must remain absent:**

| Path | Why |
|------|-----|
| `e2e_test/features/cli/cli_push.feature` | Phase 13 already trashed; D-04 confirm |
| `cli/src/sync/applyPush.ts` | No mutate push (D-04) |

## Pattern Assignments

### `docs/plans/*` D-02 trio (docs, DELETE)

**Analog:** Phase 13 trash of `e2e_test/features/cli/cli_push.feature` — `13-PATTERNS.md` delete command + `13-01-SUMMARY.md` lines 107–108

**Delete command pattern** (`general.mdc` line 12; Phase 13):
```bash
# Prefer trash over rm -f (general.mdc)
CURSOR_DEV=true nix develop -c trash \
  docs/plans/2026-07-30-cli-push-dry-run-known-issues.md \
  docs/plans/2026-07-28-cli-export-notebook.md \
  docs/plans/2026-07-28-export-notebook-markdown-zip.md
```

**Absence proofs after trash** (mirror Phase 13 D1/D2 coverage shape — `13-01-SUMMARY.md` lines 48–66):
```bash
test ! -e docs/plans/2026-07-30-cli-push-dry-run-known-issues.md
test ! -e docs/plans/2026-07-28-cli-export-notebook.md
test ! -e docs/plans/2026-07-28-export-notebook-markdown-zip.md
# WIP scan (D-04) — expect no matches / absent paths
test ! -e e2e_test/features/cli/cli_push.feature
! rg -n '@wip|@ignore' e2e_test/features/cli/
test ! -e cli/src/sync/applyPush.ts
! rg -n 'applyPush' cli/src/ --glob '*.ts*'
```

**Keep-set isolation** (D-03 — do not trash):
- `.planning/notes/2026-07-24-portable-notebook-workspace.md`
- `.planning/phases/**` (including TRIAGE + 07–13 diaries)
- Retained `cli_*` features and capability code

**Empty `docs/plans/` after trash:** Prefer leave empty dir or remove dir in same commit if git tracks it; do not recreate spent plans (RESEARCH Pitfall 5).

---

### `14-01-SUMMARY.md` + HYG-02 audit table (docs / verify)

**Analogs:**
1. `13-01-SUMMARY.md` — frontmatter coverage + Accomplishments + Verification evidence
2. Phases 10–12 SUMMARY “HYG-02: previewPullActions not in diff” one-liners
3. RESEARCH suggested audit table (discretion wording)

**Coverage / verification block shape** (`13-01-SUMMARY.md` lines 48–86):
```yaml
coverage:
  - id: D1
    description: "spent docs gone; no Story 1–6 WIP tags/features/modules"
    requirement: HYG-01
    verification:
      - kind: other
        ref: "test ! -e docs/plans/… && ! rg @wip|@ignore …"
        status: pass
  - id: D2
    description: "Terry/YS surfaces untouched (bounded audit)"
    requirement: HYG-02
    verification:
      - kind: other
        ref: "git log/blame previewPullActions + SUMMARY cites; file not in Phase 14 diff"
        status: pass
  - id: D3
    description: "retained CLI matrix green"
    requirement: HYG-03
    verification:
      - kind: unit
        ref: "pnpm cli:test"
        status: pass
      - kind: e2e
        ref: "five retained cli_*.feature specs"
        status: pass
```

**Standing HYG-02 one-liner cites to mirror** (do not rewrite instructors):
```markdown
# From 10-01-SUMMARY.md ~156:
- HYG-02: `git diff` excludes `cli/src/sync/previewPullActions.ts`

# From 12-01-SUMMARY.md ~147:
- HYG-02: `previewPullActions.ts` not in diff

# From 11-01-SUMMARY.md ~187:
- HYG-02: diff excludes `previewPullActions.ts`
```

**Suggested HYG-02 audit table** (record in SUMMARY and/or VERIFICATION — RESEARCH discretion):
```markdown
| Protected surface | Author evidence | Post–Phase-9 edits? | Phase 10–13 treatment | Verdict |
|-------------------|-----------------|---------------------|------------------------|---------|
| `cli/src/sync/previewPullActions.ts` | `git blame` / `git log` — Terry Yin | None after Phase 9 | Import-only (10/11/12 SUMMARY) | Untouched by removals/rewrites |
| Tan Yeong Sheng paths named in TRIAGE | TRIAGE author filter only — no YS path in delete/keep sets | N/A | N/A | No TRIAGE-named YS rewrite target |
```

**Audit commands** (D-05 — git without Nix):
```bash
git log --format='%h %an %s' -- cli/src/sync/previewPullActions.ts
git blame --line-porcelain cli/src/sync/previewPullActions.ts | rg '^author ' | sort | uniq -c
# Phase 14 implementation commit must exclude this file from diff
```

**TRIAGE author-filter anchor** (`TRIAGE.md` line 4 / ~509):
```markdown
**Author filter:** exclude Terry Yin / Tan Yeong Sheng / `terryyin` variants (HYG-02)
**Excluded authors (not triage basis):** Terry Yin, Tan Yeong Sheng, and `terryyin` variants.
```

---

### `14-VERIFICATION.md` (docs / verify)

**Analog:** `.planning/phases/07-publish-triage-decisions/07-VERIFICATION.md`

**Goal truths table shape** (`07-VERIFICATION.md` lines 21–28):
```markdown
## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Spent Stories 1–6 training docs / WIP gone (HYG-01) | ✓ VERIFIED | D-02 trio absent; WIP scan clean |
| 2 | Terry/YS untouched (HYG-02) | ✓ VERIFIED | Audit table + not in Phase 14 diff |
| 3 | Retained CLI units + five E2E green (HYG-03) | ✓ VERIFIED | cli:test + --spec matrix |
| 4 | Class can start without Stories 1–6 training debris | ✓ VERIFIED | Keep-set intact; mutate push absent |
```

**Requirements Coverage shape** (`07-VERIFICATION.md` lines 77–83):
```markdown
| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ---------- | ----------- | ------ | -------- |
| HYG-01 | 14-01 | Spent WIP/docs gone | ✓ SATISFIED | trash + scan |
| HYG-02 | 14-01 | Instructors untouched | ✓ SATISFIED | audit table |
| HYG-03 | 14-01 | Retained matrix green | ✓ SATISFIED | cli:test + 5 features |
```

**Prohibitions block** (mirror `07-VERIFICATION.md` lines 105–113):
```markdown
| Prohibition | Status | Evidence |
| ----------- | ------ | -------- |
| must NOT rewrite Terry/YS files | ✓ resolved | previewPullActions absent from commit diff |
| must NOT mass-delete `.planning/phases/07–13` | ✓ resolved | diaries retained |
| must NOT implement mutate push | ✓ resolved | applyPush / cli_push still absent |
| must NOT run full Cypress suite as HYG-03 gate | ✓ resolved | five-feature --spec only |
```

---

### `.planning/REQUIREMENTS.md` (+ ROADMAP / STATE) (planning close)

**Analog:** Phase 13 PUSH-02 close — `13-PATTERNS.md` lines 190–213; `13-01-SUMMARY.md` lines 111, 124–126

**HYG lines today** (`REQUIREMENTS.md` lines 32–34, 73–75):
```markdown
- [ ] **HYG-01**: WIP, incorrect, or non-valuable participant code for stories 1–6 is gone (...)
- [ ] **HYG-02**: Terry Yin and Tan Yeong Sheng changes remain untouched by this milestone’s removals/rewrites
- [ ] **HYG-03**: After triage actions, targeted CLI/unit and relevant CLI E2E for retained capabilities pass; the tree has no leftover training WIP for stories 1–6

| HYG-01 | Phase 14 | Pending |
| HYG-02 | Phase 14 | Pending |
| HYG-03 | Phase 14 | Pending |
```

**Close pattern** (flip only — do not rewrite requirement sentences):
```markdown
# REQUIREMENTS — flip checkboxes
- [x] **HYG-01**: …
- [x] **HYG-02**: …
- [x] **HYG-03**: …

# Traceability
| HYG-01 | Phase 14 | Complete |
| HYG-02 | Phase 14 | Complete |
| HYG-03 | Phase 14 | Complete |
```

**ROADMAP close** (`ROADMAP.md` lines 53, 196–224):
```markdown
# Milestone phase list
- [x] **Phase 14: Class-ready hygiene verify** - …

# Phase 14 section
**Plans**: 1/1 plans executed
# Progress table row → Complete + date
| 14. Class-ready hygiene verify | v1.2 | 1/1 | Complete | 2026-08-03 |
```

**STATE handoff** (mirror post–Phase-13 position language — `STATE.md` lines 28–39):
```markdown
**Current focus:** Milestone v1.2 — Phase 14 HYG-01/02/03 closed; class-ready handoff
## Current Position
Phase: 14 — Class-ready hygiene verify
Status: Complete — spent docs trashed; HYG-02 audit recorded; retained CLI matrix green
**Next:** `/gsd-complete-milestone` / `/gsd-cleanup` (bulk phase-dir archive) — not re-open Stories 1–6
# Decisions bullet:
- [Phase 14]: HYG-01/02/03 closed — D-02 docs trashed; instructor audit recorded; cli:test + five E2E green
```

**Commit bundling (D-08):** Prefer **one** implementation commit: trash D-02 trio + HYG-02 audit notes in SUMMARY/VERIFICATION + green matrix evidence + REQUIREMENTS/ROADMAP/STATE. Fall back to product/docs + planning split only if hooks force (slightly larger than Phase 13).

---

### HYG-03 Cypress + `cli:test` invocation (test runner, request-response)

**Analogs:**
1. `.planning/codebase/TESTING.md` lines 33–40
2. `.cursor/rules/e2e-authoring.mdc` lines 20–30
3. Prior phase single-feature gates (10/11/12/13 SUMMARYs)

**Units** (`TESTING.md`):
```bash
CURSOR_DEV=true nix develop -c pnpm cli:test
```

**Single-feature gate shape** (prior phases — copy Nix + `--spec`, never `-- --spec`):
```bash
# 12-01-SUMMARY.md ~146
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_push_dry_run.feature
# 10-01-SUMMARY.md ~155
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_sync_pull.feature
# 13-PATTERNS.md non-regression
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_push_dry_run.feature
```

**Phase 14 five-feature matrix** (D-06 — one invocation preferred; sequential OK on discovery failure):
```bash
# Assume pnpm sut already running (agent-map / e2e-authoring)
CURSOR_DEV=true nix develop -c pnpm cypress run --spec \
  "e2e_test/features/cli/cli_export.feature,e2e_test/features/cli/cli_sync_dry_run.feature,e2e_test/features/cli/cli_sync_pull.feature,e2e_test/features/cli/cli_lint_workspace.feature,e2e_test/features/cli/cli_push_dry_run.feature"
```

**Anti-pattern** (`e2e-authoring.mdc` line 30; `TESTING.md` line 39):
```bash
# WRONG — empty -- drops --spec
pnpm cypress run -- --spec e2e_test/features/cli/cli_export.feature
```

**Do not** run bare `pnpm cypress run` / full suite for HYG-03 (D-06 / deferred).

**Multi-minute E2E (~38 scenarios):** stated good reason to continue under planning.mdc time-budget exception for targeted test runtime (RESEARCH Open Question 2) — still one D-08 task.

---

### `cli/src/sync/previewPullActions.ts` (utility, NO TOUCH)

**Analog:** Standing HYG-02 constraint across Phases 10–13 SUMMARYs + TRIAGE author filter

**Rule for Phase 14:** Audit-only. Implementation commit `git diff` must exclude this file. Do not reformat, refactor, or “clean” it.

**Import-only consumers** (verify they remain consumers, not editors of this file): `applyPull.ts`, `previewPull.ts`, `previewPush.ts`, `diffReport.ts`, `portableContract.ts`.

## Shared Patterns

### Prefer `trash` for deletes
**Source:** `.cursor/rules/general.mdc` line 12; Phase 13 `13-PATTERNS.md` / `13-01-SUMMARY.md`  
**Apply to:** D-02 trio and any discretionary Story 1–6 orphan WIP  
```bash
CURSOR_DEV=true nix develop -c trash <path>
```

### WIP remove-by-default (absence proofs, no invent)
**Source:** Phase 13 `13-PATTERNS.md` Shared Patterns; PROJECT.md  
**Apply to:** HYG-01 scan — if orphan Story 1–6 WIP appears, trash it; do not finish mutate push  
```bash
test ! -e e2e_test/features/cli/cli_push.feature
! rg -n '@wip|@ignore' e2e_test/features/cli/
test ! -e cli/src/sync/applyPush.ts
```

### HYG-02 instructor filter (audit only)
**Source:** `TRIAGE.md` Author filter; Phases 10–13 SUMMARY one-liners  
**Apply to:** SUMMARY/VERIFICATION evidence; never rewrite `previewPullActions.ts`

### REQUIREMENTS / ROADMAP / STATE checkbox close
**Source:** Phase 13 PUSH-02 close (`13-PATTERNS.md` lines 190–213)  
**Apply to:** HYG-01/02/03 `[x]` + Traceability Complete + Phase 14 Progress Complete + STATE milestone-ready handoff

### Targeted Cypress `--spec` (not full suite)
**Source:** `e2e-authoring.mdc` lines 20–30; `TESTING.md` lines 37–40; prior CLI phase SUMMARYs  
**Apply to:** HYG-03 five retained features; one comma-separated `--spec` or five sequential runs

### Coarse one-plan / one-commit bundling (D-08)
**Source:** Phase 13 D-06 / `13-01-SUMMARY.md` lines 115–117; CONTEXT D-08  
**Apply to:** Entire Phase 14 — trash + audit notes + green matrix + planning close in one tracer

### Nix for tooling; git without Nix
**Source:** `general.mdc` / `agent-map.md`  
**Apply to:** `trash`, `cli:test`, `cypress run`; `git log` / `git blame` / `git commit` bare

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | None — trash, planning close, VERIFICATION, and `--spec` all have in-repo analogs. Multi-file comma `--spec` is Cypress CLI standard (discretion); no prior milestone SUMMARY shows five comma-separated CLI features in one line — fall back to sequential single-feature runs from Phases 10–13 if needed. |

## Metadata

**Analog search scope:** `.planning/phases/07–13/` SUMMARYs/PATTERNS/VERIFICATION, `.planning/REQUIREMENTS.md`, `ROADMAP.md`, `STATE.md`, `TRIAGE.md`, `.planning/codebase/TESTING.md`, `.cursor/rules/{general,e2e-authoring,planning}.mdc`, `docs/plans/`, `e2e_test/features/cli/`
**Files scanned:** ~18 primary analogs (13-01-SUMMARY, 13-PATTERNS, 12/11/10 SUMMARYs, 07-VERIFICATION, REQUIREMENTS, ROADMAP, STATE, TRIAGE, TESTING.md, e2e-authoring, general.mdc, three D-02 plan files)
**Pattern extraction date:** 2026-08-03

### Key Patterns for Planner

1. **Trash D-02 trio via Nix `trash`** + absence + WIP scan proofs — do not mass-delete `.planning/phases/` or the oracle note.
2. **HYG-02 = audit table + “not in diff”** — copy Phase 10–12 SUMMARY cites; never edit `previewPullActions.ts`.
3. **HYG-03 = `pnpm cli:test` + five targeted `--spec` features** — never full Cypress; avoid `cypress run -- --spec`.
4. **Close HYG-01/02/03 like Phase 13 PUSH-02** — flip REQUIREMENTS/ROADMAP/STATE; write SUMMARY + VERIFICATION evidence.
5. **One coarse plan / one task / prefer one commit** (D-08) — bundle trash + audit + green matrix + planning close.
