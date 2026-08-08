# Plan: Learning session hub + tagged report scores

Ad-hoc polish after post-v1.3 CLS work. Unifies commission / record / amend
behind a always-visible bar icon with a two-step flow (session list → session
detail), and tightens the Learning Session Report protocol with a tagged score
block the parser reads exclusively.

**Status:** planned  
**Capability names:** recall learning session actions; Learning Session Report
parsing  
**Related:** Proposed ADR 0005 — update Request example and Report section.

---

## Target behaviors

### Bar icon

- GraduationCap icon stays in the progress bar button group, **next to** the
  view-last-answered control.
- **Always visible** (even when there are no potential or awaiting sessions) so
  the learner can open recorded sessions to amend.
- Optional badge when the session list is non-empty (potential + awaiting +
  recorded); no badge when the list is empty.

### Two-step flow

1. Click icon → **session list** (potential, awaiting report, recorded).
2. Pick an entry → **session detail** (existing commission / request / report
   UI in `CommissionLearningSessionDialog`).

No direct open of session detail from the icon when only one entry exists.

### Report protocol

Scores live inside a tagged block; prose and markdown headers outside are
ignored:

```markdown
# Learning Session Report

Thanks for a great session today.

<session_item_scores>
Hola: 5
Gracias: 1
</session_item_scores>
```

Parser extracts lines only from `<session_item_scores>…</session_item_scores>`.
If the tag is **absent**, fall back to legacy whole-document parsing (minus
`# Learning Session Report` header) so older pasted reports still work.

Request `<how_to_report>` example and ADR 0005 updated to show the tag.

---

## Key design decisions

| Decision | Rationale |
|----------|-----------|
| Tag name `<session_item_scores>` | Mirrors request tags (`<session_items>`, etc.) |
| Legacy fallback when tag missing | Stop-safe; amend/record of old reports still works |
| Recorded sessions in hub list, strips removed | One entry point for all session actions |
| List modal before detail modal | User asked to see sessions first, then open one |
| Reuse `CommissionLearningSessionDialog` for detail step | Minimal churn; only navigation changes |

---

## Phases

### Phase 1 — Behavior: Parse scores from `<session_item_scores>` tag

**Status:** done  
**Type:** Behavior

**Observable behavior**

Recording a Learning Session Report succeeds when scores are inside
`<session_item_scores>`, even when extra prose and a `# Learning Session
Report` header appear outside the tag.

**Precondition:** Commissioned session with session items.  
**Trigger:** POST record with tagged report markdown.  
**Postcondition:** Matched scores recorded; lines outside the tag ignored;
reports without the tag still parse via legacy path.

**Tests**

- `LearningSessionReportParserTest`: tagged report with surrounding prose;
  missing tag uses legacy; empty tag; malformed tag.
- Update `LearningSessionRequestMarkdownBuilder` example in `<how_to_report>`.
- Update ADR 0005 Report section and request example.
- Update E2E report payloads in `commissioned_learning_session.feature` /
  step definitions to use tagged scores (canonical shape once).

**Implementation sketch**

- `LearningSessionReportParser`: extract tag content (case-sensitive tag names
  matching request style); parse lines inside only.
- Shared tag constant or small helper if builder and parser both reference the
  tag name.

**Done when:** Parser tests green; record/amend controller tests still pass;
E2E record + amend scenarios green with tagged reports.

**Completed 2026-08-08:** Tag extraction with legacy fallback; ADR + E2E updated.

---

### Phase 2 — Behavior: Always-visible hub icon with full session list

**Status:** planned  
**Type:** Behavior

**Observable behavior**

The recall bar always shows the learning-session icon. Clicking it opens a list
of potential, awaiting-report, and recorded sessions. Recorded-session strips
below the bar are removed.

**Precondition:** Zero or more sessions in any of the three buckets.  
**Trigger:** Open recall page; click learning-session icon.  
**Postcondition:** Icon visible even with only recorded sessions (or none);
list shows all applicable entries with action labels (Commission / Record
report / Amend report); no `recorded-learning-session` strips.

**Tests**

- `RecallProgressBar.spec.ts` / `RecallLearningSessionActions`: icon always
  present; list includes recorded; badge when list non-empty.
- E2E: amend path via icon + list (replace strip `amend-learning-session-report`
  clicks); potential-session count assertions unchanged in spirit.

**Implementation sketch**

- `RecallLearningSessionActions`: remove `v-if="actionableSessionCount > 0"`;
  add `recordedSessions` prop; extend `actionableSessions` with amend entries;
  **always** show picker on click (remove single-entry shortcut).
- `RecallProgressBar`: pass `recordedSessions`; remove recorded strips.

**Done when:** Frontend unit tests + commissioned learning E2E green.

---

### Phase 3 — Behavior: Session list step before session detail

**Status:** planned  
**Type:** Behavior

**Observable behavior**

Choosing a session from the hub list opens the session detail (request copy,
report submit) in a separate step — the list closes or yields to detail; user
returns to recall after closing detail.

**Precondition:** Hub list visible with at least one entry.  
**Trigger:** Click a list entry.  
**Postcondition:** `CommissionLearningSessionDialog` opens for that entry's mode;
list is not shown concurrently with detail.

**Tests**

- Unit: picker entry click emits selection; detail dialog mounts with correct
  mode (commission / record / amend).
- E2E: commission and record flows go icon → list → entry → detail (update
  `recallLearningSessionMethods` if list is now always shown).

**Implementation sketch**

- If Phase 2 already opens dialog on pick, this phase may only need a dedicated
  list modal/sheet wrapper so list and detail are visually distinct steps.
  Extract `LearningSessionListDialog` or equivalent if picker inline panel is
  too cramped for the two-step mental model.
- Ensure commission flow: list → detail → user clicks Commission → request shown
  (detail already supports this).

**Done when:** E2E commission, record, amend, multi-notebook scenarios green
with explicit two-step navigation.

---

## Out of scope

- Machine transport / Tutor authentication
- Changing score rubric or scheduling (ADR 0003)
- Folding unrelated recall bar controls

## Discoveries affecting remaining work

- `CommissionLearningSessionDialog` already handles commission → request →
  record in one modal; Phase 3 is mostly navigation shell, not new API.
- E2E `commissionLearningSession` auto-clicks commission submit when dialog
  already open — must tolerate list step first.
- `008` plan file may still exist locally from trash; this plan supersedes its
  session-badge UX for hub + two-step.

## Execution notes

- Backend-first (Phase 1), then frontend Phases 2–3 sequentially.
- Targeted tests per phase; full `commissioned_learning_session.feature` after
  each frontend phase.
- Nix prefix for tooling; git without Nix.
