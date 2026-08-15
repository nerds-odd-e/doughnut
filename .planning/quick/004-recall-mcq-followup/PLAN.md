# Plan: Recall MCQ follow-up

**Goal:** Unanswered MCQs never leak the solution on any recall-prompt
payload, then drop 003’s annotation-only path tests so nested `/api/mcqs`
routes can drop leftover `question` segments. Stop-safe. One slice = one
commit.

**Cite:** [CONTEXT.md](./CONTEXT.md)

## Decisions

1. Same unanswered projection everywhere: `Mcq.withoutSolution()` (ask and
   history). Answered history still includes solution fields.
2. Do not add dual JSON aliases.
3. Nested path rename is one observable: OpenAPI/SDK have no `question` in
   `/api/mcqs` paths. SDK regen is the stated reason to continue past ~5 min.
4. Do not rename `Quiz.vue` or the note **Questions** menu.
5. Do not collapse `MultipleChoicesQuestion` in this plan (internal; no
   following user-facing behavior). `RecallQuestionService` stays leftover.

## Slices

### 1. Unanswered history omits the solution

- **Status:** done
- **Type:** Behavior
- **Post:** Unanswered history `mcq` has stem+choices and no solution fields;
  answered still includes the solution. Ask DTO unchanged.

**Learnings:** `RecallPromptHistoryItem.from` uses `Mcq.withoutSolution()` when
`answer == null`. No other history leak site. Dropped spent
`predefinedQuestion` assertion.

### 2. Drop annotation-only Mcq path tests

- **Status:** planned
- **Type:** Structure
- **Enables:** 3. Nested MCQ routes have no question segment

Delete `McqControllerTests.HttpPaths` and `TestabilityInjectMcqsPathTest`.
They pin annotations, not HTTP, and would freeze leftover `*-question*`
segments. Existing controller behavior tests, OpenAPI, and E2E intercepts
remain the route contract. No production change.

### 3. Nested MCQ routes have no question segment

- **Status:** planned
- **Type:** Behavior
- **Pre:** Slice 2 done. Type is `Mcq`; prefix is already `/api/mcqs`.
- **Trigger:** List / add / generate / refine / export on a note.
- **Post:** OpenAPI/SDK paths under `/api/mcqs` contain no `question`
  segment. No `/api/mcqs/...note-questions`, `refine-question`,
  `generate-question-without-save`, or `export-question-generation`.
  Testability stays `inject-mcqs`. Contest stays on `/recall-prompts/{id}/contest`.

Regen client. Update E2E intercept in `addQuestionPage.ts`. Capability-named
controller methods may follow the path (e.g. list/add on the note) but do not
rename `Quiz.vue`.

**Tests:** note MCQ E2E; regen compile; OpenAPI paths have no `question`.

## Out of scope

- `Quiz.vue`, note **Questions** menu, `quizeQuestion`
- `RecallQuestionService` / CLI `RecallQuestionAnswerOutcome`
- Internal `MultipleChoicesQuestion` converter
- OpenAPI type `AnsweredQuestion`
- Dual JSON aliases

## Jidoka

- Nested path names (slice 3) if more than one obvious mapping (e.g. list
  as `/api/mcqs/{note}` vs `/api/notes/{note}/mcqs`) — stop if both are
  plausible; otherwise keep resource under `/api/mcqs/{note}` plus short
  verbs (`refine`, `generate`, `export`).
