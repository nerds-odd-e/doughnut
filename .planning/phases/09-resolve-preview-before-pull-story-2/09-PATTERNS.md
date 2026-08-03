# Phase 9: Resolve preview-before-pull (story 2) - Pattern Map

**Mapped:** 2026-08-03
**Files analyzed:** 10
**Analogs found:** 10 / 10

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `cli/src/sync/previewPull.ts` | service | transform / request-response | `cli/src/sync/previewPush.ts` (classify + report) + current `previewPull.ts` | exact |
| `cli/src/sync/diffReport.ts` | utility | transform | `cli/src/sync/diffReport.ts` (self) + push status labeling via `previewPush.ts` | exact |
| `cli/src/sync/previewPullActions.ts` (optional helper) | utility | transform | `cli/src/sync/previewPush.ts` `classify` + `writeNotebookExport.ts` `assertSafeEntryPath` | role-match |
| `cli/src/sync/doughnutId.ts` (optional helper) | utility | transform | Backend `ExportNoteMarkdown.DOUGHNUT_ID_LINE` + `cli/src/lint/okfConcept.ts` frontmatter fence | role-match |
| `cli/src/sync/unzip.ts` (optional listEntries; prefer preview-local) | utility | file-I/O / transform | `cli/src/sync/unzip.ts` central-directory loop | exact |
| `cli/src/commands/notebook/syncSlashCommand.tsx` | component / route | request-response | existing file (touch only if report wiring needs it) | exact |
| `cli/src/sync/syncArgument.ts` | utility | transform | existing file (unlikely to change) | exact |
| `cli/tests/previewPull.test.ts` | test | request-response | existing `previewPull.test.ts` + `zipFixture.buildZip` | exact |
| `e2e_test/features/cli/cli_sync_dry_run.feature` | test | request-response | existing feature + Rule `The preview leaves nothing behind` | exact |
| `cli/tests/previewPush.test.ts` | test | request-response | regression gate if `diffReport` shared surface changes | role-match |

**Frozen / read-only references (do not modify in this phase):**

| File | Role | Why referenced |
|------|------|----------------|
| `cli/src/sync/applyPull.ts` | service | D-07 freeze — Story 3 / Phase 10 |
| `cli/src/lint/lintWorkspace.ts` | service | Reserved basename vocabulary (`index.md`, `log.md`) |
| `cli/src/lint/bundleFiles.ts` | utility | `isHidden` / `.doughnut-sync` segment convention |
| `cli/src/sync/writeNotebookExport.ts` | service | Unsafe path criteria to mirror as **rejects** |
| `cli/src/sync/unifiedDiff.ts` | utility | Keep for content hunks |
| `cli/src/sync/readWorkspace.ts` | utility | Workspace map input |
| `cli/tests/zipFixture.ts` | test util | `buildZip` for duplicate-path fixtures |

## Pattern Assignments

### `cli/src/sync/previewPull.ts` (service, transform / request-response)

**Analog:** `cli/src/sync/previewPull.ts` (orchestration shell) + `cli/src/sync/previewPush.ts` (classify-then-report)

**Imports pattern** (current `previewPull.ts` lines 1-4):
```typescript
import { renderDiffReport, renderNoteDiff } from './diffReport.js'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import { readWorkspace } from './readWorkspace.js'
import { unzipToEntries } from './unzip.js'
```

**Core orchestration pattern** (lines 25-43) — keep injectables + non-write flow; replace only the `changed` classify body:
```typescript
export async function previewPull({
  notebookId,
  workspacePath,
  exportNotebookAsZip,
  signal,
}: PreviewPullRequest): Promise<string> {
  const workspace = readWorkspace(workspacePath)
  const { bytes } = await exportNotebookAsZip(notebookId, signal)
  const exported = unzipToEntries(bytes)

  const changed = [...exported]
    .filter(([path]) => path.endsWith(MARKDOWN_SUFFIX))
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .filter(([path, content]) => workspace.get(path) !== content)
    .map(([path, content]) => ({
      diff: renderNoteDiff(path, workspace.get(path) ?? '', content),
    }))

  return renderDiffReport(changed, NOTHING_TO_PULL)
}
```

**Classify-then-report analog** from `previewPush.ts` (lines 30-42, 109-116) — local outcome type, omit unchanged, pass status into report:
```typescript
function classify(
  baseline: string | undefined,
  local: string,
  remote: string
): NoteOutcome {
  // ... direction from baseline
  return local === remote ? 'nothing' : 'difference'
}

const reported = markdownExported.flatMap(([path, remote]) => {
  const local = workspace.get(path)
  if (local === undefined) return []
  const outcome = classify(baseline.get(path), local, remote)
  if (outcome === 'nothing') return []
  const status = outcome === 'difference' ? undefined : outcome
  return [{ status, diff: renderNoteDiff(path, local, remote, status) }]
})
```

**Non-mutation contract** (`previewPull.ts` lines 17-23, tested lines 211-217): workspace is only read; unit asserts `readBack` unchanged after preview. Preserve this when extending classify.

**Nothing-changed sentinel** (line 6): `const NOTHING_TO_PULL = 'No changes to pull.'` — D-05: rejects-only must **not** return only this string.

**Target shape (from RESEARCH — do not jam into `NoteDiffStatus`):**
```typescript
type PreviewPullAction = 'create' | 'update' | 'move' | 'reject'
// move only when same doughnut_id at different paths (D-03, D-08)
```

---

### `cli/src/sync/diffReport.ts` (utility, transform)

**Analog:** self — extend carefully; shared with `previewPush`

**Status union to preserve** (lines 3-4):
```typescript
export type NoteDiffStatus = 'pull' | 'push' | 'conflict'
```

**Heading + labeled report pattern** (lines 27-54):
```typescript
export function renderNoteDiff(
  path: string,
  workspaceContent: string,
  notebookContent: string,
  status?: NoteDiffStatus
): string {
  // ...
  const heading = status === undefined ? path : `${path} (${labelOf(status)})`
  return [
    heading,
    `  --- ${before.name}`,
    `  +++ ${after.name}`,
    ...body,
    '',
  ].join('\n')
}
```

**Summary / empty report** (lines 63-85):
```typescript
const counted = (count: number, noun: string) =>
  `${count} ${noun}${count === 1 ? '' : 's'}`

export function renderDiffReport(
  reported: readonly ReportedNoteDiff[],
  nothingChanged: string
): string {
  if (reported.length === 0) return nothingChanged
  const conflicts = reported.filter(
    ({ status }) => status === 'conflict'
  ).length
  const changes = reported.length - conflicts
  const summary = [
    ...(changes === 0 ? [] : [`${counted(changes, 'note')} would change.`]),
    ...(conflicts === 0 ? [] : [`${counted(conflicts, 'conflict')}.`]),
  ].join(' ')
  return [...reported.map(({ diff }) => diff), summary].join('\n')
}
```

**Planner guidance:** Prefer a **parallel** pull-action type / render path (or optional pull-specific fields) so `(push)` / `(CONFLICT)` for `/push --dry-run` stay intact. Do not overload `NoteDiffStatus` with create/update/move/reject. After shared changes, run `cli/tests/previewPush.test.ts`.

**Reject-row analog** (path + short reason, no unified hunk required) — `cli/src/lint/lintReport.ts` lines 31-39:
```typescript
export function lintReport(findings: readonly Finding[]): string {
  if (findings.length === 0) return CONFORMS
  return [
    ...findings.map(
      (finding) => `${at(finding)}  ${finding.severity}  ${finding.message}`
    ),
    '',
    summary(findings),
  ].join('\n')
}
```
Pull rejects can be first-class rows (path + action/reason) assembled beside or instead of hunks for reject-only findings (discretion on exact wording).

---

### `cli/src/sync/previewPullActions.ts` (optional helper, transform)

**Analog:** `previewPush.ts` classify + `writeNotebookExport.ts` safety + `lintWorkspace.ts` reserved names

**Unsafe path criteria to mirror as non-throwing rejects** (`writeNotebookExport.ts` lines 24-31):
```typescript
function assertSafeEntryPath(path: string): void {
  if (
    path.startsWith('/') ||
    path.includes('\\') ||
    path.split('/').includes('..')
  ) {
    throw new Error(`The export contained an unsafe path: ${path}.`)
  }
}
```
For preview: same checks **plus empty segments** (`''` in `split('/')`); return reject reason instead of throwing so dry-run still yields a string report (D-04/D-05). Unreadable zip (`NOT_A_ZIP`) still throws via `unzip`.

**Reserved vocabulary** (`lintWorkspace.ts` lines 14-22):
```typescript
function problemsIn(path: string, content: string): OkfProblem[] {
  const name = basename(path)
  if (name === 'index.md') return indexProblems(content, path === name)
  if (name === 'log.md') return logProblems(content)
  return conceptProblems(content)
}
```
Preview: basename `index.md` / `log.md` → **reject** (prefer reject over update when both apply). Paths under `.doughnut-sync/` → reject “sync metadata, never a pull target”.

**Hidden / sync-metadata segment convention** (`bundleFiles.ts` lines 11-13):
```typescript
export function isHidden(path: string): boolean {
  return path.split(posix.sep).some((segment) => segment.startsWith('.'))
}
```
Use for `.doughnut-sync/**` awareness; do not implement Story 4 lint here.

**Classify sketch order** (RESEARCH / CONTEXT):
1. Collect export `.md` entries with duplicate / case-clash detection **before** Map collapse
2. Unsafe / `.doughnut-sync/**` / reserved basename → reject
3. Build id→path maps from `doughnut_id` on workspace + export
4. Same id, different path → move (preview only)
5. Else missing workspace path → create; else content differs → update
6. Omit unchanged; summary by action; rejects-only ≠ `NOTHING_TO_PULL`

---

### `cli/src/sync/doughnutId.ts` (optional helper, transform)

**Analog:** Backend export contract + optional YAML fence parse in lint

**Export contract** (`ExportNoteMarkdown.java` lines 21, 44-47):
```java
private static final Pattern DOUGHNUT_ID_LINE = Pattern.compile("(?i)^doughnut_id\\s*:.*");
// emitted shape:
// ---\ndoughnut_id: {noteId}\n---
```

**Frontmatter fence pattern** if full YAML needed (`okfConcept.ts` lines 1-5, 29-30):
```typescript
import { isMap, isScalar, parseDocument } from 'yaml'

const OPENING = '---\n'
const CLOSING = '\n---'

function frontmatter(block: string): Frontmatter | undefined {
  const doc = parseDocument(block)
```
**Preference (RESEARCH):** lightweight line extract aligned with `DOUGHNUT_ID_LINE`; missing id → path-keyed create/update only (D-08). Do **not** add a new parser package (`yaml` already in CLI).

---

### `cli/src/sync/unzip.ts` (optional listEntries; prefer preview-local)

**Analog:** self — central-directory iteration

**Duplicate collapse pitfall** (lines 50-68):
```typescript
const entries = new Map<string, string>()
for (let read = 0; read < count; read++) {
  // ...
  const name = zip.subarray(at + 46, at + 46 + nameLength).toString('utf8')
  if (!name.endsWith('/')) {
    entries.set(
      name,
      contentAt(zip, localHeaderOffset, method, compressedSize).toString('utf8')
    )
  }
}
```
**Do not** silently change `Map` semantics for `applyPull` / export. Prefer preview-local scan or a new shared `listZipEntryNames` that preserves duplicates **before** collapsing. Unit fixtures: `buildZip` with two same-name entries (`zipFixture.ts` lines 21-68), not `zipOfNotes` Record.

**NOT_A_ZIP** (line 10): keep throwing for unreadable zip.

---

### `cli/src/commands/notebook/syncSlashCommand.tsx` (component, request-response)

**Analog:** self — touch only if needed to surface new report shape

**Dry-run branch** (lines 37-45):
```typescript
const runSync = useCallback(
  (signal: AbortSignal) => {
    const request = {
      notebookId,
      workspacePath,
      exportNotebookAsZip: downloadNotebookExportZip,
      signal,
    }
    return dryRun ? previewPull(request) : applyPull(request)
  },
  [notebookId, workspacePath, dryRun]
)
```
Report string from `previewPull` already flows through `AsyncAssistantFetchStage` → past assistant messages. Prefer **no** command changes unless spinner/copy must mention rejects.

---

### `cli/src/sync/syncArgument.ts` (utility, transform)

**Analog:** self — `--dry-run` parsing already sufficient (D-07 “only if needed”)

```typescript
export function parseSyncArgument(argument: string | undefined): SyncArgument {
  // [--dry-run] <workspace path>
}
```
Expect **no change** for Story 2 taxonomy.

---

### `cli/tests/previewPull.test.ts` (test, request-response)

**Analog:** self — extend with taxonomy / diagnostics Wave 0 cases

**Fixture harness** (lines 14-43):
```typescript
describe('previewPull', () => {
  // mkdtemp workspace, write/readBack helpers
  const preview = (notes: Record<string, string>, path = workspace) =>
    previewPull({
      notebookId: 1,
      workspacePath: path,
      exportNotebookAsZip: () =>
        Promise.resolve({
          bytes: zipOfNotes(notes),
          fileName: 'Ben Notebook.zip',
        }),
    })
```

**Observable string assertions** (e.g. lines 45-58) — update expected headings when action labels land:
```typescript
await expect(preview({ 'less.md': 'Hello world!' })).resolves.toBe(
  [
    'less.md',
    '  --- workspace',
    '  +++ Doughnut',
    '  - Hello',
    '  + Hello world!',
    '',
    '1 note would change.',
  ].join('\n')
)
```

**Create-as-all-added baseline** (lines 178-193) — today unlabeled “would change”; strengthen to **create** label while keeping hunk behavior.

**Non-write proof** (lines 211-217): keep and extend for new scenarios.

**Duplicate fixtures:** switch export mock to `buildZip([{ name, content }, { name, content }])` from `cli/tests/zipFixture.ts` when testing duplicate rejects.

**Regression:** if `diffReport` changes, also run `cli/tests/previewPush.test.ts` (same harness pattern, lines 15-44).

---

### `e2e_test/features/cli/cli_sync_dry_run.feature` (test, request-response)

**Analog:** self — extend Rules; preserve non-mutation Rule

**Integration surface** (lines 39-56): assert path + preview body + “N note(s) would change” via past CLI assistant messages.

**Non-mutation Rule** (lines 83-102):
```gherkin
Rule: The preview leaves nothing behind
  Scenario: The workspace is not written to
  Scenario: The preview adds no files of its own
```
Extend this Rule if new scenarios touch the filesystem (D-06). Add scenarios for at least one action label beyond content overwrite + one reserved/duplicate/invalid finding. Capability-named only — no phase numbers (D-09).

---

### Frozen: `cli/src/sync/applyPull.ts` (do not modify)

**Reference only** — path-keyed updates, remote-only notes not created (lines 23-50):
```typescript
for (const [path, localContent] of workspace) {
  if (!path.endsWith(MARKDOWN_SUFFIX)) continue
  const remote = exported.get(path)
  if (remote === undefined || remote === localContent) continue
  // writeFileSync ...
}
```
Phase 10 owns create/rename/move application. Phase 9 commits must not touch this file or `cli_sync_pull.feature` / `applyPull.test.ts` (Pitfall 5).

## Shared Patterns

### Preview-only classify; apply frozen
**Source:** `previewPull.ts` + `applyPull.ts` + CONTEXT D-03/D-07  
**Apply to:** All Story 2 strengthen work  
Dry-run path owns taxonomy + diagnostics. Real `/sync` without `--dry-run` stays today’s `applyPull`.

### Separate pull actions from push `NoteDiffStatus`
**Source:** `diffReport.ts` lines 3-4; `previewPush.ts` status wiring  
**Apply to:** `diffReport.ts`, `previewPull.ts`  
Keep `NoteDiffStatus = 'pull' | 'push' | 'conflict'`. Add `PreviewPullAction` (or equivalent) in parallel.

### Non-mutation
**Source:** `previewPull.ts` docs + tests; E2E Rule `The preview leaves nothing behind`  
**Apply to:** preview + E2E  
No `writeFile`, no baseline/sync-metadata writes, no Doughnut mutations. Contrast: `previewPush` **does** write baseline — do not copy that write into pull preview.

### Injectable export for units
**Source:** `PreviewPullRequest.exportNotebookAsZip` + `zipOfNotes` / `buildZip`  
**Apply to:** all `previewPull` unit cases  
Mock zip bytes; never hit HTTP in unit taxonomy tests.

### Unsafe paths → reject (preview) vs throw (export write)
**Source:** `writeNotebookExport.assertSafeEntryPath`  
**Apply to:** invalid-mapping diagnostics  
Same criteria; preview reports; export write still throws.

### Reserved names align with lint
**Source:** `lintWorkspace.ts` `index.md` / `log.md`  
**Apply to:** reject classification  
Read-only vocabulary alignment; do not implement Story 4.

### Verification commands
```bash
CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/previewPull.test.ts
# if diffReport shared surface changes:
CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/previewPush.test.ts
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_sync_dry_run.feature
```

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | All planned surfaces have in-repo analogs. Optional helpers are new files but copy classify / safety / frontmatter patterns above. |

## Metadata

**Analog search scope:** `cli/src/sync/`, `cli/src/lint/`, `cli/src/commands/notebook/`, `cli/tests/`, `e2e_test/features/cli/`, backend `ExportNoteMarkdown.java`  
**Files scanned:** ~25 (sync module + lint reserved + tests + E2E + export identity)  
**Pattern extraction date:** 2026-08-03
