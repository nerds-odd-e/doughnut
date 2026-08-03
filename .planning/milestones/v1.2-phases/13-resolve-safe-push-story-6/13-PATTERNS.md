# Phase 13: Resolve safe push (story 6) - Pattern Map

**Mapped:** 2026-08-03
**Files analyzed:** 4 (primary delete/modify) + 6 (keep / non-regression) + 3 (planning close)
**Analogs found:** 4 / 4 primary

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `e2e_test/features/cli/cli_push.feature` | test (DELETE) | request-response (WIP mutate E2E) | TRIAGE Story 6 delete set + Phase 7 WIP-proof of this file | exact (sole Story-6-only delete) |
| `cli/src/commands/notebook/pushSlashCommand.tsx` | component (optional polish) | request-response | self `pushDoc` + durable `CommandDoc` tone from `syncSlashCommand.tsx` / `lintSlashCommand.ts` | exact / role-match |
| `cli/src/sync/pushArgument.ts` | utility (optional JSDoc polish) | transform | self — keep `parsePushArgument` rules; rephrase JSDoc only | exact |
| `.planning/REQUIREMENTS.md` (+ ROADMAP/STATE) | config / planning | transform | Phase 12 close `ea566e90df` (PUSH-01 checkbox flip) | exact |

**Keep unchanged (do not delete; non-regression only):**

| File | Role | How Phase 13 uses it |
|------|------|----------------------|
| `e2e_test/features/cli/cli_push_dry_run.feature` | test | D-03/D-05 dry-run E2E gate |
| `cli/src/sync/previewPush.ts` | service | Phase 12 PUSH-01 surface — untouched |
| `cli/src/sync/pushBaseline.ts` | utility | Shared keep — untouched |
| `cli/src/sync/diffReport.ts` (+ export/read/unzip helpers) | utility | Shared keep — untouched |
| `cli/tests/pushArgument.test.ts` | test | Proves mandatory `--dry-run` (D-05) |
| `cli/tests/previewPush*.test.ts`, `pushBaseline.test.ts` | test | Dry-run unit non-regression |
| `docs/plans/2026-07-30-cli-push-dry-run-known-issues.md` | docs | Story 5 spent note — Phase 14, not Phase 13 |

**Must remain absent (do not create):**

| Path | Why |
|------|-----|
| `cli/src/sync/applyPush.ts` (or any mutate-push module) | D-01 — remove phase; no strengthen |

## Pattern Assignments

### `e2e_test/features/cli/cli_push.feature` (test, DELETE)

**Analog:** TRIAGE Story 6 delete/keep table + Phase 7 WIP-proof excerpt of this same file  
**Action:** Delete via `trash` — do **not** invent green mutate scenarios to “close” PUSH-02

**WIP proof shape being removed** (lines 1–5, 26–36):
```gherkin
@ignore
@withCliConfig
@interactiveCLI
@disableOpenAiService
Feature: Push a local workspace into a notebook
```
```gherkin
  Scenario: A body edited locally reaches Doughnut
    ...
    And I enter the slash command "/push ./BenNotebook" in the interactive CLI
    Then I should see "1 note updated." in past CLI assistant messages
```
Mutate aspirational (`/push` without `--dry-run`, expects `1 note updated.`) — not executable in CI; sole Story-6-only participant artifact (Eric Yeh).

**Delete command pattern** (`general.mdc` / RESEARCH):
```bash
# Prefer trash over rm -f
CURSOR_DEV=true nix develop -c trash e2e_test/features/cli/cli_push.feature
# or host trash if already on PATH:
trash e2e_test/features/cli/cli_push.feature
```

**Absence proofs after delete** (D-05 — copy these into PLAN verification):
```bash
test ! -e e2e_test/features/cli/cli_push.feature
rg -n '^@ignore' e2e_test/features/cli/ || true   # expect no matches
test ! -e cli/src/sync/applyPush.ts
ls cli/src/sync/applyPush* 2>/dev/null && exit 1 || true
```

**Do not delete shared glue:** scenarios reuse interactive CLI steps; `"1 note updated."` also appears in `cli_sync_pull.feature` / `applyPull` — leave those.

**CI tag filter stays as-is** (`e2e_test/config/ci.ts` line 7) — deleting the file is stronger than relying on `@ignore`:
```typescript
tags: process.env.CI ? 'not @ignore and not @wip' : 'not @ignore',
```

---

### `cli/src/commands/notebook/pushSlashCommand.tsx` (component, optional D-04 polish)

**Analogs:**
1. Self — keep dry-run-only wiring (`previewPush` + `parsePushArgument` + `UsageErrorStage`)
2. Durable product `CommandDoc` tone — `syncSlashCommand.tsx` / `lintSlashCommand.ts` (no “so far” / “not yet”)

**Current WIP-toned help** (lines 16–20) — rephrase description only:
```typescript
const pushDoc: CommandDoc = {
  name: '/push',
  usage: '/push --dry-run <workspace path>',
  description:
    'Preview what pushing the workspace would change in Doughnut. Only --dry-run is supported so far.',
}
```

**Durable copy options** (planner picks one; avoid mutate promise):
```typescript
// Preferred (RESEARCH):
description:
  'Preview what pushing the workspace would change in Doughnut. Requires --dry-run.',
// Alt:
description:
  'Preview push changes without applying them. Only --dry-run is supported.',
```

**Keep wiring untouched** (lines 34–40, 63–75, 80–85):
```typescript
const runPreviewPush = useCallback(
  (signal: AbortSignal) =>
    previewPush({
      notebookId,
      workspacePath,
      exportNotebookAsZip: downloadNotebookExportZip,
      signal,
    }),
  [notebookId, workspacePath]
)
// ...
const parsed = parsePushArgument(argument)
return parsed.error === undefined ? (
  <PushRunStage ... />
) : (
  <UsageErrorStage message={parsed.error} ... />
)
```
Do **not** add mutate branch; do **not** change `usage` or `argument` shape.

**Durable CommandDoc tone analog** (`syncSlashCommand.tsx` lines 16–21) — states what the command does today, no WIP foreshadowing:
```typescript
const syncDoc: CommandDoc = {
  name: '/sync',
  usage: '/sync [--dry-run] <workspace path>',
  description:
    'Pull remote note changes into a local Markdown workspace, or preview them with --dry-run. Creates, updates, and moves Markdown notes to match the remote export; reserved and unsafe paths are rejected.',
}
```

---

### `cli/src/sync/pushArgument.ts` (utility, optional JSDoc polish)

**Analog:** self — acceptance rules must stay; discretionary comment cleanup only (D-04)

**Keep mandatory `--dry-run` core** (lines 3–5, 16–32):
```typescript
const DRY_RUN_FLAG = '--dry-run'
const USAGE = 'Usage: /push --dry-run <workspace path>'

export function parsePushArgument(argument: string | undefined): PushArgument {
  const trimmed = (argument ?? '').trim()
  if (trimmed === '') return { error: USAGE }
  if (!trimmed.startsWith(DRY_RUN_FLAG)) return { error: USAGE }
  // ... path after flag; reject misplaced flag
  return { workspacePath: stripSurroundingQuotes(workspacePart) }
}
```
Do **not** change `USAGE`, flag placement rules, or return shape.

**Current JSDoc WIP tone** (lines 11–15) — optional rephrase:
```typescript
/**
 * Read `--dry-run <workspace path>`. The flag is mandatory, unlike `/sync`'s:
 * a real, mutating push is not implemented yet, so any call without it is a
 * usage error rather than a second mode.
 */
```
**Durable JSDoc option** (RESEARCH):
```typescript
/**
 * Read `--dry-run <workspace path>`. The flag is mandatory, unlike `/sync`'s:
 * `/push` is dry-run-only, so any call without `--dry-run` is a usage error
 * rather than a second mode.
 */
```

**Non-regression unit to keep** (`cli/tests/pushArgument.test.ts` lines 29–34):
```typescript
test('rejects a workspace path with no dry run flag', () => {
  expect(parsePushArgument('./BenNotebook')).toEqual({
    error: 'Usage: /push --dry-run <workspace path>',
  })
})
```
No new test file required if units already cover USAGE (RESEARCH Wave 0).

---

### `.planning/REQUIREMENTS.md` (+ ROADMAP / STATE) (planning close)

**Analog:** Phase 12 docs commit `ea566e90df` — flip checkbox + Traceability Status; do **not** rewrite the requirement sentence into a mutate-push success claim

**PUSH-02 line today** (REQUIREMENTS.md ~line 28):
```markdown
- [ ] **PUSH-02**: Kept or strengthened push of existing notes matches story 6 (...) — or removed cleanly
```

**Close pattern** (mirror PUSH-01 close in `ea566e90df`):
```markdown
# REQUIREMENTS — flip checkbox only (wording already includes “or removed cleanly”)
- [x] **PUSH-02**: … — or removed cleanly

# Traceability
| PUSH-02 | Phase 13 | Complete |

# ROADMAP — Phase 13 [x]; 13-01 plan [x]; Progress table Complete
# STATE — note: PUSH-02 closed as **removed cleanly** (deleted cli_push.feature; dry-run kept)
```

Put “removed cleanly (deleted `cli_push.feature`)” in STATE decisions + SUMMARY — not as a rewrite of the checkbox sentence.

**Commit bundling (D-06):** Prefer **one** implementation commit: trash feature + optional help/JSDoc polish + REQUIREMENTS/ROADMAP/STATE/SUMMARY. Fall back to code + docs only if hooks force (slightly larger than Phase 12’s feat-then-docs split).

---

## Shared Patterns

### WIP remove-by-default (no invent)
**Source:** TRIAGE Story 6 verdict **remove**; PROJECT.md WIP remove-by-default; Phase 7 PATTERNS WIP proof  
**Apply to:** Phase 13 sole Behavior — delete `@ignore` mutate E2E; do not build `applyPush` or green mutate scenarios  
```bash
# Proof = absence, not green mutate E2E
test ! -f e2e_test/features/cli/cli_push.feature
! ls cli/src/sync/applyPush* 2>/dev/null
rg -n '^@ignore' e2e_test/features/cli/   # expect empty after delete
```

### Shared keep-set isolation (leave Phase 12)
**Source:** TRIAGE Story 6 keep table; Phase 12 PUSH-01 surface  
**Apply to:** Never delete or behavior-change these while removing Story 6 WIP  
Keep: `pushSlashCommand.tsx`, `previewPush.ts`, `pushArgument.ts`, `pushBaseline.ts`, `diffReport.ts`, `readWorkspace.ts`, `exportNotebook.ts`, `unzip.ts`, `writeNotebookExport.ts`, `notebookStageSlashCommands.ts`, `cli_push_dry_run.feature`, `previewPush*.test.ts`, `pushArgument.test.ts`, `pushBaseline.test.ts`.

### Mandatory `--dry-run` (do not relax)
**Source:** `cli/src/sync/pushArgument.ts` + `cli/tests/pushArgument.test.ts`  
**Apply to:** Any touch of pushArgument / pushSlashCommand — USAGE without flag must remain  

### Durable dry-run-only help (optional)
**Source:** Current `pushDoc` + `syncDoc` product-tone analog  
**Apply to:** Optional D-04 polish only — remove “so far” / “not implemented yet”; no mutate promise  

### REQUIREMENTS “removed cleanly” close
**Source:** `ea566e90df` Phase 12 PUSH-01 flip  
**Apply to:** PUSH-02 checkbox + Traceability Complete + ROADMAP/STATE; SUMMARY/STATE say **removed cleanly**, not “implemented”  

### Prefer `trash` for deletes
**Source:** `general.mdc`  
**Apply to:** `cli_push.feature` deletion  

### Non-regression gates
**Source:** RESEARCH Validation Architecture  
**Apply to:** After delete (± polish)  
```bash
# Fast gate
CURSOR_DEV=true nix develop -c bash -c 'cd cli && pnpm exec vitest run tests/pushArgument.test.ts tests/previewPush.test.ts tests/previewPush.create.test.ts tests/previewPush.conflict.test.ts tests/previewPush.directional.test.ts'

# Phase gate (assume pnpm sut)
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_push_dry_run.feature
```

### HYG-02 author filter
**Source:** TRIAGE / CONTEXT  
**Apply to:** Delete target = Eric Yeh `cli_push.feature`; optional polish = Ben Huang `pushSlashCommand` / `pushArgument`. Do not rewrite Terry Yin / Tan Yeong Sheng modules.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | None — this milestone’s first **remove** phase still has in-repo analogs (TRIAGE delete set, Phase 12 checkbox close, existing CommandDoc / pushArgument surfaces). No prior milestone phase deleted a CLI `@ignore` feature; use TRIAGE + `trash` + absence proofs as the delete pattern. |

## Metadata

**Analog search scope:** `e2e_test/features/cli/`, `cli/src/commands/notebook/`, `cli/src/sync/`, `cli/tests/`, `.planning/REQUIREMENTS.md`, Phase 7 TRIAGE/PATTERNS, Phase 12 close commit `ea566e90df`
**Files scanned:** ~14 primary (cli_push.feature, cli_push_dry_run.feature, pushSlashCommand, pushArgument, pushArgument.test, previewPush*, syncSlashCommand, lintSlashCommand, ci.ts, REQUIREMENTS, ROADMAP, STATE, TRIAGE Story 6, Phase 12 PATTERNS)
**Pattern extraction date:** 2026-08-03

### Key Patterns for Planner

1. **Remove = trash `cli_push.feature` + absence proofs** — do not implement mutate push / `applyPush`.
2. **Leave Phase 12 dry-run keep set untouched for behavior** — optional `pushDoc` / JSDoc tone only.
3. **Close PUSH-02 as removed cleanly** — flip checkbox like `ea566e90df`; STATE/SUMMARY say remove, not strengthen.
4. **One coarse plan / one task / prefer one commit** (D-06) — bundle delete + optional polish + planning close.
5. **Non-regression** — `pushArgument` + `previewPush*` units; targeted `cli_push_dry_run` E2E at gate.
