# Phase 10: Overlap alias append util - Research

**Researched:** 2026-08-05
**Domain:** Frontend TypeScript note-content utils (wiki-link overlap alias append)
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Add a **named sibling util** (capability name e.g. `appendOverlapWikiLinkToNoteContent` under `frontend/src/utils/`) that always produces a wiki-link overlap item. Do **not** teach call sites to pass plain titles into `appendAliasToNoteContent`. Keep Wikidata / plain-alias `appendAliasToNoteContent` semantics unchanged. — **Reversibility:** reversible
- **D-02:** Implement by composing existing pieces: `buildWikiLinkText(...)` → then merge via existing frontmatter append path (`appendAliasToNoteContent` with the `[[…]]` token, or equivalent reuse of `parseNoteContentMarkdown` / `mergeAliasIntoList` / `composeNoteContentMarkdown`). Prefer composition over a second frontmatter parser. — **Reversibility:** reversible
- **D-03:** Reuse `buildWikiLinkText` qualification rules: same-notebook → `[[Title]]`; cross-notebook when `notebookName` available → `[[Notebook:Title]]`. Pass reviewed notebook id as `source.notebookId` and match realm/topology as `target` (same shape MatchedNoteLinkOffer uses). — **Reversibility:** reversible
- **D-04:** Do **not** pass `displayText` for overlap declaration (avoid `[[Title|display]]` pipe form). Overlap items should be whole-item wiki-link tokens matching `FrontmatterAliases` / `authoredAliasesValidation` wiki-link rules. — **Reversibility:** reversible
- **D-05:** Preserve the same contract as plain alias append: return updated markdown, or `null` when unchanged / unparseable / `aliases` present but not a YAML list / empty token. Dedupe via existing `mergeAliasIntoList` / `normalizedLookupKey` so repeating the same wiki-link token is a no-op. — **Reversibility:** reversible
- **D-06:** Preserve existing plain alias list items when appending a wiki-link overlap item (mixed `aliases` lists are already supported by backend + authored validation). — **Reversibility:** reversible
- **D-07:** **No UI wiring this phase** — do not add **Add as overlapped note**, do not call `updateTextField`, do not change `AnsweredSpellingQuestion` / resolve dialog / OVERLAP try-again chrome. Structure only. — **Reversibility:** reversible
- **D-08:** Cover with Vitest at the util boundary (capability-named `*.spec.ts`): appends a well-formed `[[…]]` wiki-link list item (not a bare title); creates `aliases` when absent; merges into existing list; returns `null` on duplicate / bad aliases shape; cross-notebook uses `Notebook:Title` when names/ids warrant it. Prefer asserting positive wiki-link shape + authoredAliasesValidation acceptance over mocking. No E2E required for this Structure phase. — **Reversibility:** reversible
- **D-09:** No backend / OpenAPI / `AnswerOutcome` / SRS changes. Downstream Phase 11 will persist via existing content-edit seam and must not re-grade. — **Reversibility:** reversible

### Claude's Discretion
- Exact util filename/export name (prefer capability clarity: overlap wiki-link append).
- Whether the sibling wraps `appendAliasToNoteContent` in one line or inlines the same merge helpers for clarity — either is fine if Pitfall 5 is avoided and tests lock the wiki-link shape.
- Fixture shape for `buildWikiLinkText` target (minimal stub vs small makeMe-like object) inside the util tests.

### Deferred Ideas (OUT OF SCOPE)
- Add as overlapped note (persist + no try-again / no reclaim) — Phase 11 (AMR-08, AMR-09)
- Title navigate, reopen resolve, E2E polish — Phase 12 (AMR-05)
- AMR-10..13 resolve polish and SEED-001 — v2 / parked seed

None — discussion stayed within phase scope (auto mode)
</user_constraints>

## Summary

Phase 10 is a **Structure-only** frontend slice: a named pure helper that turns a match target + reviewed notebook id into a **whole-item wiki-link** alias token and merges it into note frontmatter `aliases`, so Phase 11 can declare overlap without bloating the resolve dialog. The product risk this phase eliminates is **Pitfall 5** — call sites must not pass plain titles into `appendAliasToNoteContent` for overlap intent. Existing accidental-match and OVERLAP try-again flows must stay observably unchanged (no UI, no `updateTextField`, no SRS/OpenAPI work).

Everything needed already exists in-repo: `buildWikiLinkText` (qualification + optional pipe), `appendAliasToNoteContent` (parse → merge → compose with null-if-unchanged), `mergeAliasIntoList` / `normalizedLookupKey` (dedupe), and `authoredAliasesValidation` (accepts well-formed wiki-link overlap items). Backend `FrontmatterAliases.overlapWikiLinkTokensFrom*` is the **shape contract** to match in tests; **no Java change**. Zero new packages.

**Primary recommendation:** Add `appendOverlapWikiLinkToNoteContent` under `frontend/src/utils/` that calls `buildWikiLinkText(target, { notebookId })` with **no** `displayText`, then `appendAliasToNoteContent(content, token)`; lock the wiki-link shape with Vitest at `frontend/tests/utils/`.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Build overlap wiki-link token (`[[Title]]` / `[[Notebook:Title]]`) | Browser / Client (pure util) | — | Same rules as link-offer; no server round-trip |
| Merge token into frontmatter `aliases` | Browser / Client (pure util) | — | Content mutate happens client-side; Phase 11 persists via existing API |
| Dedupe / null-if-unchanged contract | Browser / Client | — | Reuse `mergeAliasIntoList` / `appendAliasToNoteContent` |
| Authored validation acceptance of wiki-link items | Browser / Client (mirror) | API / Backend (shape owner) | Frontend mirrors backend rules; this phase reads both for asserts only |
| Persist content / UI CTA / no re-grade | — (deferred Phase 11) | API / Backend | Out of scope; D-07 / D-09 |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| TypeScript + in-repo utils | current frontend | Composition only | Locked: zero new libraries [VERIFIED: `.planning/research/STACK.md`] |
| `buildWikiLinkText` | in-repo | Wiki-link token | Same helper `MatchedNoteLinkOffer` / SearchForm use [VERIFIED: `frontend/src/utils/buildWikiLinkText.ts:1-28`] |
| `appendAliasToNoteContent` | in-repo | Frontmatter `aliases` merge | Null-if-unchanged contract already shipped [VERIFIED: `frontend/src/utils/wikidataTitleActions.ts:26-62`] |
| Vitest | 4.1.10 | Util unit tests | Pinned in `frontend/package.json` [VERIFIED: `frontend/package.json` vitest pin] |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `yaml` (existing) | >=2.9.0 | Compose frontmatter via `composeNoteContentMarkdown` | Indirect — already used by append path; do not add a second parser [VERIFIED: `frontend/package.json`] |
| `authoredAliasesValidation` | in-repo | Assert produced list items are authored-valid | Prefer in Vitest over mocking [VERIFIED: `frontend/src/utils/authoredAliasesValidation.ts:8-33`] |
| `mergeAliasIntoList` / `normalizedLookupKey` | in-repo | Dedupe semantics | Reused inside `appendAliasToNoteContent` [VERIFIED: `frontend/src/utils/frontmatterAliases.ts:1-21`] |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Named sibling util | Teach callers to pass `[[Title]]` into `appendAliasToNoteContent` | **Forbidden** — Pitfall 5 / D-01 |
| New frontmatter parser | Reuse parse/merge/compose | Extra drift risk; D-02 prefers composition |
| New `POST …/declare-overlap` | Existing content-edit in Phase 11 | Out of scope; Architecture anti-pattern |
| Pipe `displayText` | Whole-item `[[…]]` only | D-04 forbids pipe for overlap declaration |

**Installation:**

```bash
# No new packages. Structure-only composition of in-repo utils.
```

**Version verification:** No new registry packages for this phase. Vitest already pinned at `4.1.10` in `frontend/package.json`. [VERIFIED: local package.json]

## Package Legitimacy Audit

> No external packages to install this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | N/A — zero installs |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
Phase 10 (this phase — pure, no I/O):

  reviewed notebookId + match target
       │
       ▼
  buildWikiLinkText(target, { notebookId })   ──►  "[[Title]]" | "[[Notebook:Title]]"
       │                                              (no displayText)
       ▼
  appendAliasToNoteContent(markdown, token)
       │
       ├─ parseNoteContentMarkdown
       ├─ mergeAliasIntoList (normalizedLookupKey dedupe)
       └─ composeNoteContentMarkdown
       │
       ▼
  string | null   (updated markdown or unchanged/invalid)

Phase 11 (deferred — do not implement here):

  resolve-dialog row CTA
       → appendOverlapWikiLinkToNoteContent(...)
       → storedApi.updateTextField(..., "edit content", …)
       → stay on ACCIDENTAL_MATCH (no retry / no SRS reclaim)
```

### Recommended Project Structure

```
frontend/src/utils/
├── appendOverlapWikiLinkToNoteContent.ts   # NEW — named sibling (capability name)
├── buildWikiLinkText.ts                    # REUSE — token shape
├── wikidataTitleActions.ts                 # REUSE — appendAliasToNoteContent unchanged
├── frontmatterAliases.ts                   # REUSE — merge/dedupe
└── authoredAliasesValidation.ts            # REUSE — test acceptance mirror

frontend/tests/utils/
├── appendOverlapWikiLinkToNoteContent.spec.ts  # NEW — capability-named Vitest
└── buildWikiLinkText.spec.ts                   # pattern reference
```

### Pattern 1: One-line composition wrapper (recommended)

**What:** Named export builds the token then delegates merge to `appendAliasToNoteContent`.
**When to use:** Default for this phase (D-01/D-02 discretion: wrap is fine).
**Example:**

```typescript
// Recommended shape — compose only; do not pass displayText (D-04)
import { buildWikiLinkText } from "./buildWikiLinkText"
import { appendAliasToNoteContent } from "./wikidataTitleActions"

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

Target/source shapes match `MatchedNoteLinkOffer`’s `NoteSearchResult`-like object and reviewed `notebookId` [VERIFIED: `frontend/src/components/recall/MatchedNoteLinkOffer.vue:53-60,87-89`]:

```typescript
return {
  noteTopology: realm.note.noteTopology,
  notebookId: realm.notebookRealm.notebook.id,
  notebookName: realm.notebookRealm.notebook.name,
}
```

`NoteSearchResult` wire type [VERIFIED: `packages/generated/doughnut-backend-api/types.gen.ts:324-332`]:

```typescript
export type NoteSearchResult = {
    noteTopology: NoteTopology;
    notebookId: number;
    notebookName?: string;
    distance?: number;
};
```

### Pattern 2: Vitest at util boundary (not component)

**What:** `frontend/tests/utils/<capability>.spec.ts` with `describe`/`it`/`expect`, minimal stubs for target (same as `buildWikiLinkText.spec.ts`).
**When to use:** Always for this Structure phase (D-08). No E2E, no dialog mount.

### Anti-Patterns to Avoid

- **Plain alias for overlap:** Passing match title into `appendAliasToNoteContent` without `[[…]]` — Pitfall 5; future OVERLAP grading never fires.
- **Teaching callers “just wrap the title”:** Defeats D-01 named sibling.
- **Passing `displayText`:** Produces pipe form; D-04 forbids for overlap declare (backend still *accepts* pipe tokens, but product wants whole-item unqualified display).
- **UI / `updateTextField` / dialog edits:** D-07 — Phase 11 only.
- **Backend / OpenAPI / outcome flips:** D-09 — Structure util only.
- **Second YAML parser:** D-02 — compose existing path.
- **Phase numbers in product filenames:** Capability names only (`appendOverlapWikiLink…`, not `phase10…`) [VERIFIED: `.cursor/rules/planning.mdc`].

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Wiki-link qualification | Custom same/cross-notebook string logic | `buildWikiLinkText` | Already tested; MatchedNoteLinkOffer contract |
| Frontmatter aliases merge | New YAML list writer | `appendAliasToNoteContent` | Parse/null/dedupe already correct |
| Dedupe keys | Ad-hoc lowercasing | `normalizedLookupKey` / `mergeAliasIntoList` | NFKC + backend parity |
| Authored wiki-link rules | Duplicate regex in the new util | Assert via `authoredAliasesValidation` in tests | Mirror already exists |
| Declare-overlap RPC | New endpoint | Phase 11 `updateTextField` content edit | Architecture anti-pattern |

**Key insight:** The hard problem is **call-site intent** (wiki-link vs plain), not parsing — a named sibling that always builds `[[…]]` is the cheapest Pitfall 5 fix.

## Common Pitfalls

### Pitfall 1: Plain alias instead of wiki-link (Pitfall 5)

**What goes wrong:** Frontmatter gets `- sedation` instead of `- "[[sedation]]"`; overlap grading never engages.
**Why it happens:** `appendAliasToNoteContent` is the obvious Wikidata helper and merges whatever string it is given.
**How to avoid:** Named util always runs `buildWikiLinkText` first; tests assert `[[`…`]]` shape and authored validation acceptance.
**Warning signs:** Spec expects bare title string in `aliases` list.

### Pitfall 2: Scope creep into Phase 11

**What goes wrong:** Dialog CTA / content save / try-again coupling lands early; Structure phase stops being stop-safe.
**Why it happens:** Util feels “incomplete” without a button.
**How to avoid:** D-07 fence — util + Vitest only; existing recall E2E/unit stay green with **no** new resolve actions.
**Warning signs:** Diffs under `AnsweredSpellingQuestion.vue` / `AccidentalMatchResolve*`.

### Pitfall 3: Pipe / displayText drift

**What goes wrong:** Tokens like `[[Title|friendly]]` when overlap declare should be whole-item target links.
**Why it happens:** Copying SearchForm dead-link path that passes `displayText`.
**How to avoid:** D-04 — omit `displayText` entirely in the overlap util.
**Warning signs:** Spec asserts `|` in produced alias item.

### Pitfall 4: Empty title → `[[]]`

**What goes wrong:** `buildWikiLinkText` can emit `[[]]` for empty title [VERIFIED: `frontend/src/utils/buildWikiLinkText.ts:9-27` + `buildWikiLinkText.spec.ts:39-42`]; authored validation rejects `[[]]` [VERIFIED: `frontend/tests/utils/authoredAliasesValidation.spec.ts:86-87`].
**Why it happens:** Util blindly forwards any title.
**How to avoid:** Phase 11 callers supply real match titles. Optional (discretion): return `null` if token fails wiki-link acceptance — not required if tests use non-empty titles and document the edge.
**Warning signs:** Spec covering empty title without expecting null or rejection.

### Pitfall 5: Conflating this util with OVERLAP try-again / ADR 0003 reclaim

**What goes wrong:** Later wiring regrades or shows try-again after declare.
**Why it happens:** Shared “overlap” language.
**How to avoid:** This phase has no UI; Phase 11 research must keep content-only mutate. Note: ADR 0003 is **Proposed** (not Accepted) as of research date — still honor milestone + CONTEXT separation of ACCIDENTAL_MATCH vs graded OVERLAP [CITED: `docs/adrs/0003-spaced-repetition-scheduling-policy.md` Status: Proposed].

### Pitfall 6: YAML quoting surprises

**What goes wrong:** Asserting unquoted `- [[Note]]` when `YAML.stringify` quotes wiki-link items.
**Why it happens:** Plain-alias Wikidata tests use unquoted scalars (`- Canine`).
**How to avoid:** Assert containment of the token string / parse back via `parseNoteContentMarkdown` + list items, or expect quoted YAML (`- "[[Other Note]]"`). Confirmed locally: `yaml` quotes `[[…]]` list items including `Notebook:Title` forms. [VERIFIED: nix `YAML.stringify` probe this session]

## Code Examples

### buildWikiLinkText (reuse — no displayText)

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
  // … optional pipe when displayText set — omit for overlap (D-04)
  return `[[${inner}]]`
}
```

Same-notebook → `[[CI]]`; cross-notebook with name → `[[Other NB:Deep Note]]` [VERIFIED: `frontend/tests/utils/buildWikiLinkText.spec.ts:9-21`].

### appendAliasToNoteContent contract (reuse unchanged)

```typescript
// Source: frontend/src/utils/wikidataTitleActions.ts:26-31
export function appendAliasToNoteContent(
  contentMarkdown: string,
  alias: string
): string | null {
  const trimmedAlias = alias.trim()
  if (!trimmedAlias) return null
  // … parse; create aliases list OR mergeAliasIntoList; null on bad shape / dup
}
```

### Backend overlap token shape (read-only)

```java
// Source: backend/.../FrontmatterAliases.java:44-47
public static List<String> overlapWikiLinkTokensFromNoteContent(String content) {
  return NoteContentMarkdown.splitLeadingFrontmatter(content == null ? "" : content)
      .map(lf -> overlapWikiLinkTokensFromFrontmatter(lf.frontmatter()))
      .orElse(List.of());
}
```

Backend tests expect whole-item tokens such as `"[[Other Note]]"` and `"[[Shared Notebook:Hue|display]]"` in `overlapWikiLinkTokensFromFrontmatter` [VERIFIED: `FrontmatterAliasesWikiLinkOverlapTest.java:56-68`]. Phase 10 produces **non-pipe** tokens; pipe acceptance is backend capability, not this phase’s emit shape.

### Suggested Vitest cases (D-08)

```typescript
// Pattern source: frontend/tests/utils/buildWikiLinkText.spec.ts + WikidataAssociationDialog append cases
import { describe, it, expect } from "vitest"
import { appendOverlapWikiLinkToNoteContent } from "@/utils/appendOverlapWikiLinkToNoteContent"
import { authoredAliasesValidationErrorForPropertyValue } from "@/utils/authoredAliasesValidation"
import { listPropertyValue } from "@/utils/noteProperties"
import { parseNoteContentMarkdown } from "@/utils/noteContentFrontmatterParse"

function target(title: string, notebookId: number, notebookName?: string) {
  return { noteTopology: { title }, notebookId, notebookName }
}

describe("appendOverlapWikiLinkToNoteContent", () => {
  it("appends a whole-item wiki-link alias, not a bare title", () => {
    const result = appendOverlapWikiLinkToNoteContent(
      "## Body\n",
      target("Sedation", 1, "NB"),
      { notebookId: 1 }
    )
    expect(result).toContain("[[Sedation]]")
    expect(result).not.toMatch(/aliases:\n\s+- Sedation\n/) // bare title anti-signal optional
  })

  it("uses Notebook:Title when cross-notebook and name present", () => {
    const result = appendOverlapWikiLinkToNoteContent(
      "## Body\n",
      target("Deep Note", 2, "Other NB"),
      { notebookId: 1 }
    )
    expect(result).toContain("[[Other NB:Deep Note]]")
  })

  it("returns null when the same wiki-link token already exists", () => {
    const markdown = `---\naliases:\n  - "[[Sedation]]"\n---\n\n# Body`
    expect(
      appendOverlapWikiLinkToNoteContent(markdown, target("Sedation", 1), {
        notebookId: 1,
      })
    ).toBeNull()
  })

  it("preserves existing plain aliases when appending a wiki-link item", () => {
    const markdown = `---\naliases:\n  - puppy\n---\n\n# Body`
    const result = appendOverlapWikiLinkToNoteContent(
      markdown,
      target("Canine", 1),
      { notebookId: 1 }
    )
    expect(result).toContain("puppy")
    expect(result).toContain("[[Canine]]")
  })
})
```

Prefer positive wiki-link shape + parsing list items + `authoredAliasesValidationErrorForPropertyValue(listPropertyValue([...])) === undefined` over brittle full-string YAML equality when quotes vary.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Wikidata plain-alias append only | Compose wiki-link token then same merge path | v1.2 Phase 10 | Overlap declare without wrong tool |
| Stacked match CTAs | Resolve dialog (Phases 7–9) then Phase 11 wire | v1.2 | Util keeps dialog SFC thin |

**Deprecated/outdated:**
- Using plain `appendAliasToNoteContent(title)` for “Add as overlapped note” — Pitfall 5; never for overlap intent.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Preferred export/file name `appendOverlapWikiLinkToNoteContent` is acceptable (discretion) | Standard Stack / Patterns | Rename only — low |
| A2 | One-line wrap of `appendAliasToNoteContent` is preferred over inlining merge helpers | Pattern 1 | Either OK per D-02; tests must still lock wiki-link shape |
| A3 | Empty-title → `[[]]` need not be specially nulled in the util if Phase 11 always has real titles | Pitfall 4 | Rare bad token if caller passes empty title |
| A4 | No new npm packages required | Package Audit | None if honored |

**If this table is empty:** — not empty; A1–A4 are discretion/edge assumptions, not blockers.

## Open Questions (RESOLVED)

1. **Should the util reject tokens that fail `isWikiLinkAliasItem` (e.g. `[[]]`)?**
   - What we know: authored validation rejects `[[]]`; real matches have titles.
   - What's unclear: whether to harden the util or leave caller responsibility.
   - Recommendation: Skip hardening in Phase 10 unless a test demands it; document in PLAN that Phase 11 passes real titles.
   - **RESOLVED:** Skip util hardening in Phase 10; Phase 11 passes real match titles (`10-01-PLAN.md` Task 1).

2. **Exact filename vs co-locate in `wikidataTitleActions.ts`?**
   - What we know: D-01 wants a **named sibling**; Wikidata file is plain-alias oriented.
   - What's unclear: new file vs export alongside appendAlias.
   - Recommendation: **New file** `appendOverlapWikiLinkToNoteContent.ts` for capability clarity and to avoid implying Wikidata ownership of overlap.
   - **RESOLVED:** New file `frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts` (not co-located in `wikidataTitleActions.ts`).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node / Nix frontend Vitest | Util tests | ✓ | node v24.5.0 (probe); vitest 4.1.10 | — |
| Existing utils (`buildWikiLinkText`, `appendAliasToNoteContent`) | Implementation | ✓ | in-repo | — |
| Backend / MySQL | This phase | N/A | — | No backend work (D-09) |
| E2E / Cypress | This phase | N/A | — | No E2E required (D-08) |

**Missing dependencies with no fallback:** none

**Missing dependencies with fallback:** none

Step 2.6: External runtime deps are the existing frontend test toolchain only — available.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Vitest 4.1.10 (frontend browser suite host; pure util specs do not need DOM) |
| Config file | frontend Vitest config (existing) |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/utils/appendOverlapWikiLinkToNoteContent.spec.ts` |
| Full suite command | `CURSOR_DEV=true nix develop -c pnpm frontend:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| — (Structure; enables AMR-08) | Append wiki-link overlap alias token into frontmatter | unit (util) | `pnpm -C frontend test tests/utils/appendOverlapWikiLinkToNoteContent.spec.ts` | ❌ Wave 0 |
| Success criterion 1 | Accidental-match + OVERLAP try-again unchanged | regression (existing) | existing recall Vitest / no dialog diffs | ✅ existing |
| Success criterion 3 | No Add as overlapped CTA | review / diff fence | no UI file changes | ✅ process |

### Sampling Rate

- **Per task commit:** targeted util spec above
- **Per wave merge:** `pnpm frontend:test` (or at least utils + no unintended recall diffs)
- **Phase gate:** util green; no new UI; existing accidental-match / overlap specs untouched and still green

### Wave 0 Gaps

- [ ] `frontend/tests/utils/appendOverlapWikiLinkToNoteContent.spec.ts` — covers Structure success criteria 2 (wiki-link append / merge / null / cross-notebook)
- [ ] `frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts` — production util (implementation after failing tests)

*(No framework install needed — Vitest already present.)*

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — (no new auth surface) |
| V3 Session Management | no | — |
| V4 Access Control | no (this phase) | Phase 11 must reuse existing authenticated `updateTextField` + readonly gates (AMR-07 pattern) |
| V5 Input Validation | yes (local) | Reuse trim/empty/`aliases` list-shape null contract; authored wiki-link rules mirrored in `authoredAliasesValidation` |
| V6 Cryptography | no | — |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Client invents unsafe alias text | Tampering | Whole-item wiki-link via `buildWikiLinkText`; authored validation rejects malformed items |
| New unauthenticated declare-overlap API | Elevation | Do not add — Phase 11 uses existing content edit (D-09) |
| Readonly notebook mutation | Elevation | Out of scope here; Phase 11 ports AMR-07 gates |

## Project Constraints (from .cursor/rules/)

| Directive | Implication for Phase 10 |
|-----------|--------------------------|
| Structure phase: no external behavior change; enables **only** immediate next behavior (Phase 11) | Util + tests only; no CTA |
| Stop-safe; one structure change | Named overlap append helper only |
| Capability names in product code — no phase numbers | `appendOverlapWikiLinkToNoteContent`, not `phase10…` |
| Small-test style: stable boundary, data over mocks, focused assertions | Drive the new util; assert wiki-link delta; reuse stubs like `buildWikiLinkText.spec.ts` |
| Frontend Vitest: Nix-prefixed `pnpm frontend:test` / single-file path under `frontend/` | Use documented commands |
| Prefer util-boundary Vitest for pure helpers that are domain-stable contracts | Matches D-08 |
| No new libraries for resolve UX stack | Zero installs |
| GSD + local wrap-up (Jidoka, refactor, commit+push) at execute time | Planner/executor; not research |
| ADR awareness: Accepted ADRs bind; Proposed do not silently bind | ADR 0003 is Proposed — follow CONTEXT/milestone separation anyway |
| Assume `pnpm sut` / Nix; git without Nix prefix | Standard env |

## Sources

### Primary (HIGH confidence)

- `frontend/src/utils/buildWikiLinkText.ts` — token rules
- `frontend/src/utils/wikidataTitleActions.ts` — `appendAliasToNoteContent`
- `frontend/src/utils/frontmatterAliases.ts` — merge/dedupe
- `frontend/src/utils/authoredAliasesValidation.ts` — wiki-link acceptance
- `frontend/src/components/recall/MatchedNoteLinkOffer.vue` — target/source shape for `buildWikiLinkText`
- `backend/.../FrontmatterAliases.java` + `FrontmatterAliasesWikiLinkOverlapTest.java` — overlap token shape (read-only)
- `.planning/research/PITFALLS.md` Pitfall 5; `ARCHITECTURE.md` build order step 1; `STACK.md` zero new libs
- `.planning/phases/10-overlap-alias-append-util/10-CONTEXT.md` — D-01..D-09

### Secondary (MEDIUM confidence)

- Context7 `/vitest-dev/vitest` — `describe`/`it`/`expect` for TypeScript utils [CITED: vitest docs writing-tests]
- DaisyUI/Vue stack pins in STACK.md — not exercised this phase

### Tertiary (LOW confidence)

- External “YAML aliases merge” web patterns — superseded by in-repo helpers; not used for recommendations

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — zero new packages; all seams read this session
- Architecture: HIGH — composition path and Phase 11 boundary verified in code + CONTEXT
- Pitfalls: HIGH — Pitfall 5 + YAML quoting + empty-title edge verified against sources

**Research date:** 2026-08-05
**Valid until:** 2026-09-04 (stable in-repo APIs; revisit if `buildWikiLinkText` / appendAlias contracts change)
