# Phase 10: Resolve incremental pull (story 3) - Pattern Map

**Mapped:** 2026-08-03
**Files analyzed:** 5
**Analogs found:** 5 / 5

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `cli/src/sync/applyPull.ts` | service | file-I/O + request-response | `cli/src/sync/previewPull.ts` (classify loop) + existing `applyPull.ts` (disk write) + `cli/src/sync/writeNotebookExport.ts` (baseline seed) | exact (compose) |
| `cli/src/commands/notebook/syncSlashCommand.tsx` | component | request-response | same file (help text / dry-run branch only) | exact |
| `cli/tests/applyPull.test.ts` | test | file-I/O | same file + `cli/tests/previewPullDiagnostics.test.ts` (move/reject fixtures) | exact / role-match |
| `e2e_test/features/cli/cli_sync_pull.feature` | test | request-response | same feature + `e2e_test/features/cli/cli_push_dry_run.feature` (baseline path assert) | exact / role-match |
| `e2e_test/step_definitions/cli_sync_pull.ts` (optional) | test | request-response | same file + `e2e_test/step_definitions/cli_workspace.ts` | role-match |

**Import-only (do not edit — HYG-02 / D-07):** `cli/src/sync/previewPullActions.ts`, `cli/src/sync/pushBaseline.ts`, `cli/src/sync/previewPull.ts`, `cli/src/sync/diffReport.ts`, `cli/src/sync/unzip.ts`, `cli/src/sync/readWorkspace.ts`.

## Pattern Assignments

### `cli/src/sync/applyPull.ts` (service, file-I/O + request-response)

**Analog (classify then act):** `cli/src/sync/previewPull.ts`  
**Analog (disk write):** current `cli/src/sync/applyPull.ts`  
**Analog (baseline persist):** `cli/src/sync/writeNotebookExport.ts` + `cli/src/sync/pushBaseline.ts`  
**Analog (baseline merge shape, push-side):** `cli/src/sync/previewPush.ts` (`load` → mutate map → `save`) — but **gate** save on mutations (unlike previewPush, which always saves).

**Imports pattern** — extend current apply imports with classify + zip names + baseline (mirror previewPull + export):

From `cli/src/sync/previewPull.ts` lines 1–12:
```typescript
import {
  renderDiffReport,
  renderNoteDiff,
  renderRejectFinding,
} from './diffReport.js'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import {
  classifyPreviewPullNotes,
  type ClassifiedPullNote,
} from './previewPullActions.js'
import { readWorkspace } from './readWorkspace.js'
import { listZipFileNames, unzipToEntries } from './unzip.js'
```

From `cli/src/sync/writeNotebookExport.ts` lines 1–5 (baseline call site):
```typescript
import { mkdirSync, writeFileSync } from 'node:fs'
import { basename, dirname, join } from 'node:path'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import { savePushBaseline } from './pushBaseline.js'
import { unzipToEntries } from './unzip.js'
```

Apply also needs `unlinkSync` (move) and `loadPushBaseline` (merge prior map). Keep `posix` join for workspace-relative paths as today.

**Core classify pattern** (`cli/src/sync/previewPull.ts` lines 64–75) — **copy this loop into apply**; replace `.map(renderClassifiedNote)` with disk mutations:
```typescript
  const workspace = readWorkspace(workspacePath)
  const { bytes } = await exportNotebookAsZip(notebookId, signal)
  const zipFileNames = listZipFileNames(bytes)
  const exported = unzipToEntries(bytes)

  const reported = classifyPreviewPullNotes(
    workspace,
    exported,
    zipFileNames
  ).map(renderClassifiedNote)
```

**Action taxonomy types** (`cli/src/sync/previewPullActions.ts` lines 109–127) — drive switch/if on these; do not re-classify:
```typescript
export type ClassifiedPullNote =
  | {
      readonly action: 'create' | 'update'
      readonly path: string
      readonly workspaceContent: string
      readonly exportContent: string
    }
  | {
      readonly action: 'move'
      readonly path: string
      readonly fromPath: string
      readonly workspaceContent: string
      readonly exportContent: string
    }
  | {
      readonly action: 'reject'
      readonly path: string
      readonly reason: string
    }
```

**Disk write pattern** (current `cli/src/sync/applyPull.ts` lines 46–49) — reuse for create/update/move destination:
```typescript
    const full = join(workspacePath, ...path.split(posix.sep))
    mkdirSync(dirname(full), { recursive: true })
    writeFileSync(full, remote, 'utf8')
```

For **move**: write `note.path` with `note.exportContent`, then `unlinkSync` on `join(workspacePath, ...note.fromPath.split(posix.sep))` only (RESEARCH A2; never unlink local-only).

**Reject reporting pattern** (`cli/src/sync/diffReport.ts` lines 76–78):
```typescript
export function renderRejectFinding(path: string, reason: string): string {
  return [`${path} (reject)`, `  ${reason}`, ''].join('\n')
}
```

**Summary / no-op sentinel** (current `cli/src/sync/applyPull.ts` lines 9–21) — keep `NOTHING_TO_PULL` for zero mutations; extend count/wording for create/update/move; rejects-only must not return bare no-op alone (align preview rejects-only):
```typescript
export const NOTHING_TO_PULL = 'No changes to pull.'

function summary(updated: number): string {
  if (updated === 0) return NOTHING_TO_PULL
  return updated === 1 ? '1 note updated.' : `${updated} notes updated.`
}
```

**Baseline save after mutate** (`cli/src/sync/writeNotebookExport.ts` lines 88–92) — call only when ≥1 create/update/move applied:
```typescript
  savePushBaseline(
    root,
    notebookId,
    new Map(entries.filter(([path]) => path.endsWith(MARKDOWN_SUFFIX)))
  )
```

**Baseline merge (discretion A1)** — load prior, patch applied paths (not full export refresh):

`cli/src/sync/pushBaseline.ts` lines 25–50:
```typescript
export function loadPushBaseline(
  workspacePath: string,
  notebookId: number
): ReadonlyMap<string, string> {
  const path = join(workspacePath, BASELINE_RELATIVE_PATH)
  if (!existsSync(path)) return new Map()

  const parsed = JSON.parse(readFileSync(path, 'utf8')) as PushBaselineFile
  if (parsed.notebookId !== notebookId) return new Map()

  return new Map(Object.entries(parsed.notes))
}

export function savePushBaseline(
  workspacePath: string,
  notebookId: number,
  notes: ReadonlyMap<string, string>
): void {
  const path = join(workspacePath, BASELINE_RELATIVE_PATH)
  mkdirSync(dirname(path), { recursive: true })
  const file: PushBaselineFile = {
    notebookId,
    notes: Object.fromEntries(notes),
  }
  writeFileSync(path, JSON.stringify(file), 'utf8')
}
```

Recommended merge (from RESEARCH Pattern 2): `const next = new Map(loadPushBaseline(...))`; for applied create/update `next.set(path, exportContent)`; for applied move `next.delete(fromPath); next.set(path, exportContent)`; `savePushBaseline` only if mutation count ≥ 1.

**Anti-pattern to remove** — workspace-only loop (`cli/src/sync/applyPull.ts` lines 40–52) and docstring claiming remote-only notes are not created (lines 23–28).

---

### `cli/src/commands/notebook/syncSlashCommand.tsx` (component, request-response)

**Analog:** same file — optional help-text polish only; routing already correct.

**Dry-run vs apply branch** (lines 37–45) — do not change:
```typescript
      return dryRun ? previewPull(request) : applyPull(request)
```

**Stale help text** (lines 16–21) — update description if touching this file (RESEARCH A5):
```typescript
const syncDoc: CommandDoc = {
  name: '/sync',
  usage: '/sync [--dry-run] <workspace path>',
  description:
    'Pull remote note changes into a local Markdown workspace, or preview them with --dry-run. Only updates files that already exist locally and match an exported note path.',
}
```
Replace “Only updates files that already exist…” with wording that covers create/update/move (and reserved rejects). Prefer not restructuring the Ink stage.

---

### `cli/tests/applyPull.test.ts` (test, file-I/O)

**Analog:** same file (harness) + `cli/tests/previewPullDiagnostics.test.ts` (taxonomy fixtures) + `cli/tests/pushBaseline.test.ts` (baseline path assert).

**Temp workspace harness** (`cli/tests/applyPull.test.ts` lines 14–43) — keep; optionally add `pullZip(bytes)` like preview harness if duplicate rejects need raw zip:
```typescript
  const pull = (notes: Record<string, string>, path = workspace) =>
    applyPull({
      notebookId: 1,
      workspacePath: path,
      exportNotebookAsZip: () =>
        Promise.resolve({
          bytes: zipOfNotes(notes),
          fileName: 'Ben Notebook.zip',
        }),
    })
```

Mirror `cli/tests/previewPullHarness.ts` lines 46–55 for zip-bytes cases:
```typescript
  const previewZip = (bytes: Buffer, path = workspace) =>
    previewPull({
      notebookId: 1,
      workspacePath: path,
      exportNotebookAsZip: () =>
        Promise.resolve({
          bytes,
          fileName: 'Ben Notebook.zip',
        }),
    })
```

**Invert anti-create** (current lines 54–61 → assert create):
```typescript
  test('does not create a file for a remote-only note', async () => {
    write('less.md', 'Hello')

    await expect(
      pull({ 'less.md': 'Hello', 'scrum.md': 'Sprint' })
    ).resolves.toBe(NOTHING_TO_PULL)
    expect(() => readBack('scrum.md')).toThrow()
  })
```
Replace with capability-named create proof: summary ≠ `NOTHING_TO_PULL`, `readBack('scrum.md')` equals remote content; keep local-only / update / no-op / perf tests green.

**Move fixture** (`cli/tests/previewPullDiagnostics.test.ts` lines 8–20) — copy content shape into apply tests; assert new path written, old path gone, local-only preserved:
```typescript
  test('reports a move when the same doughnut_id is at a different path', async () => {
    ws.write('less.md', '---\ndoughnut_id: 42\n---\n\n# less\n\nHello')

    const report = await ws.preview({
      'scrum.md': '---\ndoughnut_id: 42\n---\n\n# scrum\n\nHello',
    })

    expect(report).toContain('scrum.md (move)')
```

**Reject fixtures** — copy reserved / duplicate / unsafe / rejects-only cases from `previewPullDiagnostics.test.ts` lines 32–98; assert **no write** for reject paths and summary contains `(reject)` / reason.

**Baseline assert** (`cli/tests/pushBaseline.test.ts` lines 55–66):
```typescript
  test('writes under a hidden .doughnut-sync directory', () => {
    savePushBaseline(workspace, 1, new Map([['less.md', 'Hello world!']]))

    const raw = readFileSync(
      join(workspace, '.doughnut-sync', 'baseline.json'),
      'utf8'
    )
    expect(JSON.parse(raw)).toEqual({
      notebookId: 1,
      notes: { 'less.md': 'Hello world!' },
    })
  })
```
After mutate: `baseline.json` exists with touched paths; after no-op / rejects-only: no new baseline / mtime untouched (no `.doughnut-sync` created when none existed).

---

### `e2e_test/features/cli/cli_sync_pull.feature` (test, request-response)

**Analog:** same feature (update / local-only / no-op / `@perfSync`) + `cli_push_dry_run.feature` for baseline path listing.

**Invert anti-create scenario** (lines 42–53):
```gherkin
  Scenario: No new local file for a remote-only note
    ...
    Then the workspace "./BenNotebook" should not contain "scrum.md"
```
Replace with create proof: after pull, `scrum.md` holds remote content; assistant message not `No changes to pull.`; keep `less.md` undisturbed.

**Baseline presence pattern** (`e2e_test/features/cli/cli_push_dry_run.feature` lines 150–155) — reuse step vocabulary:
```gherkin
    Scenario: The preview's only addition is its own baseline file
      When I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI
      Then the workspace "./BenNotebook" should hold only:
        | Path                          |
        | less.md                       |
        | .doughnut-sync/baseline.json  |
```
For pull: after mutating pull, workspace may include `.doughnut-sync/baseline.json`; after no-op on zip-seeded workspace without baseline, do **not** introduce irrelevant baseline churn (D-06). Prefer existing `should hold only` / `should not contain` / `file … should hold` from `cli_workspace.ts` over new step defs unless content of `baseline.json` must be asserted (then thin cy.task — RESEARCH Wave 0).

**Keep green** (do not regress): update scenario (lines 16–25), local-only (27–40), no-op (55–63), `@perfSync` (65–72).

**E2E move:** optional (Pitfall 8) — prefer unit move proof; no new title-rename step required this phase.

---

### `e2e_test/step_definitions/cli_sync_pull.ts` (optional; test, request-response)

**Analog:** same file — keep thin glue. New baseline assertions should live in `cli_workspace.ts` / page objects if reusable, not fat step defs.

Current pattern (lines 26–28):
```typescript
When('I pull into the workspace {string}', (workspaceName: string) => {
  syncPull.pullIntoWorkspace(workspaceName)
})
```

Reuse from `e2e_test/step_definitions/cli_workspace.ts` lines 76–95:
```typescript
Then(
  'the file {string} in the workspace {string} should hold {string}',
  ...
)
Then(
  'the workspace {string} should not contain {string}',
  ...
)
Then(
  'the workspace {string} should hold only:',
  ...
)
```

## Shared Patterns

### Classify-then-apply (preview/apply alignment)
**Source:** `cli/src/sync/previewPull.ts` + `cli/src/sync/previewPullActions.ts`  
**Apply to:** `applyPull.ts` only (import classify; never edit Terry-authored classifier — HYG-02)
```typescript
classifyPreviewPullNotes(workspace, exported, listZipFileNames(bytes))
```
Always pass `listZipFileNames` (Pitfall 6 — Map collapse hides duplicates).

### Filesystem write + mkdir
**Source:** `cli/src/sync/applyPull.ts` / `writeNotebookExport.ts`  
**Apply to:** create, update, move destination paths
```typescript
mkdirSync(dirname(full), { recursive: true })
writeFileSync(full, content, 'utf8')
```

### Baseline load/save
**Source:** `cli/src/sync/pushBaseline.ts`  
**Apply to:** applyPull after successful mutations only (D-05/D-06)  
Do **not** copy previewPush’s unconditional `savePushBaseline` (lines 118–122).

### Reject rendering
**Source:** `cli/src/sync/diffReport.ts` `renderRejectFinding`  
**Apply to:** apply summary when rejects present (D-03); optional reuse of `renderDiffReport` count style for “N rejects.”

### Vitest observable behavior
**Source:** `cli/tests/applyPull.test.ts`, `cli.mdc`  
**Apply to:** all new units — assert return string + disk state; no fixed-time waits; capability-named tests (no phase numbers).

### CLI E2E thin steps
**Source:** `e2e_test/step_definitions/cli_sync_pull.ts` + `cli_workspace.ts`  
**Apply to:** feature scenarios — page-object assertions; targeted `cypress run --spec e2e_test/features/cli/cli_sync_pull.feature`.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All Phase 10 surfaces have in-repo analogs; baseline-on-pull is gated reuse of export/push baseline APIs, not a new subsystem. |

## Metadata

**Analog search scope:** `cli/src/sync/`, `cli/src/commands/notebook/`, `cli/tests/`, `e2e_test/features/cli/`, `e2e_test/step_definitions/`  
**Files scanned:** ~20 (apply/preview/export/push baseline modules, related Vitest + E2E)  
**Pattern extraction date:** 2026-08-03  
**HYG-02 gate:** `git diff` must not list `previewPullActions.ts` (Terry Yin Phase 9).
