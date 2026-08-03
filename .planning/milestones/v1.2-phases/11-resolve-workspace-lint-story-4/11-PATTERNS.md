# Phase 11: Resolve workspace lint (story 4) - Pattern Map

**Mapped:** 2026-08-03
**Files analyzed:** 6
**Analogs found:** 6 / 6

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `cli/src/lint/lintWorkspace.ts` | service | transform + request-response | same file (orchestrate OKF → append portable) | exact |
| `cli/src/lint/portable*.ts` (NEW helpers under `cli/src/lint/`) | utility | transform (Map → Finding[]) | `okfConcept.ts` / `okfLog.ts` (OkfProblem factories) + `previewPullActions.ts` (import `extractDoughnutId` / `unsafePathReason` only) | role-match + import |
| `cli/tests/lintWorkspace.test.ts` | test | transform | same file (invert must-not-reject; cascade `index.md`) | exact |
| `e2e_test/features/cli/cli_lint_workspace.feature` | test | request-response | same feature (fix conformant + four gap scenarios) | exact |
| `cli/src/commands/lintSlashCommand.ts` | route | request-response | same file (doc/help only if needed) | exact |
| `cli/tests/portable*.test.ts` (optional) | test | transform | prefer coverage via `lintWorkspace.test.ts` (`cli.mdc` export discipline) | role-match |

**Import-only (do not edit — HYG-02):** `cli/src/sync/previewPullActions.ts` (Terry Yin). Prefer no edits to `readWorkspace.ts` / `directoryArgument.ts` (D-09). Do not touch Stories 5–6 push modules.

**Keep unchanged (OKF base / report):** `okfConcept.ts`, `okfIndex.ts`, `okfLog.ts`, `okfProblem.ts`, `lintReport.ts`, `bundleFiles.ts` — unless a portable helper needs a tiny shared type import from `okfProblem` / `lintReport`.

## Pattern Assignments

### `cli/src/lint/lintWorkspace.ts` (service, transform + request-response)

**Analog:** same file — extend after OKF `flatMap`; do not replace `problemsIn`.

**Imports pattern** (lines 1–8) — keep; add portable helper import:
```typescript
import { parseDirectoryArgument } from '../sync/directoryArgument.js'
import { readWorkspace } from '../sync/readWorkspace.js'
import { isHidden, nonMarkdownPaths } from './bundleFiles.js'
import { type Finding, lintReport } from './lintReport.js'
import { conceptProblems } from './okfConcept.js'
import { indexProblems } from './okfIndex.js'
import { logProblems } from './okfLog.js'
import type { OkfProblem } from './okfProblem.js'
// NEW: import { portableContractFindings } from './portableContract.js'  // or discrete portable*.ts
```

**Reserved-role dispatch** (lines 18–23) — keep as-is; portable rules operate on the full note map, not inside `problemsIn`:
```typescript
function problemsIn(path: string, content: string): OkfProblem[] {
  const name = basename(path)
  if (name === 'index.md') return indexProblems(content, path === name)
  if (name === 'log.md') return logProblems(content)
  return conceptProblems(content)
}
```

**Core orchestration** (lines 34–48) — **additive** portable findings (RESEARCH Pattern 1 / D-02):
```typescript
export function lintWorkspace(argument: string): string {
  const parsed = parseDirectoryArgument(argument)
  if (parsed.error !== undefined) return parsed.error
  const bundle = parsed.directory

  const notes = [...readWorkspace(bundle)].filter(([path]) => !isHidden(path))
  const okfFindings = notes.flatMap(([path, content]) =>
    problemsIn(path, content).map((problem) => ({ ...problem, path }))
  )
  // NEW: const portableFindings = portableContractFindings(notes) // or Map from notes
  return lintReport([
    ...okfFindings,
    // ...portableFindings,
    ...nonMarkdownPaths(bundle).map(notAConcept),
  ])
}
```

**Error / report handling:** still only via `lintReport` — no second success slogan (D-04). `parseDirectoryArgument` error strings stay early-return.

---

### `cli/src/lint/portable*.ts` (utility, transform)

**Analog (Finding / OkfProblem shape):** `cli/src/lint/okfProblem.ts` + `lintReport.ts` `Finding`  
**Analog (identity):** import `extractDoughnutId` from `previewPullActions.ts` — do **not** reimplement  
**Analog (unsafe paths):** import `unsafePathReason` from `previewPullActions.ts` — do **not** reimplement  
**Analog (wiki extract regex):** `frontend/src/utils/noteContentWikiLinks.ts` + `WikiLinkMarkdown.INNER_LINK_PATTERN` (mirror in CLI; do not import frontend)  
**Analog (multi-path id index idea):** `indexPathsByDoughnutId` in preview (first-wins Map) — lint needs **all** colliding paths, so build a multi-map locally rather than calling that helper as-is

**Finding construction** (`okfProblem.ts` lines 1–12):
```typescript
export type OkfProblem = {
  readonly severity: 'error' | 'warning'
  readonly line?: number
  readonly message: string
}

export function error(message: string, line = 1): OkfProblem[] {
  return [{ severity: 'error', line, message }]
}
```

Portable helpers should return `Finding[]` (`OkfProblem & { path }`) or `OkfProblem[]` mapped with `path` at the call site — match existing `lintWorkspace` mapping style.

**Import-only identity + unsafe path** (`previewPullActions.ts` lines 20–74) — **copy call sites, never edit the file**:
```typescript
export function extractDoughnutId(content: string): string | undefined {
  const lines = content.replace(/\r\n/g, '\n').split('\n')
  if (lines[0] !== '---') return
  for (let i = 1; i < lines.length; i++) {
    if (lines[i] === '---') return
    const match = DOUGHNUT_ID_LINE.exec(lines[i])
    if (match) {
      const id = match[1]?.trim()
      return id === undefined || id === '' ? undefined : id
    }
  }
  return
}

export function unsafePathReason(path: string): string | undefined {
  if (
    path.startsWith('/') ||
    path.includes('\\') ||
    path.split('/').includes('..') ||
    path.split('/').includes('')
  ) {
    return 'unsafe path — not a portable pull target'
  }
  return
}
```

**Reserved basenames for duplicate-id exclusion** (mirror locally; preview keeps these private — D-05):
```typescript
// From previewPullActions.ts lines 10–11 (do not import private RESERVED_BASENAMES)
const RESERVED_BASENAMES = new Set(['index.md', 'log.md'])
// Exclude basename ∈ set from doughnut_id collision map
```

**Reject vocabulary mirror** (optional path-mapping messages — `previewPullActions.ts` lines 129–144) — reuse phrases where practical (D-08):
```typescript
// 'unsafe path — not a portable pull target'  ← from unsafePathReason
// 'sync metadata under .doughnut-sync — never a pull target'
// 'reserved role file — not an ordinary pull target'
```

**Wiki link extract** (`frontend/src/utils/noteContentWikiLinks.ts` lines 1–8) — CLI-local copy of regex; path-oriented resolve per RESEARCH A1:
```typescript
const WIKI_LINK = /\[\[([^\]]+)]]/g
// matchAll → trim → split on first `|` → target; resolve as workspace path / target.md
```

**Markdown link skip list** (RESEARCH D-06 — implement in portable links helper):
```typescript
function isRemoteOrIgnoredHref(href: string): boolean {
  const t = href.trim()
  if (/^https?:\/\//i.test(t)) return true
  if (/^\/attachments\//i.test(t)) return true
  return false
}
// Leading `/` that is not remote → workspace-root-relative (strip `/`, resolve under workspace)
```

**Hidden skip** — reuse `isHidden` from `bundleFiles.ts` (lines 11–13) for source paths; do not scan `.doughnut-sync` / dot dirs as concept sources.

**Module export discipline (`cli.mdc`):** export only what `lintWorkspace` imports (e.g. one `portableContractFindings`). Leave internal scanners unexported; prefer proving through `lintWorkspace` rather than exporting for tests.

---

### `cli/tests/lintWorkspace.test.ts` (test, transform)

**Analog:** same file — fixture helpers `write` / `concept` / tmpdir lifecycle.

**Fixture harness** (lines 7–26):
```typescript
describe('lintWorkspace', () => {
  let root: string
  beforeEach(() => {
    root = mkdtempSync(join(tmpdir(), 'doughnut-lintWorkspace-'))
  })
  afterEach(() => {
    rmSync(root, { recursive: true, force: true })
  })
  const write = (relativePath: string, content: string) => {
    const full = join(root, relativePath)
    mkdirSync(join(full, '..'), { recursive: true })
    writeFileSync(full, content, 'utf8')
  }
  const concept = (keys: string, body: string) =>
    `---\n${keys}\n---\n\n# ${body}`
```

**Invert D-03** (today lines 261–271 — **after** Phase 11 must report errors):
```typescript
test('a link to a concept that is not in the bundle', () => {
  write('apple.md', `${concept('type: concept', 'apple')}\n\n[go](/pear)`)
  write('index.md', '# Fruit\n') // required once D-07 lands
  expect(lintWorkspace(root)).toMatch(/error/i)
  expect(lintWorkspace(root)).toMatch(/pear|link|missing|broken/i)
})

test('one concept carrying only a `type`, and no index.md', () => {
  write('apple.md', concept('type: concept', 'apple'))
  expect(lintWorkspace(root)).toMatch(/index\.md/i)
  expect(lintWorkspace(root)).not.toBe('Workspace follows the OKF format.')
})
```

**Keep must-not-reject** for unrecognised `type` and unknown keys (lines 246–259).

**Fixture cascade:** every CONFORMS expectation that writes concept `.md` without `index.md` needs a minimal `index.md` (and nested `fruit/index.md` where concepts live under `fruit/`) in the same change that enables missing-index errors (Pitfall 1). Existing success cases include e.g. lines 151–156, 192–197, 234–238.

**New units (same file preferred):** duplicate `doughnut_id` on two concept paths; wiki broken target; synthetic unsafe path / unsafe link target via `unsafePathReason` messaging. Capability names only — no phase numbers.

---

### `e2e_test/features/cli/cli_lint_workspace.feature` (test, request-response)

**Analog:** same feature — Background empty workspace + `/lint ./Workspace` + past assistant assertions.

**Scenario shape** (lines 12–19) — copy for new gap scenarios:
```gherkin
  Scenario: A concept without frontmatter is an error
    Given the workspace "./Workspace" has a file "a.md" with content:
      """
      # apple
      """
    When I enter the slash command "/lint ./Workspace" in the interactive CLI
    Then I should see "a.md:1 error Frontmatter is missing" with any spacing in past CLI assistant messages
```

**Fix conformant** (lines 41–57) — D-03: add `banana.md` + root `index.md` (and keep `.git/config.md` unscanned proof):
```gherkin
  Scenario: A conformant bundle reports nothing
    # today links ./banana.md without the file — will fail once link checks land
    # add banana.md (valid concept) + index.md; assert CONFORMS
```

**Add four gap scenarios** (capability-named): duplicate `doughnut_id`; broken local link; missing `index.md`; unsupported/unsafe path (prefer unsafe **link** target `../…` — Pitfall 4). Keep success string `Workspace follows the OKF format.` Keep existing malformed / non-md warning scenarios green (cascade `index.md` into warning-only success paths that still expect CONFORMS).

---

### `cli/src/commands/lintSlashCommand.ts` (route, request-response)

**Analog:** same file — touch only if help/description should mention portable contract (discretion; D-09).

```typescript
export const lintSlashCommand: InteractiveSlashCommand = {
  literal: '/lint',
  doc: lintDoc,
  argument: { name: 'workspace directory', optional: false },
  run: (argument) => ({ assistantMessage: lintWorkspace(argument ?? '') }),
}
```

Do not change run wiring; success copy stays in `lintReport`.

---

### Optional `cli/tests/portable*.test.ts`

**Prefer not creating** unless scanners are non-trivial and cannot be proven via `lintWorkspace`. If created, use the same tmpdir/`write` harness as `lintWorkspace.test.ts`, still importing only public lint API.

## Shared Patterns

### HYG-02 import-only (Terry / Tan Yeong Sheng)
**Source:** Phase 10 precedent + `cli/src/sync/previewPullActions.ts`  
**Apply to:** All portable identity / unsafe-path work  
**Rule:** `import { extractDoughnutId, unsafePathReason } from '../sync/previewPullActions.js'` — never edit that file; never duplicate the id regex / unsafe checks.

### Finding line format + CONFORMS
**Source:** `cli/src/lint/lintReport.ts` lines 1–39  
**Apply to:** All portable errors (prefer `severity: 'error'` for oracle gaps)  
```typescript
const CONFORMS = 'Workspace follows the OKF format.'
// `${path}:${line}  ${severity}  ${message}` or `${path}  ${severity}  ${message}`
// empty findings → CONFORMS; warnings-only → `${CONFORMS} ${counts}`
```

### Reserved roles vs concepts
**Source:** `lintWorkspace.problemsIn` + preview `RESERVED_BASENAMES`  
**Apply to:** Duplicate-id set (exclude `index.md` / `log.md`); missing-index rule (concept-bearing dirs only); path-mapping (reserved not ordinary concepts).

### Read-only scan
**Source:** `lintWorkspace` + `readWorkspace`  
**Apply to:** Entire Phase 11 — no `writeFile` in lint path (D-09).

### Vitest observable behavior
**Source:** `cli.mdc` + existing `lintWorkspace.test.ts`  
**Apply to:** Assert stdout string from `lintWorkspace(root)`; do not export helpers solely for tests.

### E2E slash-command proof
**Source:** `cli_lint_workspace.feature` + `e2e-authoring.mdc`  
**Apply to:** Targeted `cypress run --spec e2e_test/features/cli/cli_lint_workspace.feature`; capability names; no `@wip` left green at phase end; no phase numbers in scenario titles.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | No greenfield surfaces. Link/wiki scanners are new logic but mirror frontend/backend regex + lint Finding shape. |

**Note:** There is no existing CLI markdown-link resolver. Closest pieces: wiki regex (frontend/backend), `unsafePathReason` (preview), Finding factories (`okfProblem`). Planner should invent small regex extractors under `cli/src/lint/portable*.ts` rather than adding a markdown AST package.

## Metadata

**Analog search scope:** `cli/src/lint/`, `cli/src/sync/previewPullActions.ts`, `cli/src/commands/lintSlashCommand.ts`, `cli/tests/lintWorkspace.test.ts`, `e2e_test/features/cli/cli_lint_workspace.feature`, `frontend/src/utils/noteContentWikiLinks.ts`, `backend/.../WikiLinkMarkdown.java`, Phase 10 PATTERNS (HYG-02 import-only)  
**Files scanned:** ~15 primary + prior phase pattern map  
**Pattern extraction date:** 2026-08-03

### Planner prohibitions (copy into PLAN)
- Do **not** edit `cli/src/sync/previewPullActions.ts` (HYG-02)
- Do **not** rewrite Terry Yin / Tan Yeong Sheng files
- Do **not** install new npm packages
- Do **not** change Stories 5–6 push modules
- Do **not** change CONFORMS success string (D-04)
- Do **not** encode phase numbers in product/test names
- Prefer **1 plan / 1–2 larger tasks** (D-11)
