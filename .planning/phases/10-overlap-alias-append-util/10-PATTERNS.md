# Phase 10: Overlap alias append util - Pattern Map

**Mapped:** 2026-08-05
**Files analyzed:** 2 (new) + 4 reuse-only seams (no edit)
**Analogs found:** 2 / 2

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts` | utility | transform | `frontend/src/utils/buildWikiLinkText.ts` + `frontend/src/utils/wikidataTitleActions.ts` (`appendAliasToNoteContent`) | exact (compose both) |
| `frontend/tests/utils/appendOverlapWikiLinkToNoteContent.spec.ts` | test | transform | `frontend/tests/utils/buildWikiLinkText.spec.ts` | exact |

**Reuse unchanged (do not edit this phase):**

| File | Role | Why listed |
|------|------|------------|
| `frontend/src/utils/buildWikiLinkText.ts` | utility | Token builder; call with no `displayText` (D-04) |
| `frontend/src/utils/wikidataTitleActions.ts` | utility | Keep `appendAliasToNoteContent` semantics; wrap only |
| `frontend/src/utils/frontmatterAliases.ts` | utility | Dedupe via existing merge path |
| `frontend/src/utils/authoredAliasesValidation.ts` | utility | Assert acceptance in Vitest (D-08) |

## Pattern Assignments

### `frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts` (utility, transform)

**Analog (composition):** `buildWikiLinkText` → `appendAliasToNoteContent`  
**Call-site shape analog:** `frontend/src/components/recall/MatchedNoteLinkOffer.vue` (target + source notebookId)

**Imports pattern** — mirror sibling utils under `frontend/src/utils/`:

```typescript
import { buildWikiLinkText } from "./buildWikiLinkText"
import { appendAliasToNoteContent } from "./wikidataTitleActions"
```

**Core composition pattern** (recommended one-line wrap — RESEARCH Pattern 1 / D-01–D-02):

```typescript
export function appendOverlapWikiLinkToNoteContent(
  contentMarkdown: string,
  target: {
    noteTopology: { title: string }
    notebookId: number
    notebookName?: string
  },
  source: { notebookId?: number }
): string | null {
  const token = buildWikiLinkText(target, { notebookId: source.notebookId })
  return appendAliasToNoteContent(contentMarkdown, token)
}
```

**Wiki-link token rules** — copy from `buildWikiLinkText.ts` lines 1–28; **omit** `displayText`:

```typescript
// Source: frontend/src/utils/buildWikiLinkText.ts:1-28
export function buildWikiLinkText(
  target: {
    noteTopology: { title: string }
    notebookId: number
    notebookName?: string
  },
  source: { notebookId?: number; displayText?: string }
): string {
  const title = target.noteTopology.title
  const useNotebookPrefix =
    source.notebookId !== undefined &&
    target.notebookId !== source.notebookId &&
    Boolean(target.notebookName)

  const defaultInner = useNotebookPrefix
    ? `${target.notebookName}:${title}`
    : title

  const trimmedDisplay = source.displayText?.trim() ?? ""
  const inner =
    trimmedDisplay.length > 0 &&
    defaultInner.length > 0 &&
    trimmedDisplay !== defaultInner
      ? `${defaultInner}|${trimmedDisplay}`
      : defaultInner

  return `[[${inner}]]`
}
```

**Merge / null contract** — reuse unchanged from `wikidataTitleActions.ts` lines 26–62:

```26:62:frontend/src/utils/wikidataTitleActions.ts
export function appendAliasToNoteContent(
  contentMarkdown: string,
  alias: string
): string | null {
  const trimmedAlias = alias.trim()
  if (!trimmedAlias) return null

  const parsed = parseNoteContentMarkdown(contentMarkdown)
  if (!parsed.ok) return null

  const aliasesKey = findAliasesPropertyKey(parsed.properties)
  if (!aliasesKey) {
    return composeNoteContentMarkdown({
      properties: {
        ...parsed.properties,
        aliases: listPropertyValue([trimmedAlias]),
      },
      body: parsed.body,
    })
  }

  const existingValue = parsed.properties[aliasesKey]
  if (existingValue === undefined || !isListPropertyValue(existingValue)) {
    return null
  }

  const merged = mergeAliasIntoList(existingValue.items, trimmedAlias)
  if (merged === null) return null

  return composeNoteContentMarkdown({
    properties: {
      ...parsed.properties,
      [aliasesKey]: listPropertyValue(merged),
    },
    body: parsed.body,
  })
}
```

**Target/source shape** — same as MatchedNoteLinkOffer (lines 53–60, 87–89):

```53:60:frontend/src/components/recall/MatchedNoteLinkOffer.vue
  return {
    noteTopology: realm.note.noteTopology,
    notebookId: realm.notebookRealm.notebook.id,
    notebookName: realm.notebookRealm.notebook.name,
  }
```

```87:89:frontend/src/components/recall/MatchedNoteLinkOffer.vue
  const linkText = buildWikiLinkText(target, {
    notebookId: sourceNotebookId.value,
  })
```

**Anti-pattern (do not copy):** SearchForm / dead-link paths that pass `displayText` (pipe form). D-04 forbids pipe for overlap declaration.

**Auth / error handling:** N/A — pure transform; no HTTP. Null return is the failure signal (same as `appendAliasToNoteContent`).

---

### `frontend/tests/utils/appendOverlapWikiLinkToNoteContent.spec.ts` (test, transform)

**Primary analog:** `frontend/tests/utils/buildWikiLinkText.spec.ts`  
**Secondary analog (merge/null cases):** `frontend/tests/notes/WikidataAssociationDialog.spec.ts` describe `"append alias to frontmatter"` (lines 411–474)  
**Acceptance asserts:** `frontend/tests/utils/authoredAliasesValidation.spec.ts` (wiki-link items)

**Imports + stub pattern** (lines 1–6 of buildWikiLinkText.spec):

```1:6:frontend/tests/utils/buildWikiLinkText.spec.ts
import { describe, it, expect } from "vitest"
import { buildWikiLinkText } from "@/utils/buildWikiLinkText"

function makeTarget(title: string, notebookId: number, notebookName?: string) {
  return { noteTopology: { title }, notebookId, notebookName }
}
```

Adapt for the new util:

```typescript
import { describe, it, expect } from "vitest"
import { appendOverlapWikiLinkToNoteContent } from "@/utils/appendOverlapWikiLinkToNoteContent"
import { authoredAliasesValidationErrorForPropertyValue } from "@/utils/authoredAliasesValidation"
import { listPropertyValue } from "@/utils/noteProperties"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatter"

function makeTarget(title: string, notebookId: number, notebookName?: string) {
  return { noteTopology: { title }, notebookId, notebookName }
}
```

**Same- vs cross-notebook expectations** — copy assertion shape from buildWikiLinkText.spec lines 9–21:

```9:21:frontend/tests/utils/buildWikiLinkText.spec.ts
  it("returns simple wiki link when source and target are in the same notebook", () => {
    expect(
      buildWikiLinkText(makeTarget("CI", 1, "Doughnut"), { notebookId: 1 })
    ).toBe("[[CI]]")
  })

  it("returns qualified wiki link when target is in a different notebook with a name", () => {
    expect(
      buildWikiLinkText(makeTarget("Deep Note", 2, "Other NB"), {
        notebookId: 1,
      })
    ).toBe("[[Other NB:Deep Note]]")
  })
```

For the append util: assert `result.toContain("[[…]]")` / `"[[Notebook:Title]]"` rather than full YAML string equality (YAML quotes wiki-link scalars — RESEARCH Pitfall 6).

**Merge / create / null contract** — copy scenarios from WikidataAssociationDialog.spec lines 411–473:

```411:473:frontend/tests/notes/WikidataAssociationDialog.spec.ts
  describe("append alias to frontmatter", () => {
    it("writes a YAML aliases list instead of appending to the title", () => {
      const result = appendAliasToNoteContent("## Workshop\n", "Canine")
      expect(result).toBe(`---\naliases:\n  - Canine\n---\n## Workshop\n`)
    })
    // … preserves frontmatter; merges into existing list;
    // … returns null on normalized dup / non-list aliases
  })
```

Adapt: pass wiki-link tokens via the new util (not bare `"Canine"`); assert mixed list preserves plain items (D-06); assert duplicate wiki-link → `null`. Prefer parse-back + `toContain("[[…]]")` over brittle full-string YAML for wiki-link items.

**Authored validation acceptance** — copy assert style from authoredAliasesValidation.spec lines 72–84:

```72:84:frontend/tests/utils/authoredAliasesValidation.spec.ts
  it("accepts well-formed wiki-link overlap alias items", () => {
    expect(
      authoredAliasesValidationErrorForPropertyValue(
        listPropertyValue([
          "color",
          "[[Other Note]]",
          "[[Shared Notebook:Hue]]",
          "[[Title|display]]",
          "[[Shared Notebook:Hue|display]]",
        ])
      )
    ).toBeUndefined()
  })
```

In Phase 10 specs: after append, parse aliases list items and assert `authoredAliasesValidationErrorForPropertyValue(listPropertyValue(items)) === undefined`. Do **not** assert pipe forms as emit shape (D-04); backend accepts pipes, this util must not emit them.

**Focused D-08 cases (planner checklist):**
1. Appends whole-item `[[…]]`, not bare title
2. Creates `aliases` when absent
3. Merges into existing list
4. Returns `null` on duplicate / bad aliases shape
5. Cross-notebook → `[[Notebook:Title]]` when names/ids warrant
6. Preserves existing plain alias items alongside wiki-link (D-06)

**Do not test:** UI, `updateTextField`, dialog mount, E2E (D-07 / D-08).

## Shared Patterns

### Pure util composition (no second parser)

**Source:** `buildWikiLinkText.ts` + `appendAliasToNoteContent` in `wikidataTitleActions.ts`  
**Apply to:** New overlap append util only  
**Rule:** Compose existing parse → `mergeAliasIntoList` → compose path; never add a second YAML frontmatter writer (D-02).

### Null-if-unchanged / invalid shape

**Source:** `appendAliasToNoteContent` (empty trim, parse fail, non-list aliases, dedupe via `mergeAliasIntoList`)  
**Apply to:** New util return type `string | null`  
**Dedupe key:** `normalizedLookupKey` in `frontmatterAliases.ts` lines 1–21.

### Capability-named sibling (Pitfall 5)

**Source:** CONTEXT D-01 + RESEARCH Open Question 2  
**Apply to:** New file `appendOverlapWikiLinkToNoteContent.ts` — do **not** co-locate in `wikidataTitleActions.ts` (avoids implying Wikidata owns overlap). Do **not** teach callers to pass plain titles into `appendAliasToNoteContent` for overlap intent.

### Vitest at util boundary

**Source:** `frontend/tests/utils/buildWikiLinkText.spec.ts`  
**Apply to:** `frontend/tests/utils/appendOverlapWikiLinkToNoteContent.spec.ts`  
**Run:** `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/utils/appendOverlapWikiLinkToNoteContent.spec.ts`

### No UI / no backend this phase

**Fence:** Do not touch `AnsweredSpellingQuestion`, AccidentalMatch resolve dialog, OpenAPI, `AnswerOutcome`, SRS (D-07 / D-09). Phase 11 wires CTA + `updateTextField`.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| — | — | — | Both new files have exact in-repo analogs |

## Metadata

**Analog search scope:** `frontend/src/utils/`, `frontend/tests/utils/`, `frontend/tests/notes/WikidataAssociationDialog.spec.ts`, `frontend/src/components/recall/MatchedNoteLinkOffer.vue`  
**Files scanned:** ~33 utils + 18 util specs + appendAlias call sites  
**Pattern extraction date:** 2026-08-05
