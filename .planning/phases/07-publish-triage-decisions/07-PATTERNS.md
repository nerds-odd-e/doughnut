# Phase 7: Publish triage decisions - Pattern Map

**Mapped:** 2026-08-03
**Files analyzed:** 3
**Analogs found:** 3 / 3

Phase 7 is **documentation/decision only** — no product code changes. The sole deliverable is a capability-named triage dossier that Phases 8–13 consume as their only action source.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `.planning/phases/07-publish-triage-decisions/TRIAGE.md` | config (decision dossier) | transform (audit → keep/strengthen/remove dossiers) | `.planning/milestones/v1.0-phases/02-empty-folder-findings/02-VERIFICATION.md` | role-match |
| `.planning/phases/07-publish-triage-decisions/07-CONTEXT.md` (optional pointer to `TRIAGE.md`) | config | transform | same file (`## Canonical References` / integration notes) | exact |
| `.planning/STATE.md` (progress after publish) | config | transform | same file (`## Current Position`) | exact |

**Not in scope for Phase 7 (inventory sources only, do not modify):** `cli/**`, `e2e_test/features/cli/**` — cite paths from these surfaces inside `TRIAGE.md`.

## Pattern Assignments

### `TRIAGE.md` (config / decision dossier, transform)

**Analog (evidence + file inventory + verdict tables):** `.planning/milestones/v1.0-phases/02-empty-folder-findings/02-VERIFICATION.md`

**Structure to copy — per-item truth table with Status + Evidence** (lines 31–45):
```markdown
### Observable Truths

| # | Truth | Status | Evidence |
| --- | ------- | ---------- | -------------- |
| 1 | Calling notebook Health lint reports folders whose entire subtree has no notes … | ✓ VERIFIED | `EmptyFolderHealthRule` … Tests: `listsEveryNestedFullyEmptyFolder` … — **PASS** |
```

**Adapt for triage:** replace `Truth` / `VERIFIED` with story dossier fields: **Verdict** (`keep` \| `strengthen` \| `remove`) + **Acceptance citations** + **Proof** (paths, commands, scenario names). One row-set (or section) per story 1–6.

**Structure to copy — Required Artifacts / delete-keep inventory** (lines 49–58):
```markdown
### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | ---------- | ------ | ------- |
| `backend/.../EmptyFolderHealthRule.java` | Fully-empty `HealthRule` bean | ✓ VERIFIED | Substantive recursive evaluate; `@Service`; … |
| `backend/.../EmptyFolderHealthRuleTest.java` | Predicate coverage | ✓ VERIFIED | Recursive, soft-delete, … |
```

**Adapt for triage (D-02):** for each story, publish three path lists:
1. **Capability entrypoints** (CLI command(s), main modules, matching E2E features)
2. **Delete/keep file set** (what Phases 8–13 should act on)
3. **Whole participant-touched inventory** under that story surface (superset; tag overlaps `shared` per D-03)

**Structure to copy — judgment-tier Verdict + Evidence** (lines 101–110):
```markdown
### Prohibitions (judgment-tier, evidence-backed)

| Prohibition | Verdict | Evidence |
| ----------- | ------- | -------- |
| must NOT mutate notebook data on lint path | satisfied | readOnly transaction; no writes in rule; folder-count test PASS |
```

**Adapt for triage (D-04):** WIP/gap rows use **label + one concrete proof** (not labels-only):
| Gap label | Proof |
| --------- | ----- |
| `@ignore` / half-wired | `e2e_test/features/cli/cli_push.feature` line 1 `@ignore`; scenario `A body edited locally reaches Doughnut` |
| wrong acceptance | missing acceptance bullet: "…" from portable-workspace note |
| no external value | command path only reachable via … |

---

**Analog (story acceptance citations — oracle text):** `.planning/notes/2026-07-24-portable-notebook-workspace.md`

**Cite acceptance examples verbatim per story** (story 1 excerpt, lines 10–26):
```markdown
### 1. Pull a notebook into a usable Markdown workspace
…
Acceptance examples:
- Notes and folders reproduce the notebook hierarchy with deterministic paths.
- Every exported note has stable Doughnut identity in frontmatter.
- …
- A failed pull reports what happened and does not present a partial workspace as successfully synchronized.
```

**TRIAGE-02 rule:** each story dossier must cite the matching bullets from this note (stories 7–10 out of scope).

---

**Analog (verdict vocabulary / keep bar):** `.planning/PROJECT.md`

**Decision bar** (lines 63–68):
```markdown
## Constraints

- **Authors:** Do not revert, rewrite, or "clean" commits/changes attributable to Terry Yin or Tan Yeong Sheng
- **Acceptance bar:** Story acceptance examples in the portable-workspace note are the keep/remove oracle
- **WIP:** Incomplete or `@wip` / half-wired work is remove-by-default unless strengthening is cheaper and clearly valuable
```

**Key Decisions table shape** (lines 71–78) — optional summary table at top of `TRIAGE.md`:
```markdown
| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Keep only correct + no WIP + external value | Class-ready mainline over preserving every experiment | — Pending |
```

Replace Outcome with the published story verdicts once audited.

---

**Analog (capability entrypoint + path table):** `docs/plans/2026-07-28-cli-export-notebook.md`

**Reusable-parts / path inventory table** (lines 63–74) — copy table shape for entrypoints:
```markdown
## Reusable parts — do not reimplement

| Purpose | Location |
| --- | --- |
| Download the export zip … | `downloadNotebookExportZip()` — `cli/src/backendApi/doughnutBackendClient.ts:350` |
| zip -> `Map<path, content>` | `unzipToEntries()` — `cli/src/sync/unzip.ts:43` |
| Notebook stage slash command registry | `notebookStageSlashCommandsFor()` — `cli/src/commands/notebook/notebookStageSlashCommands.ts:20` |
| … | `syncSlashCommandFor()` — `cli/src/commands/notebook/syncSlashCommand.tsx` |
| e2e file helpers | `readCliWorkspaceFile`, … — `e2e_test/config/cliE2ePluginTasks.ts` |
```

**Decisions block** (lines 49–60) — keep terse numbered/locked decisions when a story needs a short strengthen sketch (CONTEXT discretion: brief finish criteria only).

---

**Analog (consumer phase mapping):** `.planning/REQUIREMENTS.md`

**Traceability table** (lines 63–74) — mirror in `TRIAGE.md` header so Phases 8–13 find their dossier:
```markdown
| Requirement | Phase | Status |
|-------------|-------|--------|
| TRIAGE-01 | Phase 7 | Pending |
| TRIAGE-02 | Phase 7 | Pending |
| EXP-01 | Phase 8 | Pending |
| EXP-02 | Phase 9 | Pending |
| EXP-03 | Phase 10 | Pending |
| LINT-01 | Phase 11 | Pending |
| PUSH-01 | Phase 12 | Pending |
| PUSH-02 | Phase 13 | Pending |
```

**Adapt:** after publish, TRIAGE-01/02 → done; each story section points at its consumer phase (8→story 1 … 13→story 6).

---

### Suggested `TRIAGE.md` skeleton (compose from analogs)

```markdown
# Portable workspace triage (stories 1–6)

**Published:** YYYY-MM-DD
**Author filter:** exclude Terry Yin / Tan Yeong Sheng / `terryyin` variants (HYG-02)
**Oracle:** `.planning/notes/2026-07-24-portable-notebook-workspace.md`
**Consumers:** Phases 8–13 (sole action source)

## Summary

| Story | Capability | Verdict | Consumer phase |
|-------|------------|---------|----------------|
| 1 | pull/export | keep\|strengthen\|remove | 8 |
| … | … | … | … |

## Story N: <title from oracle>

### Verdict
keep | strengthen | remove

### Acceptance citations
- "<bullet from oracle>" — <match / gap / N/A with proof>

### Capability entrypoints
| Role | Path / command |
|------|----------------|
| CLI | `/export`, `/sync`, … |
| Module | `cli/src/…` |
| E2E | `e2e_test/features/cli/….feature` |

### Delete / keep file set
| Path | Action | Shared? |
|------|--------|---------|
| … | keep \| delete \| strengthen | shared → also Story M |

### Participant-touched inventory
(full list under story surface; duplicate shared paths with `shared` tag)

### WIP / gap signals (strengthen or remove only)
| Label | Proof |
|-------|-------|
| … | scenario / missing bullet / broken command path |
```

---

### `07-CONTEXT.md` optional pointer (config)

**Analog:** same file’s Canonical References block (lines 29–46).

After `TRIAGE.md` exists, add a single canonical pointer (do not duplicate dossiers):
```markdown
### Published triage (Phases 8–13 action source)
- `.planning/phases/07-publish-triage-decisions/TRIAGE.md`
```

---

### `.planning/STATE.md` progress update (config)

**Analog:** same file lines 30–35 (`## Current Position`).

After publish, update phase status / `stopped_at` to reflect triage published — do not embed story dossiers here.

## Shared Patterns

### Verdict vocabulary (keep / strengthen / remove)
**Source:** `.planning/PROJECT.md` lines 7–9, 63–68  
**Apply to:** Every story section in `TRIAGE.md`  
- **keep** = correct vs acceptance + no WIP + external user value  
- **strengthen** = keepable + minor gaps; Phase 7 locks verdict + gap list with proofs (D-04); implementation in 8–13  
- **remove** = WIP / incorrect / non-valuable (default for unfinished/`@wip`/half-wired unless strengthen is clearly cheaper)

### Acceptance citation
**Source:** `.planning/notes/2026-07-24-portable-notebook-workspace.md` stories 1–6  
**Apply to:** Every story dossier (TRIAGE-02)  
Cite the note’s acceptance bullets; do not invent criteria.

### Author filter (HYG-02)
**Source:** CONTEXT Claude’s Discretion + `PROJECT.md` Constraints  
**Apply to:** Evidence gathering for all dossiers  
Exclude Terry Yin, Tan Yeong Sheng, `terryyin` variants. In-scope LIA names include Eric Yeh, Ben Huang, etta.huang, Joy-kgo, and peers (e.g. Logan / XinxinKao on CLI). Mixed-author files: attribute by participant hunks/commits; do not rewrite Terry/YS-owned work.

### Shared-path duplication (D-03)
**Source:** CONTEXT D-03  
**Apply to:** Overlapping CLI/E2E modules (e.g. `sync/` helpers used by stories 1–3 and 5–6)  
List the same path under every related story’s inventory, tagged `shared` — never “listed only under story X.”

### WIP / gap proof (D-04)
**Source:** CONTEXT D-04 + live E2E signal  
**Apply to:** strengthen and remove dossiers  

Concrete WIP proof pattern already in tree — `cli_push.feature` (story 6 surface):
```gherkin
@ignore
@withCliConfig
@interactiveCLI
@disableOpenAiService
Feature: Push a local workspace into a notebook
```
(`e2e_test/features/cli/cli_push.feature` lines 1–5)

And half-wired CLI surface — push command documents dry-run only (`cli/src/commands/notebook/pushSlashCommand.tsx` lines 15–20):
```typescript
const pushDoc: CommandDoc = {
  name: '/push',
  usage: '/push --dry-run <workspace path>',
  description:
    'Preview what pushing the workspace would change in Doughnut. Only --dry-run is supported so far.',
}
```

Use these as **examples of proof shape**, not as pre-decided verdicts — Phase 7 audit decides keep/strengthen/remove from participant-only evidence.

### Capability entrypoint map (audit starting points)
**Source:** CONTEXT code insights + live `cli/` / `e2e_test/features/cli/`  
**Apply to:** Entrypoint rows in each story dossier  

| Story | CLI entrypoints (current tree) | Core modules | E2E feature |
|-------|--------------------------------|--------------|-------------|
| 1 pull/export | `/export`; pull side of `/sync` | `exportSlashCommand.tsx`, `writeNotebookExport.ts`, `exportNotebook.ts`, `applyPull.ts` | `cli_export.feature`, overlap with `cli_sync_pull.feature` |
| 2 preview-before-pull | `/sync --dry-run` | `syncSlashCommand.tsx`, `previewPull.ts`, `syncArgument.ts` | `cli_sync_dry_run.feature` |
| 3 incremental pull | `/sync` (non-dry-run) | `applyPull.ts`, `readWorkspace.ts`, baseline under `.doughnut-sync/` | `cli_sync_pull.feature` |
| 4 workspace lint | `/lint` | `lintSlashCommand.ts`, `lint/lintWorkspace.ts`, `okf*.ts` | `cli_lint_workspace.feature` |
| 5 push dry-run | `/push --dry-run` | `pushSlashCommand.tsx`, `previewPush.ts`, `pushArgument.ts` | `cli_push_dry_run.feature` |
| 6 safe push | `/push` (non-dry-run, if any) | push path beyond preview; related sync baseline | `cli_push.feature` (`@ignore` today) |

Notebook stage registration (entrypoint discovery):
```typescript
// cli/src/commands/notebook/notebookStageSlashCommands.ts lines 22–31
export function notebookStageSlashCommandsFor(
  notebook: Notebook
): readonly InteractiveSlashCommand[] {
  return [
    attachNotebookSlashCommandFor(notebook),
    syncSlashCommandFor(notebook),
    exportSlashCommandFor(notebook),
    pushSlashCommandFor(notebook),
    leaveNotebookStageSlashCommand,
  ]
}
```

`/lint` is registered outside notebook stage (`lintSlashCommand.ts` lines 7–19) — still story 4’s capability entrypoint.

### Sync dry-run vs mutate branch (stories 2–3 shared)
**Source:** `cli/src/commands/notebook/syncSlashCommand.tsx` lines 37–46  
```typescript
      return dryRun ? previewPull(request) : applyPull(request)
```
**Apply to:** Stories 2 and 3 dossiers — duplicate shared files with `shared` tag; verdicts may differ if participant work only lands on one branch.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | No prior `TRIAGE.md` in-repo; closest patterns are VERIFICATION dossiers + PROJECT decision bar + CLI export plan path tables (role-match, sufficient for planning) |

## Metadata

**Analog search scope:** `.planning/` (PROJECT, REQUIREMENTS, ROADMAP, STATE, notes, milestones VERIFICATION/PATTERNS), `docs/plans/`, `cli/src/commands/`, `cli/src/sync/`, `cli/src/lint/`, `e2e_test/features/cli/`  
**Files scanned:** ~40 planning + CLI/E2E surface files  
**Pattern extraction date:** 2026-08-03  
**Product code changes expected:** none (CONTEXT)
