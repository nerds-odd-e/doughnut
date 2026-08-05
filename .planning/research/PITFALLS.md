# Pitfalls Research

**Domain:** Accidental-match resolve dialog UX (Doughnut recall / spelling)
**Researched:** 2026-08-05
**Confidence:** HIGH (integration pitfalls verified against shipped v1.1 code + ADR 0003; dialog-navigation patterns MEDIUM via Vue Router ecosystem)

**Suggested v1.2 phase labels** (for mapping; roadmapper may rename):

| # | Phase topic |
|---|-------------|
| 1 | Compact accidental-match result (drop stacked NoteShows; Resolve CTA → dialog shell) |
| 2 | Build a link from the resolve dialog |
| 3 | Add as overlapped note (no try-again / no credit reclaim) |
| 4 | Clickable titles navigate away; reopen resolve on return |
| — | Readonly / permission gates (constraint across Phases 2–3, verified in E2E) |

## Critical Pitfalls

### Pitfall 1: Treating “remove stacked NoteShows” as “stop revealing matches”

**What goes wrong:**
Matched notes disappear entirely, or only an alert remains with no way to inspect *which* notes collided. Learners lose the v1.1 value of making confusion visible (AM-03). E2E/page objects that assert `matched-notes-section` + NoteShow titles fail for the wrong reason (missing reveal vs intentional compact chrome).

**Why it happens:**
v1.1 Phase 3 shipped reveal as stacked `NoteShow` bodies under `matched-notes-section`. The milestone goal is to replace that *layout*, not the reveal contract. Implementers delete the section and forget the compact substitute (dialog list / titles + breadcrumbs).

**How to avoid:**
- Replace stacked bodies with a compact, optional path: alert + “Resolve accidental match” → dialog listing each match (title + notebook path/breadcrumb, **no note body**).
- Keep reviewed-note `NoteShow` full-height focus; matched content lives only in the dialog.
- Rewrite assertions to the new selectors; do not delete accidental-match E2E coverage.

**Warning signs:**
- Accidental-match result shows only the red alert and the reviewed note.
- `accidental_match_reveal.feature` scenarios deleted or `@wip`’d without a replacement assert for match identity.
- Unit tests still look for `matched-notes-section` NoteShows after chrome change.

**Phase to address:**
Phase 1 (compact result + dialog shell)

---

### Pitfall 2: Making resolve mandatory or burying the CTA

**What goes wrong:**
Dialog auto-opens on every accidental match, or resolve is hidden behind overflow menus. Optional resolution becomes a blocking step; learners who only needed to see the confusion are slowed. Conversely, if the CTA is easy to miss, link/overlap actions become undiscoverable after stacked CTAs are removed.

**Why it happens:**
Existing “Link to this note” PopButtons were always visible under each match. Moving actions into a dialog invites either auto-open “wizard” UX or a weak single button.

**How to avoid:**
- CTA under the alert, always visible on ACCIDENTAL_MATCH when there is ≥1 match.
- Dialog opens only on explicit click; closing returns to the same result with no forced next step.
- Empty `matchedNotes` stays section-less (already true today).

**Warning signs:**
- Focus trap / modal on answer submit without a user click.
- No `data-testid` for the resolve CTA; E2E can only open actions via brittle text.

**Phase to address:**
Phase 1 (CTA + optional open); reinforce in Phase 2/3 UX copy

---

### Pitfall 3: Nested PopButton / dialog-in-dialog for “Build a link”

**What goes wrong:**
Resolve dialog hosts another `PopButton`/`MatchedNoteLinkOffer` portal. Focus traps fight, close handlers close the wrong layer, or the outer resolve dialog unmounts mid-link and abandons the relationship finalize step. User appears “done” but no property/relationship was written.

**Why it happens:**
v1.1 embeds `MatchedNoteLinkOffer` inside `PopButton` per match. Naively moving that tree *inside* a new resolve dialog stacks two modal systems (`AnsweredSpellingQuestion.vue` + `MatchedNoteLinkOffer.vue`).

**How to avoid:**
- Prefer one modal host: resolve dialog *is* the shell; reuse `LinkInsertionChoice` / `AddRelationshipFinalize` (or a thin wrapper) as **in-dialog steps**, not a second PopButton.
- Keep `navigate-on-success="false"` / stay-on-result behavior from AM-04.
- Close outer dialog only after successful link write (or keep it open listing remaining matches — pick one and test it).

**Warning signs:**
- Two `[role=dialog]` / daisy modals in the DOM at once.
- E2E “still on accidental match result” fails only when linking from the new dialog.
- `closeDialog` from the offer tears down the resolve list unexpectedly.

**Phase to address:**
Phase 2 (Build a link)

---

### Pitfall 4: Conflating “Add as overlapped note” with the OVERLAP try-again / reclaim path

**What goes wrong:**
After the user declares overlap from an **already graded** ACCIDENTAL_MATCH result, the UI switches to overlap try-again chrome, emits `retry`, or offers to undo/reclaim the accidental-match SRS penalty. That corrupts the session narrative and violates ADR 0003: ACCIDENTAL_MATCH already applied a light penalty; OVERLAP try-again is a *grading* outcome for a *future* answer that is correct-but-non-distinguishing.

**Why it happens:**
Product language (“overlap”) collides with `AnswerOutcome.OVERLAP`. Implementers reuse `isOverlap` UI (`overlap-try-again` button / warning alert) or invent a “reclaim credit” prompt because the user “fixed” the knowledge graph. Contestable / re-assimilation flows nearby make “undo grade” feel available.

**How to avoid:**
- **Declare overlap = note content mutation only** (wiki-link item in `aliases` frontmatter via the same seam as OVL-02).
- Do **not** change `answeredQuestion.answer.outcome`, do **not** emit `retry`, do **not** call contest/regrade/reclaim APIs.
- Stay on ACCIDENTAL_MATCH alert + result; optionally dismiss or mark that match as resolved in the dialog only.
- Assert in unit + E2E: after “Add as overlapped note”, no `overlap-try-again` / `overlap-try-again-alert`, memory tracker schedule unchanged from the accidental-match grade.

**Warning signs:**
- Shared handler between “Add as overlapped note” and `emit('retry')`.
- Tests that expect OVERLAP outcome after a content PATCH from the dialog.
- Copy that says “try again” or “get your credit back” on the resolve success path.

**Phase to address:**
Phase 3 (Add as overlapped note) — **highest-risk behavior pitfall for this milestone**

---

### Pitfall 5: Writing a plain alias instead of a wiki-link overlap declaration

**What goes wrong:**
“Add as overlapped note” appends a plain string alias (e.g. via `appendAliasToNoteContent`) instead of a well-formed wiki-link token. Future reviews never enter OVERLAP grading; accidental matches may worsen (shared plain alias). Authored validation may accept plain aliases while failing the product intent.

**Why it happens:**
`appendAliasToNoteContent` is the obvious existing helper (Wikidata flows) and only merges **plain** alias strings. Overlap declaration is a distinct frontmatter shape (`[[Note]]` / wiki-link items), parsed by `FrontmatterAliases.overlapWikiLinkTokensFrom*`.

**How to avoid:**
- Build the overlap item with the same wiki-link text rules used elsewhere (`buildWikiLinkText` / notebook-qualified when needed).
- Reuse or extend a frontmatter list merge that accepts wiki-link items; validate with `authoredAliasesValidation` / backend authored rules.
- Prefer a controller-level or content-update unit test that asserts `overlapWikiLinkTokensFromNoteContent` contains the target after the dialog action.

**Warning signs:**
- Saved frontmatter shows `- sedation` instead of `- "[[sedation]]"` (or equivalent wiki form).
- Next-day recall with the shared answer still grades ACCIDENTAL_MATCH, never OVERLAP.

**Phase to address:**
Phase 3 (Add as overlapped note)

---

### Pitfall 6: Dialog open state dies when a clickable title navigates away

**What goes wrong:**
User opens resolve, clicks a match title (`NoteTitleWithLink` → `noteShowLocation`), inspects the note, returns to the recall result — dialog is closed and there is no clear way to resume, or the whole answered-question view remounted without matches. Milestone requirement (“after navigating away via a title, user can return and reopen”) fails.

**Why it happens:**
Titles are real `router-link`s. Component-local `ref(dialogOpen)` resets on unmount. Vue Router modal ecosystem patterns show local modal state does not survive route leave without history.state / explicit reopen affordance. Doughnut already uses navigable titles inside NoteShow; stacking that inside a dialog amplifies the trap.

**How to avoid:**
- **Do not** require the dialog to stay mounted across navigation.
- Guarantee the Resolve CTA remains on the accidental-match result after return (browser back or in-app return to last answered question).
- Optional: remember “user had resolve open” in session/query only if product wants auto-reopen; minimum bar is **manual reopen** via the same CTA.
- E2E: open dialog → click title → leave → return to result → CTA works → dialog lists the same matches.

**Warning signs:**
- Resolve CTA missing after `goToLastAnsweredQuestion`.
- Tests only cover “dialog open” in a single mount without a route round-trip.
- Using `prevent` on title clicks “to keep the dialog open,” which blocks the inspect-matched-note goal.

**Phase to address:**
Phase 4 (navigate + reopen); CTA persistence also guarded in Phase 1

---

### Pitfall 7: Dropping readonly / unload gates when moving CTAs into the dialog

**What goes wrong:**
Readonly notebooks gain “Build a link” / “Add as overlapped note” and fail on write, or CTAs appear before realms load and no-op. v1.1 already omits link CTAs when `notebookRealm.readonly === true` or realms are unloaded (`canOfferLinkToMatched`).

**Why it happens:**
Dialog redesign reimplements the match list and forgets to port `canOfferLinkToMatched` (and equivalent write gate for overlap mutation). Title/breadcrumb stay visible (good); action buttons accidentally always render.

**How to avoid:**
- Port the same gates: no mutate CTAs when reviewed notebook is readonly; wait until reviewed + matched realms are loaded.
- Titles/breadcrumbs can remain visible for inspection even when actions are omitted.
- Keep the focused unit tests in `AnsweredSpellingQuestionAccidentalMatch.spec.ts` (“omits link CTAs when reviewed notebook is readonly”, “until realms are loaded”) — adapt selectors to dialog actions.

**Warning signs:**
- Readonly scenario shows action buttons.
- Flaky tests that click resolve actions before `flushPromises` / realm load.

**Phase to address:**
Phases 2–3 (mutate actions); regression assert in final E2E pass

---

### Pitfall 8: Breaking OVERLAP try-again by sharing accidental-match chrome

**What goes wrong:**
Refactor of `AnsweredSpellingQuestion.vue` couples ACCIDENTAL_MATCH and OVERLAP templates. Overlap results start showing resolve CTAs / matched sections, or accidental-match grows a try-again button. v1.1 explicitly hides matched notes on OVERLAP even if `matchedNotes` leak.

**Why it happens:**
One component owns both outcomes today (`isOverlap` vs `showMatchedNotesSection`). A careless `v-if` merge (“show resolve whenever matchedNotes.length”) ignores outcome.

**How to avoid:**
- Keep outcome-discriminated rendering: resolve UX **only** for `ACCIDENTAL_MATCH`; try-again **only** for `OVERLAP`.
- Preserve unit test: “hides matched-notes section even when matchedNotes leak on OVERLAP” → update to “hides resolve CTA / dialog entry on OVERLAP”.
- Do not delete `overlap_try_again.feature` coverage while changing the sibling accidental-match UI.

**Warning signs:**
- `overlap-try-again` and “Resolve accidental match” both present.
- Shared computed like `showMatchUi` without checking `answer.outcome`.

**Phase to address:**
Phase 1 (chrome split); verify again in Phase 3 so overlap declaration UI does not bleed into OVERLAP grading UI

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Keep stacked NoteShows behind a feature flag “just in case” | Easy rollback | Two reveal UIs; E2E/page objects diverge; milestone goal unmet | Never for mainline — delete stacked path once dialog reveal ships |
| Reuse `appendAliasToNoteContent` for overlap | Fast wire-up | Wrong frontmatter shape; overlap grading never triggers | Never — use wiki-link overlap write path |
| Auto-reopen dialog via global store without tests | Feels magical | Stale open state across unrelated recall prompts | Only if Phase 4 explicitly specs it + E2E |
| Duplicate link-offer markup instead of extracting steps | Ships Phase 2 faster | Nested modal bugs; drift from property/relationship rules | Avoid — extract/reuse `LinkInsertionChoice` steps |
| Soft-delete E2E asserts for matched NoteShows without replacements | Green CI quickly | Regress invisible match identity | Never — replace asserts in the same phase |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| `MatchedNoteLinkOffer` + new resolve dialog | Nest PopButton inside dialog | Single dialog host; offer as inner step |
| `AnswerOutcome` / ADR 0003 | Regrade ACCIDENTAL_MATCH → OVERLAP after declare | Mutate note only; leave graded answer intact |
| `FrontmatterAliases` / alias index | Plain alias append | Wiki-link overlap token + index refresh on content save |
| `NoteTitleWithLink` / router | Prevent default to keep dialog open | Allow navigate; ensure CTA reopen on return |
| `canOfferLinkToMatched` / readonly | Show actions always in dialog | Same readonly + realm-loaded gates |
| E2E `AnsweredQuestionPage` | Only update unit tests | Update page object (`expectAccidentalMatchReveal`, link helpers) with dialog selectors in the same phase as chrome change |
| Overlap try-again (`emit('retry')`) | Wire declare-overlap success to retry | Keep retry exclusive to OVERLAP outcome button |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Mounting full `NoteShow` per match inside the dialog “for convenience” | Slow accidental-match results; scroll jank | Titles + breadcrumbs only (milestone intent) | Multi-match answers (already tested with 2+ matches) |
| Loading every matched realm eagerly for breadcrumbs | Spinners / waterfalls on open | Load on dialog open; gate actions until ready (existing pattern) | Many matches across notebooks |
| Leaving stacked NoteShows *and* dialog NoteShows during migration | Double fetch of note realms | Remove stacked path in Phase 1 | Even 1–2 matches |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Offering mutate CTAs on readonly / unreadable matched notes | Failed writes or confusing errors; possible attempt to edit foreign content | Keep `canOfferLinkToMatched`-style gates; titles may show if topology already returned by grade API |
| Skipping auth on any new “declare overlap” endpoint | Public content mutation | Prefer existing authenticated content update path; no new unauthenticated API |
| Trusting client-only “resolved” flags for SRS | Client could fake reclaim | Never reclaim via client flag — SRS stays server-graded |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Auto-open resolve dialog | Interrupts flow after every accidental match | Optional CTA under alert |
| Title click does nothing | Cannot inspect matched note body | Keep navigable titles; support reopen |
| “Add as overlapped” then immediate try-again | Feels like punishment after fixing structure | Confirm note updated; stay on accidental-match result |
| Ambiguous “Resolve” without Build link vs Overlap | Wrong action; wrong frontmatter | Per-match explicit actions with distinct labels |
| Dialog lists note bodies again | Defeats full-height reviewed-note focus | Title + breadcrumb only |

## "Looks Done But Isn't" Checklist

- [ ] **Compact reveal:** Stacked matched `NoteShow`s gone **and** match identity still visible via resolve dialog list — verify unit + E2E.
- [ ] **Optional CTA:** Dialog does not open on submit alone — verify no auto-open.
- [ ] **Build a link:** Property + relationship from dialog still stay on recall result (AM-04) — verify E2E scenarios updated, not deleted.
- [ ] **Add as overlapped note:** Frontmatter has wiki-link overlap item; **no** try-again chrome; **no** SRS reclaim — verify unit + E2E + tracker schedule.
- [ ] **Navigate + reopen:** Title → note → return → Resolve CTA → same matches — verify E2E round-trip.
- [ ] **Readonly:** Mutate CTAs omitted; titles may remain — verify existing readonly unit test adapted.
- [ ] **OVERLAP unchanged:** `overlap_try_again.feature` still green; no resolve CTA on overlap results.
- [ ] **Page objects:** `AnsweredQuestionPage` match/link helpers updated to dialog selectors in the chrome phase.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Stacked NoteShows removed without dialog list | LOW | Restore reveal via dialog list; fix E2E asserts before next phase |
| Nested modal broke link write | MEDIUM | Flatten to single dialog host; re-run property + relationship E2E |
| Declare-overlap regrades / try-again | HIGH | Revert grade/UI coupling; keep content PATCH only; add regression tests before continuing |
| Plain alias written instead of wiki-link | MEDIUM | Fix writer; migrate any bad fixtures; assert overlap tokens in test |
| Dialog cannot reopen after navigate | LOW–MEDIUM | Ensure CTA always on result; add round-trip E2E; avoid title `preventDefault` hacks |
| Readonly CTAs regress | LOW | Re-port `canOfferLinkToMatched`; restore unit tests |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Reveal lost when removing stacked NoteShows | Phase 1 | Dialog lists match titles; E2E identity assert |
| Resolve mandatory / CTA buried | Phase 1 | Optional open; CTA `data-testid` present |
| Nested PopButton / focus fight | Phase 2 | Single dialog; property + relationship E2E stay on result |
| Overlap declare → try-again / reclaim | Phase 3 | No overlap chrome; schedule unchanged; content has wiki-link |
| Plain alias instead of wiki-link overlap | Phase 3 | `overlapWikiLinkTokensFrom*` / authored validation assert |
| Title navigate loses resolve forever | Phase 4 | Round-trip E2E reopen via CTA |
| Readonly / unload gates dropped | Phases 2–3 | Adapted readonly + unloaded-realm unit tests |
| OVERLAP chrome coupled to resolve | Phase 1 (+ check Phase 3) | Overlap unit leak test + `overlap_try_again.feature` |

## Sources

- Doughnut shipped UI: `frontend/src/components/recall/AnsweredSpellingQuestion.vue`, `MatchedNoteLinkOffer.vue`
- Unit contracts: `AnsweredSpellingQuestionAccidentalMatch.spec.ts`, `AnsweredSpellingQuestionOverlap.spec.ts`
- E2E: `e2e_test/features/recall/accidental_match_reveal.feature`, `overlap_try_again.feature`, `AnsweredQuestionPage.ts`
- ADR 0003 graded outcomes (accidental match vs overlap): `docs/adrs/0003-spaced-repetition-scheduling-policy.md`
- v1.1 requirements/roadmap archives: `.planning/milestones/v1.1-REQUIREMENTS.md`, `v1.1-ROADMAP.md`
- Milestone intent: `.planning/PROJECT.md` (v1.2 Accidental Match Resolve UX)
- Overlap frontmatter seam: `FrontmatterAliases.overlapWikiLinkTokensFrom*`; plain-alias helper `appendAliasToNoteContent` (wrong tool for overlap)
- Vue Router modal / navigation state (MEDIUM): Vue Router e2e modal + community modal-route discussions — prefer explicit CTA reopen over assuming local dialog state survives navigation
- SRS “don’t corrupt logged grades when relating cards” (MEDIUM): community SRS practice aligns with ADR 0003 separation — declare relatedness ≠ regrade

---
*Pitfalls research for: Accidental-match resolve dialog UX (Doughnut v1.2)*
*Researched: 2026-08-05*
