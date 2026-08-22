# Tutor descriptive feedback in Learning Session protocol

**Status:** in progress (slice 1 done)

## Goal

A Tutor returns a Grade **and** free-text feedback per Session Item. Doughnut
records the text with the Feedback, the learner reviews it in the memory
tracker's recall history, and the next Learning Session Request carries the last
two dated Feedbacks per Session Item.

## Agreed Report format

Preferred block; item split by `###` heading, first structured line is `Grade: N`,
everything after (until the next `###` or the close tag) is descriptive text.

```markdown
# Learning Session Report

<session_item_feedback>
### Hola
Grade: 4
Pronunciation was clear; still mixes ser/estar under pressure.

### Gracias
Grade: 1
Needed several reminders on the soft g.
</session_item_feedback>
```

Legacy shapes stay accepted for grade-only reports: `<session_item_grades>`,
`<session_item_scores>`, and bare `Title: N` lines.

## Key design decisions

| Decision | Choice | Why |
|---|---|---|
| Item structure | `###` heading + `Grade: N` + prose | Matches Request `<session_items>`; nested `<session_item>` tags are the part LLM paste breaks |
| Block tag | `<session_item_feedback>` preferred; grades/scores blocks legacy | Outer name states Feedback (Grade + text), not just grades |
| Persistence | descriptive text on the same tutor `RecallLog` (`answer_id` null) | One Feedback = one history entry (ADR 0001); "last two" is one query |
| Grade required | Item block without a valid `Grade:` line is rejected and reported | A grade-less `RecallLog` means CONFUSION; text-only Feedback has no clean representation |
| Recording semantics | Unchanged from ADR 0003 — partial recording, append-only, per-notebook | No new failure mode |
| Learner review | recall history on the memory tracker page | Feedback history already lives there |

## Open forks (Jidoka before the slice that needs them)

1. **Tutoring status line** — keep today's `N previous sessions, last on DATE`
   and add up to two dated Feedback entries beneath (recommended), or replace it.
   Needed by slice 6.
2. **Prose size** — no cap on stored or echoed text initially (recommended);
   revisit if Requests bloat against the related-notes budget. Needed by slice 6.

Fork 3 (legacy grades block) is closed: keep `<session_item_grades>` as a
supported grade-only shape (recorded in the protocol).

## Slices

### 1. Protocol decision recorded — Docs

Status: done

Protocol (`docs/commissioned-learning-session-protocol.md`) now documents
preferred `<session_item_feedback>` (`###` + `Grade: N` + prose), Grade-required,
storage on the tutor RecallLog, and Request last-two dated Feedbacks. Legacy
grade-only shapes remain accepted. ADR 0001 **Feedback** gloss: Grade and
descriptive text; recommendations remain later. No new ADR.

Learning: the commissioned-learning ADR was already removed (glossary in ADR
0001; protocol in the normal doc). There was no Out of scope section to edit.

### 2. Report `<session_item_feedback>` records Grades — Behavior

Parser prefers the new block: `###` title, `Grade: N`, prose tolerated and
ignored for now. Legacy blocks behave exactly as today.

- E2E (`commissioned_learning_session.feature`): record a report in the new
  format → Feedback shown, both trackers scheduled.
- Unit (`LearningSessionReportParser` boundary): item without `Grade:` rejected;
  grade outside 1–4 rejected; unknown title rejected; duplicate title rejected;
  new block wins over a legacy block present in the same document.

Stop-safe alone: Tutors can use the richer shape; prose is ignored, nothing lost
that Doughnut promised to keep.

### 3. Tutor feedback column — Structure

- Migration `V300000301__add_tutor_feedback_to_recall_log.sql` — `tutor_feedback`
  TEXT NULL on `recall_log`.
- `RecallLog` field + JSON property; regenerate `docs/database-erd.md`
  (`database-erd` skill) and the frontend API client (`generate-api-client`).

Verify: no observable change; backend + frontend suites stay green. Exists only
for slice 4.

### 4. Descriptive feedback recorded and reviewable — Behavior

- Parse result carries the text; `LearningSessionService` writes it on the tutor
  RecallLog it already creates.
- `RecallHistory.vue` renders it (`data-testid="recall-log-tutor-feedback"`).
- E2E: record a report with prose → visit the commissioned memory tracker →
  see the tutor's text.
- Frontend unit test: text rendered when present, absent when not.

### 5. Request asks for descriptive feedback — Behavior

`<how_to_report>` describes the new format and the worked example includes prose,
so Tutors actually produce what slice 4 can store.

- E2E: extend the existing "instruct the tutor to report one grade per session
  item" assertion to cover the descriptive-feedback instruction.

### 6. Request carries the last two Feedbacks with dates — Behavior

- Repository query: last two tutor RecallLogs per tracker (recordedAt, grade,
  text).
- Render per Session Item alongside the tutoring status (fork 1).
- E2E: two recorded sessions → open the request → both dated Feedbacks appear
  for that Session Item.

## Notes

- Migration versions must exceed `300000300` (`db-migration.mdc`).
- No product artifact carries plan or slice numbers.
