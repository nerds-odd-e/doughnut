# Phase 9: Build a link from resolve dialog - Research

**Researched:** 2026-08-05
**Domain:** Vue 3 recall accidental-match resolve dialog — single-Modal link offer step (frontend-only)
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Single-Modal link offer host
- **D-01:** Never nest a second `PopButton`/`Modal` around `MatchedNoteLinkOffer` inside the resolve dialog. Host Build a link as a **step swap** in the same Modal opened by **Resolve accidental match**. — **Reversibility:** costly — nested modals reintroduce Pitfall 3 (focus/close fights) already documented for this milestone
- **D-02:** Own the list ↔ offer step state in the resolve Modal content (prefer extending `AccidentalMatchResolveDialog` — or a thin sibling host under `recall/` — so `AnsweredSpellingQuestion` stays CTA + `PopButton` only). Pass `reviewedNoteId` from the answered recalled note into the dialog host. — **Reversibility:** reversible
- **D-03:** Per-row **Build a link** CTA on `AccidentalMatchResolveRow` (capability copy locked). Clicking it switches the Modal body to `MatchedNoteLinkOffer` with that row’s `matchedNoteId`. Do **not** add **Add as overlapped note** in this phase. — **Reversibility:** reversible

#### Stay-on-result and offer exit semantics
- **D-04:** Keep `MatchedNoteLinkOffer` stay-on-result semantics (`navigate-on-success=false`; property/relationship writes do not leave recall). After a successful link or offer go-back/`closeDialog`, **return to the match list inside the same resolve Modal** (do not force-dismiss the outer resolve dialog). User dismisses resolve via existing Modal closer only. — **Reversibility:** reversible
- **D-05:** Reuse `MatchedNoteLinkOffer` as-is for the offer pipeline (`LinkInsertionChoice` → property append or `AddRelationshipFinalize`). Prefer adapting the host/step wiring over rewriting the offer. Preserve existing `link-to-matched-note-*` (or equivalent capability) testids so E2E page objects stay callable. — **Reversibility:** reversible

#### Readonly / unload gates (AMR-07)
- **D-06:** Port v1.1-style gates: omit **Build a link** when the reviewed notebook is `readonly` **or** when reviewed and/or matched realms required for the offer are not yet loaded. Prefer **hide** (not disabled-looking) to match prior accidental-match link CTAs. Titles + path remain visible when topology is available. — **Reversibility:** reversible
- **D-07:** Hydrate reviewed + matched realms as needed for the gate and offer (existing `getNoteRealmRefAndLoadWhenNeeded`). Do not invent a new capability API or OpenAPI change for gating. — **Reversibility:** reversible

#### Test coverage for this phase
- **D-08:** Extend Vitest at the answered-spelling / resolve-dialog boundary: Build a link appears when realms are writable+loaded; omitted when reviewed notebook readonly or realms unloaded; opening offer is a step in the same Modal (no nested dialog); after link success user still on accidental-match result chrome. Prefer capability-named tests; reuse `makeMe` / existing accidental-match fixtures. — **Reversibility:** reversible
- **D-09:** Untag `@wip` on the two accidental-match link E2E scenarios once green. Prefer **page-object** updates (open Resolve → Build a link → existing link helpers) over rewriting Gherkin step text. Keep `overlap_try_again` uncoupled. — **Reversibility:** reversible

### Claude's Discretion
- Exact step-state shape (`ref`/`enum` for `list` vs `offer`) and whether go-back chrome lives in the dialog host or relies solely on `MatchedNoteLinkOffer`’s existing go-back.
- Whether reviewed-realm hydrate for AMR-07 lives on the dialog host vs each row (prefer one reviewed hydrate at host level to avoid N× duplicate loads).
- Visual density of the per-row Build a link button (DaisyUI size/classes consistent with resolve chrome).

### Deferred Ideas (OUT OF SCOPE)
- Overlap alias append util — Phase 10 (Structure)
- Add as overlapped note (no try-again / no reclaim) — Phase 11 (AMR-08, AMR-09); AMR-07 gate rules already cover that CTA when added
- Title navigate, reopen resolve, full E2E polish — Phase 12 (AMR-05)
- AMR-10..13 resolve polish and SEED-001 — v2 / parked seed

None — discussion stayed within phase scope (auto mode)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AMR-06 | From a resolve-dialog row, user can **Build a link** to that matched note using the existing property/relationship link offer and remains on the accidental-match result afterward | Single-Modal step swap to `MatchedNoteLinkOffer`; map offer `closeDialog` → back to list; keep `navigate-on-success=false`; E2E stay-on-result already encoded in `@wip` scenarios |
| AMR-07 | Build-a-link and Add-as-overlapped actions are unavailable when the reviewed notebook is readonly or required note data is not loaded | Port v1.1 `canOfferLinkToMatched` (currentUser + reviewed realm loaded + `notebookRealm.readonly !== true` + matched realm loaded); hide CTA; titles/path stay; Add-as-overlapped CTA itself deferred to Phase 11 |
</phase_requirements>

## Summary

Phase 9 restores **Build a link** after Phase 7 removed per-match `PopButton` + `MatchedNoteLinkOffer` from the stacked result. The only correct host is a **step swap inside the existing Resolve `PopButton` Modal**: list ↔ `MatchedNoteLinkOffer`. Nesting a second `PopButton`/`Modal` reopens Pitfall 3 and conflicts with native `<dialog showModal()>` (Doughnut’s `Modal.vue` calls `showModal()` on mount).

AMR-07 is a direct port of the pre–Phase 7 gate `canOfferLinkToMatched`: require logged-in user, loaded reviewed realm, `reviewedRealm.notebookRealm.readonly !== true`, and loaded matched realm. Prefer **hide** over disabled. Hydrate reviewed realm once at the dialog host; rows already hydrate match realms for path display.

**Primary recommendation:** Extend `AccidentalMatchResolveDialog` with `reviewedNoteId` + list/link step state; per-row gated **Build a link** (`data-testid="link-to-matched-note-{id}"`); mount `MatchedNoteLinkOffer` in the same Modal; map `@closeDialog` to reset step to list (never dismiss outer Modal); restore Vitest gate cases; update `AnsweredQuestionPage` then untag the two `@wip` link E2E scenarios.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Resolve CTA / outer Modal open | Browser / Client | — | Existing `AnsweredSpellingQuestion` + `PopButton` |
| List ↔ offer step state | Browser / Client | — | Dialog host owns UI state; no backend step API |
| Build a link CTA + gates | Browser / Client | API / Backend (authz already on write) | Readonly + realm load are client-visible; writes still go through existing content/relationship APIs |
| Property / relationship write | API / Backend | Browser / Client | Reuse `MatchedNoteLinkOffer` → `updateTextField` / `AddRelationshipFinalize` |
| Stay on accidental-match result | Browser / Client | — | `navigate-on-success=false` already on relationship path; property path emits `closeDialog` only |
| E2E / Vitest evidence | Browser / Client | — | Mount answered spelling boundary; Cypress page object |

## Project Constraints (from .cursor/rules/)

| Rule | Directive for this phase |
|------|--------------------------|
| `planning.mdc` | Behavior phase; one observable behavior (Build a link from dialog + stay + gates); stop-safe; ~5 min slice budget; after phase: Jidoka, post-change-refactor, update plan, commit+push |
| `unit-testing.mdc` | Drive `AnsweredSpellingQuestion` / resolve dialog boundary; data over mocks; focused assertions; concise `makeMe` |
| `frontend-testing.mdc` | `CURSOR_DEV=true nix develop -c pnpm frontend:test …`; Vitest browser; `data-testid`; `mockSdkService` for HTTP; avoid role queries in Vitest |
| `gsd-coexistence.mdc` | Local wrap-up required; Nix prefix for tooling; phase numbers only under `.planning/` |
| `general.mdc` | No phase numbers in product test/feature names; capability-named testids |
| E2E (planning) | Targeted Cypress for touched feature; untag `@wip` when green; CI skips `@wip` |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue | 3.5.40 | Step `ref` + `v-if` swap list/offer | Pinned in `frontend/package.json` [VERIFIED: frontend/package.json:79] — quote: `"vue": "3.5.40"` |
| vue-router | 5.2.0 | Unchanged title links; relationship must not navigate | Pinned [VERIFIED: frontend/package.json:80] — quote: `"vue-router": "5.2.0"` |
| Vitest | 4.1.10 | Unit tests | Pinned [VERIFIED: frontend/package.json:75] — quote: `"vitest": "4.1.10"` |
| In-repo `PopButton` → `Modal` | — | Single resolve Modal (`showModal`) | Existing host [VERIFIED: frontend/src/components/commons/Modal.vue:83-85] — quote: `dialogRef.value?.showModal()` |
| `MatchedNoteLinkOffer` | — | Property/relationship offer | Reuse as-is (D-05) |
| `@generated/doughnut-backend-api` | — | `NotebookRealm.readonly`, realms, content update | No OpenAPI change (D-07) |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| DaisyUI `daisy-btn` classes | in-repo | Per-row Build a link visual density | Match Resolve CTA: `daisy-btn daisy-btn-secondary daisy-btn-sm` [ASSUMED density OK — discretion] |
| `makeMe` / `answeredSpellingQuestionTestSupport` | in-repo | Fixtures + mount with `seedRealms` / `currentUser` | Gate + link Vitest |
| Cypress + `AnsweredQuestionPage` | in-repo | Untag `@wip` link scenarios | D-09 |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Step swap in one Modal | Nested `PopButton` around offer | **Forbidden** (D-01 / Pitfall 3); native modal stacking is hostile |
| New “build link” wizard | Rewrite offer | Waste; offer already stay-on-result |
| OpenAPI enrich for gates | Client realm hydrate | Locked out (D-07) |

**Installation:**

```bash
# No new packages. Zero npm installs for this phase.
```

**Version verification:** Vue / Vitest / vue-router read from `frontend/package.json` this session. No registry packages added → package-legitimacy check N/A.

## Package Legitimacy Audit

> No external packages are installed in this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | None — reuse in-repo stack only |

**Packages removed due to [SLOP] verdict:** none  
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
AnsweredSpellingQuestion
  └─ PopButton (Resolve accidental match) ── one Modal (showModal)
        └─ AccidentalMatchResolveDialog  [step: list | link(matchedId)]
              ├─ step=list:
              │     AccidentalMatchResolveRow × N
              │       title + path (Phase 8)
              │       [Build a link] ──if canOffer──► set step=link(id)
              │
              └─ step=link:
                    MatchedNoteLinkOffer(reviewedNoteId, matchedNoteId)
                      ├─ LinkInsertionChoice → property write → emit closeDialog
                      ├─ AddRelationshipFinalize navigate-on-success=false → emit closeDialog
                      └─ go-back → emit closeDialog
                    host maps closeDialog → step=list  (Modal stays open)
```

### Recommended Project Structure

```
frontend/src/components/recall/
├── AnsweredSpellingQuestion.vue       # MODIFY: pass reviewedNoteId into dialog
├── AccidentalMatchResolveDialog.vue   # MODIFY: step state + offer mount + reviewed hydrate
├── AccidentalMatchResolveRow.vue      # MODIFY: gated Build a link CTA; emit select
└── MatchedNoteLinkOffer.vue           # REUSE as-is

frontend/tests/components/recall/
├── AnsweredSpellingQuestionAccidentalMatch.spec.ts  # EXTEND: gates + step + stay
└── MatchedNoteLinkOffer.spec.ts                     # KEEP (offer pipeline already covered)

e2e_test/start/pageObjects/AnsweredQuestionPage.ts   # MODIFY: Resolve → Build a link
e2e_test/features/recall/accidental_match_reveal.feature  # Untag @wip on two link scenarios
```

### Pattern 1: Single-Modal step state (not nested PopButton)

**What:** Local step in `AccidentalMatchResolveDialog`; `v-if` list vs offer.  
**When to use:** Always for Build a link (D-01, D-02).  
**Recommended shape (discretion):**

```typescript
// Source: .planning/research/ARCHITECTURE.md Pattern 1 + Vue v-if swap
type ResolveStep =
  | { kind: "list" }
  | { kind: "link"; matchedNoteId: number }

const step = ref<ResolveStep>({ kind: "list" })

function openLinkOffer(matchedNoteId: number) {
  step.value = { kind: "link", matchedNoteId }
}

function returnToList() {
  step.value = { kind: "list" }
}
```

```vue
<!-- sketch — host owns step; offer closeDialog → returnToList -->
<ul v-if="step.kind === 'list'" data-testid="accidental-match-resolve-dialog">
  <AccidentalMatchResolveRow
    v-for="matched in matchedNotes"
    :key="matched.id"
    :matched="matched"
    :can-build-link="canOfferBuildLink(matched.id)"
    @build-link="openLinkOffer(matched.id)"
  />
</ul>
<MatchedNoteLinkOffer
  v-else
  :reviewed-note-id="reviewedNoteId"
  :matched-note-id="step.matchedNoteId"
  @close-dialog="returnToList"
/>
```

### Pattern 2: Port `canOfferLinkToMatched` gates

**What:** Pre–Phase 7 gate (restored for Build a link). Verbatim historical logic [VERIFIED: git show 375a5d2589^ AnsweredSpellingQuestion.vue — recovered this session]:

```typescript
function canOfferLinkToMatched(matchedNoteId: number): boolean {
  if (!currentUser?.value || !reviewedRealm.value) return false
  if (reviewedRealm.value.notebookRealm.readonly === true) return false
  const matchedRealm = storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(matchedNoteId).value
  return !!matchedRealm
}
```

**Wire type for readonly** [VERIFIED: packages/generated/doughnut-backend-api/types.gen.ts:243-246]:

```typescript
export type NotebookRealm = {
    notebook: Notebook;
    hasAttachedBook?: boolean;
    readonly?: boolean;
```

**When to use:** Per-row Build a link visibility (AMR-07 / D-06). Prefer host-level reviewed hydrate + `currentUser` inject; rows keep match hydrate.

### Pattern 3: `closeDialog` means “back to list,” not Modal dismiss

**What:** `MatchedNoteLinkOffer` emits `closeDialog` on go-back, property success (`closeDialogThen`), and relationship success. Host must **not** pass PopButton `closer` into that emit for Phase 9 stay-in-dialog (D-04). Outer dismiss remains Modal X / backdrop / ESC only.

**Offer contract** [VERIFIED: frontend/src/components/recall/MatchedNoteLinkOffer.vue:1-18]:

```vue
  <LinkInsertionChoice
    …
    @go-back="$emit('closeDialog')"
  />
  <AddRelationshipFinalize
    …
    :navigate-on-success="false"
    @success="$emit('closeDialog')"
```

### Anti-Patterns to Avoid

- **Nested `PopButton` around `MatchedNoteLinkOffer`:** Pitfall 3; may fight `showModal` / focus / close. [CITED: medium.com/@beiselanja — only one modal dialog at a time]
- **Wiring offer `@closeDialog` to PopButton `closer`:** Forces dismiss of resolve list; violates D-04.
- **Disabled-looking Build a link on readonly:** Prefer omit (D-06).
- **Add as overlapped note CTA:** Phase 11 only (D-03).
- **Changing ACCIDENTAL_MATCH / OVERLAP grading or SRS:** Out of scope; UI-only link write.
- **Asserting `matched-notes-section` in E2E stay-on-result:** Removed in Phase 7; page object still references it — must update.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Property/relationship link UI | New wizard | `MatchedNoteLinkOffer` | Already stay-on-result + tested |
| Modal stack | Headless UI / Daisy modal-box | `PopButton`/`Modal` | One modal stack in app |
| Readonly / unload gate | New capability API | Port `canOfferLinkToMatched` + `getNoteRealmRefAndLoadWhenNeeded` | D-07 |
| Nested focus trap | Custom trap | Step swap in one `<dialog>` | Avoid dual showModal |

**Key insight:** The offer already works; the phase is **host wiring + gates + test surface restore**, not a new link pipeline.

## Common Pitfalls

### Pitfall 1: Nested PopButton / dual Modal (Pitfall 3)

**What goes wrong:** Second `showModal` / focus/close fights; offer abandons mid-finalize.  
**Why:** v1.1 embedded offer in its own `PopButton`; naive move nests it.  
**How to avoid:** Step swap only (D-01). Assert a single resolve dialog / no second link PopButton.  
**Warning signs:** Two modals; E2E stay-on-result fails only from dialog path.

### Pitfall 2: `closeDialog` dismisses resolve Modal

**What goes wrong:** After link or go-back, user loses match list.  
**Why:** Old pattern was `@close-dialog="closer"`.  
**How to avoid:** Host `returnToList` only (D-04).  
**Warning signs:** Resolve dialog gone after property click; Vitest can’t find list after offer success.

### Pitfall 3: Dropping readonly / unload gates (Pitfall 7)

**What goes wrong:** Readonly notebooks show Build a link; CTAs flash before realms load.  
**Why:** Gate lived on removed stacked section.  
**How to avoid:** Port gate; restore Vitest cases from pre–Phase 7 (writable / readonly / never-settling `showNote`).  
**Warning signs:** Readonly fixture still finds `link-to-matched-note-*`.

### Pitfall 4: Stale E2E page object (`matched-notes-section`)

**What goes wrong:** `@wip` scenarios stay red after product works.  
**Why:** `openLinkToMatchedNote` and `expectStillOnAccidentalMatchResult` still target stacked section [VERIFIED: e2e_test/start/pageObjects/AnsweredQuestionPage.ts:102-146].  
**How to avoid:** Page-object path: click Resolve → `link-to-matched-note-*` (label **Build a link**) → existing Link to: helpers; stay-on-result asserts alert + Resolve CTA (and optionally reopen dialog for match title), **not** `matched-notes-section`.  
**Warning signs:** Cypress looks for `matched-notes-section` after Phase 7.

### Pitfall 5: Coupling OVERLAP try-again

**What goes wrong:** Resolve / link chrome appears on OVERLAP.  
**How to avoid:** Keep Resolve gated to `ACCIDENTAL_MATCH`; leave `overlap_try_again` alone (D-09).

## Code Examples

### Pass reviewed note id into dialog

```vue
<!-- AnsweredSpellingQuestion — extend existing PopButton body -->
<AccidentalMatchResolveDialog
  :matched-notes="answeredQuestion.matchedNotes ?? []"
  :reviewed-note-id="answeredQuestion.recalledNote.noteTopology.id"
/>
```

Current host (no reviewed id yet) [VERIFIED: frontend/src/components/recall/AnsweredSpellingQuestion.vue:9-18]:

```vue
  <PopButton
    v-if="showResolveAccidentalMatchCta"
    title="Resolve accidental match"
    …
  >
    <AccidentalMatchResolveDialog
      :matched-notes="answeredQuestion.matchedNotes ?? []"
    />
  </PopButton>
```

### Per-row Build a link CTA (testid preserved)

```vue
<button
  v-if="canBuildLink"
  type="button"
  class="daisy-btn daisy-btn-secondary daisy-btn-sm"
  :data-testid="`link-to-matched-note-${matched.id}`"
  title="Build a link"
  aria-label="Build a link"
  @click="$emit('buildLink')"
>
  Build a link
</button>
```

Locked copy: **Build a link** (FEATURES / D-03). Testid family `link-to-matched-note-*` (D-05).

### Vitest gate restoration (adapt selectors)

Pre–Phase 7 cases to port into resolve-dialog flow (open Resolve first; query `document.body`):

1. Writable + seeded realms → two `link-to-matched-note-*`
2. `reviewedRealm.notebookRealm.readonly = true` → zero CTAs
3. Never-settling `showNote` + no `seedRealms` → zero CTAs
4. Click Build a link → body contains `Link to:` / matched title; still one Modal; no nested link PopButton
5. After property success (mock `updateNoteContent`) → resolve list visible again; accidental-match alert still on wrapper

Mount helpers already support `currentUser` + `seedRealms` [VERIFIED: frontend/tests/components/recall/answeredSpellingQuestionTestSupport.ts:29-61].

### E2E page-object sketch

```typescript
openLinkToMatchedNote(matchedNoteTitle: string) {
  cy.findByTestId('resolve-accidental-match').click()
  waitUntilAppIsNotBusy()
  cy.findByTestId('accidental-match-resolve-dialog')
    .should('contain.text', matchedNoteTitle)
  cy.findByTestId(/^link-to-matched-note-/) // or resolve note id if known
    .should('be.visible')
    .and('contain.text', 'Build a link')
    .click()
  cy.contains('Link to:')
    .should('be.visible')
    .parent()
    .should('contain.text', matchedNoteTitle)
  …
}
```

Keep Gherkin step text; change page object only (D-09).

## State of the Art

| Old Approach (v1.1 / Phase 7 interim) | Current Approach (Phase 9) | When Changed | Impact |
|---------------------------------------|----------------------------|--------------|--------|
| Per-match `PopButton` “Link to this note” under stacked `NoteShow` | In-dialog **Build a link** step | Phase 7 removed; Phase 9 restores | Compact reviewed focus + link restored |
| Nested Modal for offer | Single Modal step swap | Locked D-01 | Avoids Pitfall 3 |
| Gate on result surface | Same gate inside dialog rows | Phase 9 | AMR-07 |

**Deprecated/outdated:**

- `matched-notes-section` / stacked link CTAs on result surface — gone; do not revive for E2E stay asserts.
- CTA copy **Link to this note** — product verb is **Build a link**; keep testid prefix for stability.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Per-row button classes `daisy-btn daisy-btn-secondary daisy-btn-sm` match design intent | Discretion / UI | Visual inconsistency — easy CSS tweak |
| A2 | Community claim that only one `showModal` dialog may be open applies strictly in all Doughnut browsers | Pitfall 1 | If stacking “works,” still forbidden by D-01 |
| A3 | `currentUser` inject remains part of AMR-07 gate (as in v1.1) | Pattern 2 | If product wants anonymous read-only CTA hide via readonly alone, drop user check — confirm only if tests fail without it |

**If this table is empty:** — not empty; A1–A3 need planner awareness only (A1 discretionary; A2 reinforces locked decision; A3 port from v1.1).

## Open Questions (RESOLVED)

1. **Should go-back leave an extra host chrome button?**
   - What we know: `LinkInsertionChoice` already emits `goBack` → offer `closeDialog`.
   - What's unclear: Whether list needs a visible “Back” when on offer step beyond offer’s Reply button.
   - Recommendation: Rely solely on offer go-back (discretion default); no duplicate chrome.
   - **RESOLVED:** Rely solely on offer go-back; no duplicate host Back chrome. Encoded in 09-01-PLAN / 09-UI-SPEC.

2. **E2E stay-on-result: must dialog stay open after link?**
   - What we know: D-04 returns to match **list inside same Modal**; product E2E says “still on the accidental match **result**” (alert + recall URL).
   - What's unclear: Whether Cypress should assert dialog still open after link.
   - Recommendation: Minimum = still on result chrome; prefer also assert resolve dialog/list still open after link success to lock D-04.
   - **RESOLVED:** Prefer assert resolve dialog/list still available after link success (locks D-04); minimum stay-on-result chrome still required. Encoded in 09-02-PLAN.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node | Vitest / tooling | ✓ | v24.5.0 | — |
| pnpm | Scripts | ✓ | 11.20.0 | — |
| Nix (`CURSOR_DEV=true nix develop -c …`) | Canonical test cmds | assumed ✓ | — | Cloud VM skill if missing |
| `pnpm sut` (app + MySQL) | Cypress E2E | assumed running | — | Do not restart per agent-map |
| Chromium (Vitest browser / Playwright) | Unit + E2E | via frontend pretest | — | `playwright install chromium` |

**Missing dependencies with no fallback:** none identified for this phase  
**Step 2.6:** External tools are existing repo toolchain only (no new services).

## Validation Architecture

> `workflow.nyquist_validation` is enabled (`true` in `.planning/config.json`).

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Vitest 4.1.10 (browser/chromium) + Cypress E2E |
| Config file | `frontend` Vitest config (package scripts) |
| Quick run command | `CURSOR_DEV=true nix develop -c pnpm -C frontend test tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` |
| Full suite command (targeted) | Same file + `MatchedNoteLinkOffer.spec.ts`; E2E: Cypress `--spec` for `accidental_match_reveal.feature` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AMR-06 | Build a link from resolve row; stay on result | unit | Vitest accidental-match spec (open Resolve → Build a link → offer; after success still alert / list) | ✅ extend existing |
| AMR-06 | Property + relationship link stay on result | e2e | Cypress `accidental_match_reveal.feature` (untag `@wip`) | ✅ scenarios exist `@wip` |
| AMR-07 | Hide Build a link when readonly | unit | Vitest readonly seed case | ❌ Wave 0 — restore removed case |
| AMR-07 | Hide until realms loaded | unit | Vitest never-settling `showNote` | ❌ Wave 0 — restore removed case |
| AMR-07 | Show when writable+loaded | unit | Vitest two CTAs | ❌ Wave 0 — restore adapted case |

### Sampling Rate

- **Per task commit:** Vitest `AnsweredSpellingQuestionAccidentalMatch.spec.ts`
- **Per wave merge:** + `MatchedNoteLinkOffer.spec.ts` if offer touched; Cypress targeted feature when untagging `@wip`
- **Phase gate:** Vitest green for accidental-match + offer; both link E2E scenarios green without `@wip`

### Wave 0 Gaps

- [ ] Restore/adapt Vitest cases: writable CTAs, readonly omit, unloaded realms omit, step-in-same-Modal, stay-on-result after link — in `AnsweredSpellingQuestionAccidentalMatch.spec.ts`
- [ ] Update `AnsweredQuestionPage.openLinkToMatchedNote` / `expectStillOnAccidentalMatchResult` before untagging `@wip`
- [ ] Untag `@wip` on wiki-property and relationship scenarios in `accidental_match_reveal.feature` only after green

*(Framework install: none — infrastructure exists.)*

## Security Domain

> `security_enforcement` enabled in config.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes (indirect) | Existing session; gate requires `currentUser` like v1.1 |
| V3 Session Management | no new surface | — |
| V4 Access Control | yes | Hide mutate CTA when `notebookRealm.readonly === true`; backend still enforces write authz on content/relationship APIs |
| V5 Input Validation | yes (existing) | Offer reuses existing content update / relationship finalize validation |
| V6 Cryptography | no | — |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Attempt write on readonly / foreign notebook | Elevation of Privilege | Client hide CTA + server-side notebook authz on `updateNoteContent` / create relationship |
| Clickjacking / focus steal via nested modal | Tampering / Spoofing | Single Modal step swap; no nested dialog |
| XSS via matched title in dialog | Tampering | Existing title components (no `v-html` for titles in Phase 7–8 list) |

## Sources

### Primary (HIGH confidence)

- In-repo components read this session: `AnsweredSpellingQuestion.vue`, `AccidentalMatchResolveDialog.vue`, `AccidentalMatchResolveRow.vue`, `MatchedNoteLinkOffer.vue`, `PopButton.vue`, `Modal.vue`
- Pre–Phase 7 gate + Vitest cases recovered via `git show 375a5d2589^` / `2ce5b39380`
- `.planning/research/{ARCHITECTURE,PITFALLS,STACK,FEATURES,SUMMARY}.md`
- `09-CONTEXT.md` locked decisions
- `packages/generated/doughnut-backend-api/types.gen.ts` `NotebookRealm.readonly`
- `e2e_test/.../AnsweredQuestionPage.ts` + `accidental_match_reveal.feature`

### Secondary (MEDIUM confidence)

- Context7 Vue (`/vuejs/core`) — `v-if` / ref-driven view swap (generic; applied to step host)
- classify-confidence context7 --verified → MEDIUM

### Tertiary (LOW confidence)

- WebSearch on nested `showModal` / focus — reinforces D-01; not required beyond locked decision
- classify-confidence websearch → LOW

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH — zero new libs; pins and components verified in-repo
- Architecture: HIGH — step host + gate port are concrete and locked
- Pitfalls: HIGH — milestone Pitfalls 3/7 + verified stale E2E page object

**Research date:** 2026-08-05  
**Valid until:** 2026-09-04 (30 days; stable in-repo UI wiring)
