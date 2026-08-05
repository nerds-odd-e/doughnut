# Feature Research

**Domain:** Accidental-match / same-title conflict resolve UX in PKM + spaced-repetition (Doughnut v1.2 resolve dialog)
**Researched:** 2026-08-05
**Confidence:** MEDIUM

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist once an accidental-match (or “same title elsewhere”) is surfaced. Missing these = resolve UX feels incomplete or hostile to review flow.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Clear conflict signal kept on result | Learners already saw the accidental-match alert in v1.1; the signal must remain when stacked notes go away | LOW | Reuse `accidental-match-alert`; do not demote the outcome to a quiet badge |
| Single CTA to open resolve | PKM users expect “do something about this collision” from the notice (Obsidian forums repeatedly ask for jump/merge from the duplicate-name notice) | LOW | Locked copy: **Resolve accidental match** under the alert |
| Match list with identity, not full notes | Same-name notes are disambiguated by **path** in Obsidian (shortest path when ambiguous); stacked full `NoteShow`s are not the norm for conflict lists | MEDIUM | Clickable **title** + **notebook breadcrumb/path**; no body peek (locked) |
| Per-match discrete actions | Conflict dialogs work when each row has clear verbs; “notice only” is the PKM gap users complain about | MEDIUM | Locked: **Build a link** \| **Add as overlapped note** |
| Optional resolve / dismiss anytime | RemNote/Anki treat interference fixes as optional edits; forcing fix mid-queue is anti-pattern | LOW | Dismiss dialog; continue reviewing; no gate before next card |
| Navigate to matched note by title | Jump-to-other is the #1 ask on Obsidian duplicate-name notices | LOW–MEDIUM | Title click may leave result; **reopen dialog on return** is locked |
| Reviewed note stays primary focus | Review UI table stake: answer result + reviewed note full height | MEDIUM (structure) | Requires removing stacked matched `NoteShow`s (locked) |
| Stay-on-page after link build | v1.1 already promised this (AM-04 + E2E); users will regress if link flow kicks them out | LOW | Reuse `MatchedNoteLinkOffer` / relationship `navigate-on-success=false` |
| Readonly / capability gating for mutations | Existing link offer already hides when reviewed notebook is readonly | LOW | Hide or disable Build/Overlap when user cannot mutate reviewed note |

### Differentiators (Competitive Advantage)

Features that set Doughnut apart. RemNote/Anki do **not** auto-detect “your typed answer is another card’s title”; Obsidian/Logseq collisions are create/rename/index-time, not recall-time.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Recall-time accidental-match → graph/SRS fix | Turns a graded interference into an optional knowledge-graph action without leaving the learning loop | MEDIUM | Builds on shipped `ACCIDENTAL_MATCH` + `matchedNotes` |
| Dual resolve verbs: link **or** declare overlap | Competitors offer merge/disable/clarify; Doughnut’s overlap wiki-link aliases are unique and feed the existing OVERLAP try-again loop **on future reviews** | MEDIUM–HIGH | “Add as overlapped note” writes overlap alias; must **not** trigger try-again or reclaim SRS credit **on this result** (locked) |
| Compact resolve dialog vs stacked notes | Preserves full-height reviewed note — competitors either show nothing useful or dump full pages | MEDIUM | Core milestone UX bet |
| Lighter SRS penalty already applied; resolve is non-grading | Fixing graph structure is separate from the grade — avoids RemNote-style “edit card then re-rate” confusion | LOW | v1.1 AM-02 already graded; dialog must not re-open grading |
| Reopen resolve after title navigation | Optional workflow without losing the match list context | MEDIUM | Needs stable result state / reopen affordance when user returns to the accidental-match result |
| Multi-match list in one dialog | One answer can collide with several readable notes; batch visibility beats serial stacked sections | LOW–MEDIUM | Depends on existing multi-`matchedNotes` payload |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good in PKM/SRS discourse but fight this milestone’s decisions or create rewrite risk.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Stacked full matched-note bodies on the result | “Show me both notes so I can compare” (v1.1 AM-03) | Steals height from the reviewed note; duplicate of NoteShow; locked out for v1.2 | Title + path in dialog; title navigates for deep read |
| Content peek / preview pane in dialog | Familiar from merge UIs and search | Spoils focus, balloons dialog, invites “which body is right?” merge thinking | Identity-only rows; navigate for content |
| Force resolve before continuing recall | Feels “complete” / Notebook Health–like | Violates optional-resolve lock; RemNote explicitly allows ignoring leeches | Soft CTA; dismiss OK |
| Auto-merge notes on title collision | Obsidian/Logseq community favorite for duplicates | Destructive; wrong for *similar but distinct* vocabulary (sedition/sedation); not Doughnut’s model | Explicit **Build a link** or **Add as overlapped note** |
| Coupling “Add as overlapped” → try-again / credit reclaim | Symmetry with OVERLAP result UX | Locked **out**: declaring overlap is the action; this answer already graded ACCIDENTAL_MATCH | Persist overlap only; leave current result as-is |
| Auto-create “distinguish these two” card | RemNote interference advice | Speculative content creation mid-review; out of scope | User may later author notes; not dialog auto-action |
| Require resolving every match before dismiss | Completeness | Multi-match + optional resolve conflict; partial resolve is valuable | Per-row actions; dismiss with remaining unresolved OK |
| Inline rename / delete competing note from dialog | Logseq “nuke one file” request | Dangerous in shared/readonly notebooks; wrong severity for recall confusion | Navigate + existing note tools outside dialog |
| LLM / fuzzy “you meant X” suggestions | Seed / v2 spelling follow-ons | Out of scope; SEED-001; mechanical title/alias only today | Exact matches only (v1.1) |
| Change ACCIDENTAL_MATCH SRS math in this milestone | “While we’re here…” | Scope creep; grading already shipped | Keep AM-02 behavior |

## Feature Dependencies

```
ACCIDENTAL_MATCH grading + matchedNotes (v1.1)
    └──requires──> Accidental-match alert on result
                       └──requires──> "Resolve accidental match" CTA
                                          └──requires──> Resolve dialog shell (open/dismiss/reopen)
                                                 └──requires──> Match row: title + notebook path
                                                        ├──requires──> Title navigation (existing note route)
                                                        ├──requires──> Build a link → MatchedNoteLinkOffer (v1.1)
                                                        │                  └──requires──> LinkInsertionChoice / AddRelationshipFinalize
                                                        └──requires──> Add as overlapped note
                                                                           └──requires──> aliases overlap wiki-link write (v1.1 OVL-02)
                                                                                  └──enhances──> Future OVERLAP try-again (OVL-01)
                                                                                  └──conflicts──> Triggering try-again / credit reclaim on this result

Remove stacked matched NoteShows
    └──requires──> CTA + dialog (else AM reveal regresses to alert-only)

OVERLAP try-again button on OVERLAP outcome
    └──conflicts──> Showing try-again after dialog "Add as overlapped note" on ACCIDENTAL_MATCH result
```

### Dependency Notes

- **Dialog requires v1.1 `matchedNotes` / outcome:** No new detection; UX re-homes existing payload.
- **Build a link requires `MatchedNoteLinkOffer`:** Prefer reuse over a second link wizard; keep stay-on-page semantics.
- **Add as overlapped requires frontmatter overlap wiki-link write path:** Same mechanism as OVL-02; new UI entry point only. Must **not** emit `retry` or alter this answer’s SRS credit.
- **Remove stacked notes conflicts with “alert-only”:** Structure phase (strip stacks) is only stop-safe if Behavior (CTA→dialog) lands in the same milestone before shipping users an empty reveal.
- **Title nav + reopen:** Soft dependency on result remount/state — if answered result unmounts on leave, reopen needs persisted match list (from API/session) when user returns to that recall result.

## MVP Definition

### Launch With (v1.2)

Minimum for the locked milestone goal: compact optional resolve without stacked notes.

- [ ] Remove stacked matched-note `NoteShow`s from accidental-match result — restores full-height reviewed note
- [ ] CTA **Resolve accidental match** under alert — opens dialog
- [ ] Dialog lists each match: clickable title + notebook path/breadcrumb only
- [ ] Per match: **Build a link** (reuse existing offer) and **Add as overlapped note** (overlap alias write, no try-again / no credit reclaim)
- [ ] Dismiss anytime; title navigation allowed; reopen dialog when back on the accidental-match result
- [ ] Update E2E (`accidental_match_reveal.feature` + page objects) to dialog flows; keep stay-on-page link assertions

### Add After Validation (v1.x)

Features to add once core dialog is working.

- [ ] Per-row “already linked / already overlapped” quiet state — when reopening after partial resolve
- [ ] Keyboard: Esc dismiss, Enter on focused row’s primary action — power-user polish
- [ ] Multi-match progress cue (“2 of 3 unresolved”) — only if users leave many open
- [ ] Readonly explanatory empty state when no mutation actions available — clarity without fake buttons

### Future Consideration (v2+)

Features to defer until product-market fit / later seeds.

- [ ] MCQ / fuzzy / `Notebook:Title` accidental-match (SEED-001)
- [ ] Auto-suggest distinguish-card templates
- [ ] Merge-notes action
- [ ] Health-lint rule: “unresolved accidental matches” backlog outside recall
- [ ] Batch “mark all as overlapped”

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Remove stacked matched notes | HIGH | LOW | P1 |
| Resolve CTA + dialog shell | HIGH | LOW–MEDIUM | P1 |
| Match rows: title + path | HIGH | MEDIUM | P1 |
| Build a link (reuse offer) | HIGH | LOW | P1 |
| Add as overlapped note (no try-again/credit) | HIGH | MEDIUM | P1 |
| Optional dismiss | HIGH | LOW | P1 |
| Title nav + reopen dialog | HIGH | MEDIUM | P1 |
| E2E rewrite for dialog | HIGH | MEDIUM | P1 |
| Already-resolved row state | MEDIUM | MEDIUM | P2 |
| Keyboard / a11y polish | MEDIUM | LOW | P2 |
| Distinguish-card / merge / Health backlog | LOW–MEDIUM | HIGH | P3 |
| Fuzzy / MCQ match detection | MEDIUM | HIGH | P3 (SEED-001) |

**Priority key:**
- P1: Must have for v1.2 launch
- P2: Should have once dialog is green
- P3: Nice to have / other milestone

## Competitor Feature Analysis

| Feature | Obsidian / Logseq (PKM) | RemNote / Anki (SRS) | Doughnut v1.2 approach |
|---------|-------------------------|----------------------|------------------------|
| Detect same title / interference | Create/rename/index: “name already exists”; ambiguous wiki links get path disambiguation | No auto “answer matched another card”; RemNote leech tips; Anki sibling bury only | Already detects title/alias hit at spelling grade (`ACCIDENTAL_MATCH`) |
| Surface collision in context | Weak notice; users ask for jump/merge on the notice | Out-of-queue edit advice | Alert + **Resolve** CTA on recall result |
| Identify which other note | Path in links / file path in errors | Manual search | Dialog: title + notebook breadcrumb |
| Content comparison inline | Rarely; merge is separate composer/flow | N/A (edit card) | **No** content peek (locked) |
| Primary fix actions | Merge / open existing / delete duplicate (manual) | Disable, reword, distinguish card, suspend | **Build a link** or **Add as overlapped note** |
| Forced vs optional | Notices don’t block typing forever | Fixes optional | **Optional** dismiss (locked) |
| Effect on scheduling | N/A | Edit doesn’t auto re-grade past review | Overlap add does **not** try-again or reclaim credit (locked); future reviews may hit OVERLAP |

## Expected behavior (Doughnut resolve dialog)

Synthesized from ecosystem norms + locked product decisions:

1. User gets `ACCIDENTAL_MATCH` → alert remains; reviewed note stays the main surface; **no** matched-note stack.
2. Optional CTA opens a modal listing each `matchedNotes` entry (title + notebook path).
3. User may dismiss, ignore, or continue the recall session with zero graph changes.
4. Title click opens that note; returning to the result still offers Resolve (dialog reopen).
5. **Build a link** runs the existing property/relationship offer with matched note pre-selected; stay on result.
6. **Add as overlapped note** declares overlap via aliases wiki-link; dialog/result stay on ACCIDENTAL_MATCH presentation — **no** overlap try-again button, **no** SRS credit reclaim for this answer.
7. OVERLAP try-again remains solely for answers graded `OVERLAP` (already-declared overlap), not for freshly declaring overlap from this dialog.

## Sources

- Doughnut product: `.planning/PROJECT.md`, `.planning/MILESTONES.md`, v1.1 requirements archive; `AnsweredSpellingQuestion.vue`; `MatchedNoteLinkOffer.vue`; `accidental_match_reveal.feature`
- Obsidian: duplicate-name notice / jump-or-merge feature requests; official ambiguous-link path disambiguation (Files & Links → New link format) — [confidence: MEDIUM]
- Logseq: “Page already exists with another file” / merge-on-rename discussions — [confidence: MEDIUM]
- RemNote Help: leech + memory interference (disable / clarify / distinguish card); typed-answer grading — [confidence: MEDIUM]
- Anki Manual + forums: sibling burying; no semantic similarity detection; confuse-pair advice (suspend / distinguish) — [confidence: MEDIUM]
- Cross-check: optional conflict dialogs favor identity + actions + cancel; full-content stacks are review anti-patterns — [confidence: MEDIUM]

---
*Feature research for: accidental-match resolve dialog UX (Doughnut v1.2)*
*Researched: 2026-08-05*
