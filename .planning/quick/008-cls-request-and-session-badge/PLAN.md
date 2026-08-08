# Plan: CLS post-v1.3 request brief + session badge

Ad-hoc polish after **v1.3 Commissioned Learning Session MVP**. Improves the
Learning Session Request the learner copies to a Tutor, and compresses
commission / record-report entry points into a single progress-bar icon with a
count badge.

**Status:** planned  
**Capability names (product):** Learning Session Request markdown;
recall progress bar session actions  
**Related:** Proposed ADR 0005 (`docs/adrs/0005-commissioned-learning-session-protocol.md`) —
update the draft Request example to match; still Proposed (humans approve).

---

## Target Request shape (design contract)

Commission (and re-render for awaiting/recorded sessions) produces markdown
roughly like:

```markdown
# Learning Session Request

<instructions>
You are the tutor to help the learner to study Spanish conversation.

Focus on conversational phrases.

Wait for the learner's instruction before starting the learning session.
</instructions>

<session_item_titles>
- Hola
- Gracias
</session_item_titles>

<session_items>
### Hola
- Expected learning content: Hello
- Learning status: not yet tutored

### Gracias
- Expected learning content: Thank you
- Learning status: not yet tutored
</session_items>

<how_to_report>
Teach the session items above, then return a Learning Session Report giving one
score from 0 to 5 per item:

- 5 — mastered the learning point with full fluency
- 4 — mastered the learning point with fluency
- 3 — mastered the learning point, but not fluent
- 2 — needed a reminder at first, then showed signs of mastering it
- 1 — needed several reminders
- 0 — could not reach the learning point even with help

Example of how to provide feedback:

# Learning Session Report

Hola: 5
Gracias: 1

Only score session items that were actually taught in this session. Do not list
items that were not learnt in the session.
</how_to_report>
```

Notes:

- Middle paragraph under `<instructions>` is the notebook readme’s
  `question_generation_instruction` (or legacy `questionGenerationInstruction`)
  when present; omit that paragraph when absent. **Notebook readme only** — not
  folder/note trail (user asked for notebook instruction).
- XML-ish tags are literal section delimiters inside markdown (LLM-friendly), not
  a schema to validate.
- Report parsing stays title → score (ADR 0005 matching); this plan does not
  change Report grammar.

---

## Key design decisions

| Decision | Rationale |
|----------|-----------|
| Reuse notebook-readme QGI frontmatter parse (same keys as question generation) | One representation; already on notebook readme |
| Whole Request rewrite stays in `LearningSessionRequestMarkdownBuilder` | Single place that owns the brief; controller tests already assert markdown |
| Badge count = `potentialLearningSessions.length + awaitingReportSessions.length` | Matches “potential + not yet reported”; recorded/amend stay separate strips |
| Icon in ProgressBar `#buttons`; hidden when count is 0 | Compact recall chrome; no empty CTA |
| One actionable session → open that dialog; several → short picker then dialog | Avoid forced menu when only one action exists |
| Update Proposed ADR 0005 Request example in the request phases | Protocol doc must match shipped brief; still human-owned approval |

---

## Phases

### Phase 1 — Behavior: Tutor role, notebook QGI, wait for learner

**Status:** done  
**Type:** Behavior

**Observable behavior**

When a learner commissions a learning session, the Request’s instructions tell
the Tutor they are tutoring that notebook by name, include the notebook’s
`question_generation_instruction` when set, and tell the Tutor to wait for the
learner’s instruction before starting.

**Precondition:** Notebook with due commissioned trackers; optional notebook
readme frontmatter with `question_generation_instruction`.  
**Trigger:** Commission learning session.  
**Postcondition:** `requestMarkdown` contains the tutor-role sentence with the
notebook name, the QGI text when present (and omits it when absent), and an
explicit wait-for-learner instruction.

**Tests**

- Extend / adjust `LearningSessionCommissionTests` (controller boundary):
  - Canonical Spanish notebook: role sentence + wait instruction present.
  - With notebook readme QGI: instruction text appears after the role sentence.
  - Without QGI: no leftover empty instruction block / no phantom QGI text.
- Keep E2E commission scenario green; add a light assertion only if the happy
  path must mention the role sentence (prefer unit for QGI presence/absence).
- Update Proposed ADR 0005 Request example for the new instructions (same phase
  as behavior so doc and code do not diverge).

**Implementation sketch**

- `LearningSessionRequestMarkdownBuilder`: prepend instructions (still fine to
  use `##` headings here if Phase 2 has not landed yet — interim OK).
- Extract QGI from `session.getNotebook().getReadmeContent()` via existing
  frontmatter helpers (`NoteContentMarkdown` / same key list as
  `NoteRealmService`). Prefer extracting a tiny shared parse helper only if
  duplication is immediate and ugly; do not invent a second key name.

**Done when:** Targeted backend tests green; commission E2E still passes;
ADR 0005 draft instructions section matches.

**Completed 2026-08-08:** `## Instructions` block with tutor role, optional
notebook-readme QGI via shared `FrontmatterQuestionGenerationInstruction`,
wait-for-learner line. Refactor deduped QGI parsing into `NoteRealmService`.

---

### Phase 2 — Behavior: XML sections, title list first, report example

**Status:** done  
**Type:** Behavior

**Observable behavior**

The Learning Session Request uses XML-ish section tags, lists session item
titles before the detailed items, and the how-to-report section includes a
concrete feedback example plus an instruction not to list items that were not
learnt in the session.

**Precondition:** Same as Phase 1 (commission yields a Request).  
**Trigger:** Commission (or view request on an awaiting session).  
**Postcondition:** Markdown contains `<instructions>`, `<session_item_titles>`,
`<session_items>`, and `<how_to_report>`; titles appear as a list before
detailed `###` items; how-to-report includes an example report and the
omit-unlearnt guidance.

**Tests**

- Update commission markdown assertions to the XML-ish contract (canonical shape
  once; QGI case only asserts QGI still lands inside `<instructions>`).
- Update E2E page-object helpers that look for rubric / `###` headers so they
  still match (e.g. titles may appear both in `<session_item_titles>` and as
  `###` under `<session_items>` — assert without double-counting in
  “only notes” checks).
- ADR 0005 Request example → full target shape above.

**Implementation sketch**

- Finish the target shape in `LearningSessionRequestMarkdownBuilder`.
- Remove interim Phase 1 headings if they conflict with the tagged sections.

**Done when:** Backend + E2E commission scenarios green; ADR draft matches.

**Completed 2026-08-08:** XML-ish tagged sections, title list before items,
report example + omit-unlearnt guidance; E2E page object counts `###` only
inside `<session_items>`.

---

### Phase 3 — Behavior: Session action icon with badge on recall progress bar

**Status:** planned  
**Type:** Behavior

**Observable behavior**

On the recall progress bar, potential sessions and sessions awaiting a report
are reached via one icon button in the bar. The button shows a number badge =
count of potential sessions + awaiting-report sessions. The button is absent
when that count is 0. Activating an entry still opens the existing commission /
record dialog for that notebook/session.

**Precondition:** Zero, one, or several potential and/or awaiting sessions.  
**Trigger:** Open recall page; optionally click the session-actions icon and
choose an entry.  
**Postcondition:**

- Count 0 → no session-actions icon (and no potential/awaiting strips).
- Count N → icon with badge N in the progress bar button group.
- Single actionable entry → click opens that dialog directly.
- Multiple → picker then dialog for the chosen entry.
- Recorded sessions (amend) remain available as today (strips unchanged unless
  a follow-up asks to fold them in).

**Tests**

- `RecallProgressBar.spec.ts`: replace strip-based potential/awaiting cases with
  badge visibility, count, and open-dialog flows (single vs multi).
- Update E2E steps/page objects that assert
  `potential-learning-session` / commission CTAs to use the icon + picker path
  (`commissioned_learning_session.feature` stays capability-named).
- Recorded/amend scenarios still pass unchanged.

**Implementation sketch**

- Add icon (e.g. Lucide, consistent with Pause/SkipBack) into ProgressBar
  `#buttons` beside existing recall controls.
- Badge via existing DaisyUI badge patterns used elsewhere in nav.
- Drop potential + awaiting `LearningSessionStrip` rows from the bar body.
- Keep `CommissionLearningSessionDialog` wiring; only change how the dialog is
  opened.

**Done when:** Frontend unit tests + commissioned learning E2E green for
potential, multi-notebook, commission, record, and amend paths.

---

## Out of scope

- Changing Learning Session Report parsing or score rubric values
- Folder/note-level QGI trail in the Request (notebook only)
- Folding recorded/amend into the badge
- Machine transport / smart request generator (ADR 0005 out of scope)
- Approving ADR 0005 (human process)

## Discoveries affecting remaining work

- Request markdown is owned solely by `LearningSessionRequestMarkdownBuilder`
  and asserted heavily in `LearningSessionCommissionTests` + E2E page objects —
  update those together; do not add a parallel builder test surface.
- Notebook QGI already parses from readme frontmatter (`NoteRealmService`);
  reuse that parsing approach for notebook-only extraction.
- E2E currently binds UX to strip `data-test` ids; Phase 3 must update steps
  before or with the UI change so scenarios stay green.
- ADR 0005 is still **Proposed**; treat draft updates as part of shipping the
  brief, not as Accepted architecture until humans approve.

## Execution notes

- Run targeted backend tests after Phases 1–2; frontend unit +
  `pnpm cypress run --spec e2e_test/features/learning_session/commissioned_learning_session.feature`
  after Phase 3 (and after request phases if E2E assertions change).
- Assume `pnpm sut` running; Nix prefix for tooling; git without Nix.
- After each phase: Jidoka → post-change-refactor → update this plan →
  commit + push (execute-plan wrap-up).
