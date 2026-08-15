# Recall MCQ follow-up — context

**Status:** planned  
**Cite:** [PLAN.md](./PLAN.md), prior [003-recall-prompt-mcq](../003-recall-prompt-mcq/PLAN.md)  
**Glossary:** Proposed [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md) — **recall prompt** HAS_A **MCQ**; unanswered projection must not leak the solution.

## What 003 shipped

OpenAPI type `Mcq`, HTTP `/api/mcqs`, tables `mcq` / `answer`, unanswered ask
`RecallPrompt.mcq` via `Mcq.withoutSolution()`, AI parse type `GeneratedMcq`.

## Findings from inspecting 003 (slices 1–13)

### Bug

Unanswered **history** still serializes the persisted `Mcq` (`RecallPromptHistoryItem.from`
sets `item.setMcq(recallPrompt.getMcq())`). JSON can include `correctAnswerIndex`,
`testedFocus`, and `validationRationale`. The ask DTO already omits those.

The tracker page lists unanswered prompts in history. The Vue unanswered branch
does not paint the correct choice, but the HTTP payload still leaks the solution
(Decision 2). CLI `tryLoadMcqPayload` prefers a pending history item; it does not
display the index, but the same leak is on the wire.

### Dead / redundant tests (introduced in 003)

- `McqControllerTests.HttpPaths` and `TestabilityInjectMcqsPathTest` only read
  Spring annotations. They do not hit HTTP. OpenAPI + E2E intercept already
  pin the routes. They also freeze leftover `*-question*` path segments.
- `shouldExposeAnsweredMcqUnderMcqField` still asserts `predefinedQuestion` is
  absent (spent translation guard). It does not cover unanswered history.

### Missed cohesion (not user-facing by itself)

- `Mcq` JSON flatten still wraps internal `MultipleChoicesQuestion` +
  `MCQToJsonConverter`. Third copy of stem+choices besides `GeneratedMcq`.
- `RecallQuestionService`, `generateAQuestion`, `getAllQuestionByNote`,
  `AnsweredQuestion`, `Quiz.vue`, `quizeQuestion` — leftover **question** names.

Those last identifiers were leftover in 003 on purpose (`Quiz.vue` / Questions
menu). This follow-up does **not** rename UI chrome. Nested `*-question*` **HTTP
paths** are operator-observable API leftover and are in scope after the leak.

## Out of this plan

- `Quiz.vue`, note **Questions** menu, `quizeQuestion` prop typo
- Renaming OpenAPI `AnsweredQuestion`
- Using the note MCQ list as the recall source
- Splitting `e2e_test/start/testability.ts` (800+ lines; 003 only renamed a field)
