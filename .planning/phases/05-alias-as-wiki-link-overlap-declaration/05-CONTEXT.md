# Phase 5: Alias-as-wiki-link overlap declaration - Context

**Gathered:** 2026-07-24
**Status:** Ready for planning
**Mode:** `--auto` (all gray areas auto-selected; recommended option chosen for each; no interactive prompts)

<domain>
## Phase Boundary

Extend the note `aliases` frontmatter so list items may be **wiki-link values** (e.g. `[[Other Note]]`) that **declare overlap** with another note (OVL-02), without regressing existing **wiki-resolve**, **search**, or **cloze-masking** behavior for plain aliases (OVL-03).

**In this phase (Structure — declaration model only):**
- Accept well-formed wiki-link tokens as `aliases` list items (authored validation + parse path).
- Segregate **plain aliases** vs **overlap wiki-link aliases** so existing consumers keep plain-only semantics.
- Expose a small extraction API so Phase 6 can read declared overlaps without re-inventing YAML parsing.
- Gate with regression coverage for each alias consumer (wiki resolve, search/index, cloze, `matchAnswer`).

**Not in this phase (later phases):**
- Overlap "try again, no credit" grading / UI / `Answer.outcome = OVERLAP` / `AnsweredQuestion.overlap` → Phase 6 (OVL-01).
- Accidental-match / offer-link behavior — already shipped in Phases 2–4; do not reopen.

</domain>

<decisions>
## Implementation Decisions

### Wiki-link alias syntax
- **D-01:** Accept standard Doughnut wiki-link token forms already understood by `WikiLinkMarkdown` / `WikiLinkTargetReference`: `[[Title]]`, qualified `[[Notebook:Title]]`, and pipe-display `[[Title|display]]` / `[[Notebook:Title|display]]`. Overlap semantics use the **target** segment (`WikiLinkMarkdown.splitInner(…).target()`), not the display segment. — **Reversibility:** reversible — validation/parser rules; can narrow forms later.
  - Rationale: Reuses the product's existing wiki-link grammar instead of inventing a second alias-only dialect. Today `FrontmatterAliases.isValidAliasText` rejects any `[[`/`]]`; Phase 5 flips that for well-formed tokens only.

### Consumer segregation (OVL-03)
- **D-02:** Segregate at parse time in `FrontmatterAliases` (and keep frontend helpers consistent if they validate aliases). **Only plain aliases** participate in: `NoteAliasIndex` / search, wiki-resolve alias targets, cloze masking (`hideAliases`), and `Note.matchAnswer`. **Wiki-link aliases are overlap declarations only** — never indexed as searchable alias text, never used as wiki-resolve alias targets, never cloze-masked as alias strings, never counted as a correct spelling answer. — **Reversibility:** costly — many call sites share `fromNoteContent` today; changing the default to "all items including wiki links" would reintroduce blast-radius bugs.
  - Rationale: ROADMAP success criteria 2–4 and PROJECT "alias blast radius" require plain-alias consumers unchanged. Indexing `[[Other]]` or its inner title as an alias would corrupt search/accidental-match.

### Overlap extraction API (Phase 6-ready, unused for grading yet)
- **D-03:** Extend `FrontmatterAliases` with an explicit split API: plain-alias accessors keep today's `fromNoteContent` / `fromFrontmatter` / `matchesFromNoteContent` **plain-only** contract; add overlap accessors (e.g. `overlapWikiLinkTokensFromNoteContent` / equivalent) that return the raw wiki-link token strings (or a thin typed wrapper) for declared overlaps. Phase 5 does **not** wire `MemoryTrackerService` / `AnswerOutcome.OVERLAP` / `AnsweredQuestion.overlap`. — **Reversibility:** reversible — additive methods; grading stays Phase 6.
  - Rationale: Structure phase must leave a stable seam for Phase 6 without front-running OVL-01 behavior. Shared parse ownership prevents duplicated YAML/wiki rules.

### Authored validation & dead targets
- **D-04:** Authored validation accepts well-formed wiki-link alias items (D-01) alongside existing plain alias rules. Malformed items (bare `[[`, non-wiki strings with `#`/`|`/path separators outside a valid wiki token, nested YAML, blanks) still fail `authoredValidationErrorForNoteContent` with an updated message that mentions both plain aliases and wiki-link overlap declarations. **Dead / unresolvable targets are valid declarations** — do not require the target note to exist at save time; resolution is Phase 6's job. Soft-parse paths that feed the index continue to **skip** invalid items (today's lenient `from*` behavior) while authored validation remains strict. — **Reversibility:** reversible — validation policy; existence checks can be added later if product wants them.
  - Rationale: Overlap is user-declared intent; notes and titles move. Blocking saves on resolve would fight wiki-link authoring elsewhere. Malformed tokens must still be rejected so `[[` does not become a loophole for unsafe plain-alias characters.

### Claude's Discretion
- Exact method names / return types for the overlap extraction API (token strings vs `WikiInnerSplit` / `WikiLinkTargetReference` with focus notebook).
- Whether `fromNoteContent` stays the plain-only name or is renamed with a thin deprecated/compat wrapper — prefer least churn for existing callers (`NoteAliasIndexService`, `Note.matchAnswer`, cloze, tests).
- How to detect "this list item is a wiki-link alias" (whole-string match on `WikiLinkMarkdown.INNER_LINK_PATTERN` vs strip-and-parse) — prefer whole-item token, not wiki links embedded in longer plain text.
- Frontend parity (`frontend/src/utils/frontmatterAliases.ts` and any authored-alias UI validation) — update only if a user-facing path would reject valid wiki-link aliases or accept unsafe ones.
- Test placement: prefer extending `FrontmatterAliasesTest`, `NoteAliasIndexServiceTest`, wiki-resolve / search / cloze tests with capability-named cases; treat this as the ROADMAP design spike (enumerate consumers before parser change).
- No Flyway / schema change expected unless research proves `NoteAliasIndex` needs a type column (prefer **not** indexing overlap rows at all per D-02).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project / requirements / roadmap
- `.planning/PROJECT.md` — Overlap modeled by extending aliases to accept wiki links; overlap declared not auto-detected; Constraints "Alias blast radius"; Current State next = Phase 5.
- `.planning/REQUIREMENTS.md` — **OVL-02** (aliases accept wiki-link values), **OVL-03** (preserve wiki-resolve, search, cloze). **OVL-01** is Phase 6 — do not implement grading here.
- `.planning/ROADMAP.md` §"Phase 5: Alias-as-wiki-link overlap declaration" — goal, success criteria 1–4, and the **alias blast radius** warning (enumerate every consumer before changing the parser).

### Prior phase locks (carry forward — do not re-litigate)
- `.planning/phases/02-accidental-match-grading-penalty/02-CONTEXT.md` — D-06: when answer matches reviewed note, skip accidental-match search; overlap is declared (this phase), not auto-detected.
- `.planning/phases/01-extend-answer-outcome-api/` (SUMMARY) — `AnswerOutcome.OVERLAP` and `AnsweredQuestion.overlap` already exist on the contract; Phase 5 does not need to write them yet (Phase 6).

### Codebase maps / concerns
- `.planning/codebase/CONCERNS.md` — **Derived index coherence (wiki title / property / alias)** — `WikiTitleCacheService.refreshForNote` / alias index refresh must stay coherent; missed refresh sites recreate bugs.
- `.planning/codebase/ARCHITECTURE.md` — service/algorithm boundaries for content parsing vs indexing.
- `.planning/codebase/STACK.md` — Spring Boot / MySQL stack context for index tables.

### Source files (integration points — read before editing)
- `backend/src/main/java/com/odde/doughnut/algorithms/FrontmatterAliases.java` — today rejects `[[`/`]]`; primary change surface for D-01–D-04.
- `backend/src/main/java/com/odde/doughnut/algorithms/WikiLinkMarkdown.java` — `INNER_LINK_PATTERN`, `splitInner` (pipe display).
- `backend/src/main/java/com/odde/doughnut/algorithms/WikiLinkTargetReference.java` — qualified `Notebook:Title` parse.
- `backend/src/main/java/com/odde/doughnut/services/NoteAliasIndexService.java` — refresh must index **plain aliases only**.
- `backend/src/main/java/com/odde/doughnut/entities/NoteAliasIndex.java` + `NoteAliasIndexRepository.java` — derived alias index rows.
- `backend/src/main/java/com/odde/doughnut/services/search/NoteAliasSearchService.java` — search consumer of alias index.
- `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java` — wiki-resolve alias targets + accidental-match alias leg (must not treat overlap wiki-links as aliases).
- `backend/src/main/java/com/odde/doughnut/entities/Note.java` — `matchAnswer` + cloze `hideAliases(FrontmatterAliases.fromNoteContent(…))`.
- `backend/src/main/java/com/odde/doughnut/algorithms/ClozeReplacement.java` / cloze path — plain aliases only for masking.
- `backend/src/main/java/com/odde/doughnut/services/WikiTitleCacheService.java` — refresh boundary for derived indexes (CONCERNS).
- `backend/src/main/java/com/odde/doughnut/validators/AuthoredNoteContent.java` — authored validation seam using `FrontmatterAliases.authoredValidationError…`.
- `frontend/src/utils/frontmatterAliases.ts` — frontend NFKC/dedupe helpers; check for validation parity needs.
- `backend/src/test/java/com/odde/doughnut/algorithms/FrontmatterAliasesTest.java` — extend for wiki-link accept + plain segregation.
- Existing consumer tests: `NoteAliasIndexServiceTest`, `SearchControllerAliasTests`, `WikiLinkResolver*` / cloze tests, `WikiTitleCacheServiceTest`.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `FrontmatterAliases` — single owner of aliases list parsing/validation; extend rather than fork a second parser.
- `WikiLinkMarkdown.INNER_LINK_PATTERN` + `splitInner` — detect/parse wiki-link list items and separate target vs display.
- `WikiLinkTargetReference.forToken` — notebook/title resolution for Phase 6 (not required to call from grading in Phase 5).
- `NoteAliasIndexService.refreshForNote` — already rebuilds from `FrontmatterAliases.fromNoteContent`; stays plain-only after D-02.

### Established Patterns
- Authored validation is strict (`authoredValidationErrorForNoteContent`); soft parse for index/match skips invalid items.
- Alias lookup keys use `FrontmatterAliases.normalizedLookupKey` (NFKC + lower-case).
- Derived alias rows are deleted and rewritten on note refresh (pessimistic lock) — do not leave stale wiki-link strings in the index.
- Wiki-resolve alias targets query `NoteAliasIndex` by lookup key — segregation at index time is the strongest OVL-03 guarantee.

### Integration Points
- Parser/validation: `FrontmatterAliases` (+ `AuthoredNoteContent`).
- Index: `NoteAliasIndexService` via existing refresh call sites (`WikiTitleCacheService` / content write paths).
- Consumers to regression-test: wiki resolve, alias search, cloze masking, `Note.matchAnswer`, accidental-match alias leg (must ignore overlap wiki-link items).
- No OpenAPI / frontend recall UI changes expected in Phase 5.

</code_context>

<specifics>
## Specific Ideas

- Example authored frontmatter shape:
  ```yaml
  aliases:
    - color
    - "[[Other Note]]"
    - "[[Shared Notebook:Hue|display]]"
  ```
  → plain searchable/matchable alias: `color`; overlap declarations: the two wiki-link items (targets `Other Note` and `Hue` in notebook `Shared Notebook`).
- ROADMAP frames this phase as a **design spike**: enumerate every `aliases` consumer before changing the parser; expect longer wall-clock than neighboring phases.
- Phase 5 success is structural: wiki-link aliases authorable + consumers safe. Observable overlap grading belongs to Phase 6.

</specifics>

<deferred>
## Deferred Ideas

- **Overlap "try again, no credit"** — Phase 6 (OVL-01): wire grading when `correct=true` AND reviewed note declares overlap via alias-as-wiki-link; set `outcome=OVERLAP` / `AnsweredQuestion.overlap`; no SRS credit; no note mutation.
- **Save-time existence check for overlap wiki-link targets** — deferred unless product later wants stricter authoring UX (D-04 keeps dead targets valid).
- **Separate `overlaps:` frontmatter key** — rejected for v1; PROJECT locks extending `aliases`.
- **MCQ accidental-match / fuzzy matching / qualified Notebook:Title typing in answers** — v2, out of scope.

None of the above were folded into Phase 5; discussion stayed within OVL-02/OVL-03 declaration scope.

</deferred>

---

*Phase: 5-alias-as-wiki-link-overlap-declaration*
*Context gathered: 2026-07-24*
