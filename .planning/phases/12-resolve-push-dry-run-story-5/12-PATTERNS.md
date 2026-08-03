# Phase 12: Resolve push dry-run (story 5) - Pattern Map

**Mapped:** 2026-08-03
**Files analyzed:** 5 (primary modify) + 4 (import/keep / optional touch)
**Analogs found:** 5 / 5

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `cli/src/sync/previewPush.ts` | service | transform + request-response | `cli/src/sync/previewPull.ts` | exact (dry-run report; load-only sibling) |
| `cli/src/sync/diffReport.ts` | utility | transform | self (`diffReport.ts`) — prefer no API change | exact (shared renderer) |
| `cli/tests/previewPush.test.ts` | test | request-response | `cli/tests/previewPull.test.ts` + existing export-primed unit in same file | exact |
| `e2e_test/features/cli/cli_push_dry_run.feature` | test | request-response | `e2e_test/features/cli/cli_sync_dry_run.feature` (non-mutation) + own Export Rule | exact |
| `cli/src/commands/notebook/pushSlashCommand.tsx` | component | request-response | self — touch only if help/doc required (D-06) | exact |

**Import-only / keep APIs (do not rewrite):**

| File | Role | How Phase 12 uses it |
|------|------|----------------------|
| `cli/src/sync/previewPullActions.ts` | service | Import `classifyCreateOrUpdate` only (HYG-02 — no edits) |
| `cli/src/sync/pushBaseline.ts` | utility | `loadPushBaseline` from dry-run; `savePushBaseline` stays for export/pull |
| `cli/src/sync/writeNotebookExport.ts` | service | Unit/E2E priming via export baseline seed |
| `cli/src/sync/applyPull.ts` | service | Contrast: mutating path that **may** `savePushBaseline` — do not regress |

## Pattern Assignments

### `cli/src/sync/previewPush.ts` (service, transform + request-response)

**Analog:** `cli/src/sync/previewPull.ts` (non-mutating dry-run orchestration) + keep local `classify` / directional labels

**Target shape after strengthen:** Match pull’s “read only, return report string” contract; keep push’s merge-base `classify` + `(push)`/`(pull)`/`(CONFLICT)`; expand path union; stop writing baseline.

**Imports pattern** — pull dry-run (lines 1–12 of `previewPull.ts`):
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

**Push should import (after D-02):** drop `savePushBaseline`; keep `loadPushBaseline`; add import-only:
```typescript
import { classifyCreateOrUpdate } from './previewPullActions.js'
import { loadPushBaseline } from './pushBaseline.js'
```

**Core non-mutating dry-run pattern** (`previewPull.ts` lines 50–76) — copy this contract (no writes):
```typescript
/**
 * Report what pulling the notebook would change in the workspace.
 * ...
 * The workspace is only ever read.
 */
export async function previewPull({...}): Promise<string> {
  const workspace = readWorkspace(workspacePath)
  const { bytes } = await exportNotebookAsZip(notebookId, signal)
  // classify → render → return — no savePushBaseline
  return renderDiffReport(reported, NOTHING_TO_PULL)
}
```

**Anti-pattern to remove** (`previewPush.ts` lines 118–122):
```typescript
savePushBaseline(
  workspacePath,
  notebookId,
  nextBaseline(baseline, workspace, markdownExported)
)
```
Also delete `nextBaseline` helper (lines 60–72) once unused. Update docstring lines 90–91 (“the only write is the updated baseline”) to match pull: workspace + Doughnut + sync metadata are never written.

**Keep directional classify** (`previewPush.ts` lines 30–42) — do not replace with pull’s path-keyed-only engine:
```typescript
function classify(
  baseline: string | undefined,
  local: string,
  remote: string
): NoteOutcome {
  if (baseline !== undefined) {
    const remoteChanged = remote !== baseline
    const localChanged = local !== baseline
    if (remoteChanged !== localChanged) return remoteChanged ? 'pull' : 'push'
    if (remoteChanged && local !== remote) return 'conflict'
  }
  return local === remote ? 'nothing' : 'difference'
}
```

**Intersecting-only loop to replace** (`previewPush.ts` lines 109–116):
```typescript
const reported = markdownExported.flatMap(([path, remote]) => {
  const local = workspace.get(path)
  if (local === undefined) return []
  // ...
})
```
**Replace with:** union of exported Markdown paths ∪ workspace Markdown paths; exclude reserved `index.md` / `log.md` and `.doughnut-sync` segments (mirror vocabulary from `previewPullActions.ts` locally — do not edit Terry file).

**Create/update taxonomy** — import-only (`previewPullActions.ts` lines 51–58):
```typescript
export function classifyCreateOrUpdate(
  workspaceContent: string | undefined,
  exportContent: string
): 'create' | 'update' | 'unchanged' {
  if (workspaceContent === undefined) return 'create'
  if (workspaceContent === exportContent) return 'unchanged'
  return 'update'
}
```
For push dry-run, adapt semantics carefully:
- Local-only (no remote) → create (push orientation)
- Remote-only (no local) → create (pull orientation)
- Both present + outcome `conflict` → `(CONFLICT)` only — never `(update)` (D-04)
- Both present + directional push/pull → keep `(push)`/`(pull)`; treat as update signal (discretion: literal `(update)` optional)

**Render create with empty side** — pull pattern via `renderNoteDiff(..., undefined, action)` (`previewPull.ts` lines 33–39):
```typescript
const diff = renderNoteDiff(
  note.path,
  note.workspaceContent,
  note.exportContent,
  undefined,
  note.action  // 'create' | 'update' | ...
)
```
**Discretion (A1):** Prefer composing headings in `previewPush` so Story 2 pull `action` XOR `status` in `diffReport` stays untouched. For creates, mirror pull’s `path (create)` expectation.

**Reserved path filter vocabulary** (`previewPullActions.ts` lines 10–11, 81–87) — copy constants locally if needed:
```typescript
const RESERVED_BASENAMES = new Set(['index.md', 'log.md'])
const SYNC_METADATA_SEGMENT = '.doughnut-sync'
```

---

### `cli/src/sync/diffReport.ts` (utility, transform)

**Analog:** self — shared by `/sync --dry-run` and `/push --dry-run`

**Heading XOR pattern** (lines 51–57) — do not break pull:
```typescript
const paren =
  action !== undefined
    ? action
    : status !== undefined
      ? labelOf(status)
      : undefined
const heading = paren === undefined ? path : `${path} (${paren})`
```

**Recommendation:** Prefer **no API change**. If push needs both direction and create on one line, compose the heading string in `previewPush` (or pass a pre-built heading) rather than changing the XOR. If extending, add a regression unit that pull create still yields `scrum.md (create)`.

**Status union** (line 5) — keep narrow; do not widen with create/update:
```typescript
export type NoteDiffStatus = 'pull' | 'push' | 'conflict'
```

**Push vs pull diff orientation** (lines 39–42) — keep:
```typescript
const [before, after] =
  status === 'push'
    ? [doughnutSide, workspaceSide]
    : [workspaceSide, doughnutSide]
```
Local-only create (push) should use `status: 'push'` with empty remote content so orientation matches existing `(push)` diffs.

---

### `cli/tests/previewPush.test.ts` (test, request-response)

**Analogs:** `cli/tests/previewPull.test.ts` (create + non-write) + existing export-primed unit in this file

**Fixture harness** (keep — lines 15–44):
```typescript
describe('previewPush', () => {
  let workspace: string
  beforeEach(() => {
    workspace = mkdtempSync(join(tmpdir(), 'doughnut-previewPush-'))
  })
  const preview = (notes: Record<string, string>, path = workspace) =>
    previewPush({
      notebookId: 1,
      workspacePath: path,
      exportNotebookAsZip: () =>
        Promise.resolve({
          bytes: zipOfNotes(notes),
          fileName: 'Ben Notebook.zip',
        }),
    })
```

**Flip: dry-run must not seed baseline** — replace tests at lines 150–161, 323–337, 339–348. New contract:
```typescript
test('does not write sync metadata', async () => {
  write('less.md', 'Hello')
  await preview({ 'less.md': 'Hello world!' })
  expect(existsSync(join(workspace, '.doughnut-sync'))).toBe(false)
})
```

**Re-prime directional units** — stop using `await preview({ 'less.md': 'Hello' })` as seed. Use either:
1. Export priming (already in file, lines 475–508):
```typescript
await writeNotebookExport({
  notebookId: 1,
  destinationDirectory: workspace,
  exportNotebookAsZip,
})
```
2. Or explicit `savePushBaseline` (RESEARCH unit priming):
```typescript
import { savePushBaseline } from '../src/sync/pushBaseline.js'
savePushBaseline(workspace, 1, new Map([['less.md', 'Hello']]))
```

**Flip remote-only** — replace lines 351–357 (`leaves a note missing from the workspace out of the report`) with remote-only **create** expectation (mirror pull create shape from `previewPull.test.ts` lines 143–157):
```typescript
// Expected pull create shape to mirror for remote-only push preview:
'scrum.md (create)',
'  --- workspace',
'  +++ Doughnut',
'  + Sprint plan',
```

**Create unit shape from pull** (`previewPull.test.ts` lines 143–157):
```typescript
test('reports a note the pull would create with an explicit create label', async () => {
  ws.write('less.md', 'Hello')
  await expect(
    ws.preview({ 'less.md': 'Hello', 'scrum.md': 'Sprint plan' })
  ).resolves.toBe(
    [
      'scrum.md (create)',
      '  --- workspace',
      '  +++ Doughnut',
      '  + Sprint plan',
      '',
      '1 note would change.',
    ].join('\n')
  )
})
```

**Keep:** conflict counting, unlabeled first preview, `.md` non-write (`does not write to the workspace`), export-primed `(push)` unit.

---

### `e2e_test/features/cli/cli_push_dry_run.feature` (test, request-response)

**Analogs:**
1. Own Rule *Exporting primes the baseline…* (lines 110–134) — re-prime directional Background
2. `cli_sync_dry_run.feature` Rule *The preview leaves nothing behind* (lines 98–117) — non-mutation inventory

**Invert Feature blurb** (lines 10–12 today claim baseline write). Target: match pull’s “never writes” voice — no sync-metadata exception.

**Invert baseline scenario** — replace lines 150–155:
```gherkin
Scenario: The preview's only addition is its own baseline file
  ...
  Then the workspace "./BenNotebook" should hold only:
    | Path                          |
    | less.md                       |
    | .doughnut-sync/baseline.json  |
```
**With** pull’s “adds no files” pattern (`cli_sync_dry_run.feature` lines 112–117):
```gherkin
Scenario: The preview adds no files of its own
  When ...
  Then the workspace "./BenNotebook" should hold only:
    | Path    |
    | less.md |
```

**Re-prime directional Rule Background** — replace dry-run priming (lines 50–56):
```gherkin
And I enter the slash command "/push --dry-run ./BenNotebook" in the interactive CLI
```
**With** export path already proven in same Feature (lines 112–119):
```gherkin
And an empty export destination "./ExportTarget"
And I export the notebook into "./ExportTarget"
And the workspace "./BenNotebookExport" is the notebook "Ben Notebook" exported into "./ExportTarget"
```
(Adapt path/workspace names to keep Scenario steps coherent; or use successful mutate pull if preferred — D-03.)

**Keep:** first-preview unlabeled diffs on zip-seeded workspaces; conflict `(CONFLICT)`; `.md` / Doughnut non-mutation scenarios.

**Add:** capability-named create scenarios (local-only and/or remote-only) — capability names only, no phase numbers.

---

### `cli/src/commands/notebook/pushSlashCommand.tsx` (component, request-response)

**Analog:** self — D-06 says touch only for help/doc if required

**Keep dry-run-only wiring** (lines 15–20, 34–40):
```typescript
const pushDoc: CommandDoc = {
  name: '/push',
  usage: '/push --dry-run <workspace path>',
  description:
    'Preview what pushing the workspace would change in Doughnut. Only --dry-run is supported so far.',
}
const runPreviewPush = useCallback(
  (signal: AbortSignal) =>
    previewPush({
      notebookId,
      workspacePath,
      exportNotebookAsZip: downloadNotebookExportZip,
      signal,
```
Do **not** relax `parsePushArgument`; do **not** implement mutate push.

---

## Shared Patterns

### Load-only baseline on dry-run
**Source:** `cli/src/sync/previewPull.ts` (no baseline I/O) + `loadPushBaseline` from `pushBaseline.ts`
**Apply to:** `previewPush.ts` only
```typescript
// pushBaseline.ts lines 25–36 — READ ok from dry-run
export function loadPushBaseline(
  workspacePath: string,
  notebookId: number
): ReadonlyMap<string, string> { /* ... */ }

// savePushBaseline — export + applyPull only (writeNotebookExport.ts:88-92, applyPull.ts:78-80)
```

### Baseline writers (do not call from previewPush)
**Source:** `writeNotebookExport.ts` lines 88–92; `applyPull.ts` lines 78–80
```typescript
// Export seed (preferred E2E/unit priming — D-03)
savePushBaseline(
  root,
  notebookId,
  new Map(entries.filter(([path]) => path.endsWith(MARKDOWN_SUFFIX)))
)

// Successful mutate pull
if (mutated > 0) {
  savePushBaseline(workspacePath, notebookId, nextBaseline)
}
```

### Create/update classification
**Source:** `cli/src/sync/previewPullActions.ts` lines 51–58
**Apply to:** `previewPush.ts` path-union reporting
**Constraint:** HYG-02 — import only; never edit `previewPullActions.ts`

### Report rendering
**Source:** `cli/src/sync/diffReport.ts` — `renderNoteDiff` + `renderDiffReport`
**Apply to:** all reported notes; empty-report constant `No changes to push.` (`previewPush.ts` line 11)

### Conflict never as update
**Source:** existing `classify` → `'conflict'` + `labelOf` → `CONFLICT`
**Apply to:** any create/update overlay — if outcome is conflict, skip `(update)`

### Reserved / sync-metadata exclusion
**Source:** `previewPullActions.ts` `RESERVED_BASENAMES` / `SYNC_METADATA_SEGMENT`
**Apply to:** ordinary create/update rows in push preview (local filter OK)

### Unit/E2E priming after D-02
**Source:** `previewPush.test.ts` export-primed unit; Feature Export Rule; `cli_sync_dry_run.feature` inventory
**Apply to:** all directional proofs — never prime via dry-run

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | None — all Phase 12 targets have in-repo analogs |

## Metadata

**Analog search scope:** `cli/src/sync/`, `cli/tests/`, `e2e_test/features/cli/`, `cli/src/commands/notebook/`
**Files scanned:** ~12 primary (previewPush, previewPull, previewPullActions, diffReport, pushBaseline, writeNotebookExport, applyPull, pushSlashCommand, previewPush.test, previewPull.test, cli_push_dry_run.feature, cli_sync_dry_run.feature)
**Pattern extraction date:** 2026-08-03

### Key Patterns for Planner

1. **Dry-run = pull’s non-mutation bar** — delete `savePushBaseline`/`nextBaseline` from `previewPush`; docstring must match.
2. **Import `classifyCreateOrUpdate`; expand path union** — do not fork Terry’s reject/move engine.
3. **Compose headings in `previewPush`** — avoid breaking `diffReport` action/status XOR used by Story 2.
4. **Re-prime with export/`savePushBaseline`** — flip units + E2E Background that used priming dry-run.
5. **Invert E2E inventory** — copy `cli_sync_dry_run` “adds no files of its own” (`less.md` only).
