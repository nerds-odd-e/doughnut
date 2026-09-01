# Recall prompt MCQ insert lock timeout

**Status:** planned (do not execute until asked)

## Goal

`GET /api/memory-trackers/{id}/recall-prompt` can generate an MCQ without failing
with MySQL lock wait timeout (or a sibling deadlock) while OpenAI is still
running.

## Cause

The production stack is:

```
GET /api/memory-trackers/1560/recall-prompt
  MemoryTrackerController.getRecallPrompt   (@Transactional)
    RecallQuestionService.generateNewRecallPrompt
      McqService.generateAFeasibleQuestion
        AiQuestionGenerator.getAiGeneratedQuestion   ← OpenAI, often tens of seconds
        EntityPersister.save(mcq)                    ← INSERT INTO mcq … FAILS HERE
```

MySQL error is **1205** (`Lock wait timeout exceeded`), mapped by Hibernate to
`jakarta.persistence.LockTimeoutException`. The waiter is this INSERT. InnoDB
takes a shared lock on the parent `note` row for `fk_mcq_note` (`note_id`).
Anything holding an exclusive lock on that note (or a conflicting gap/AUTO-INC
lock on `mcq`) for ~50s (`innodb_lock_wait_timeout`) produces this failure.

The transaction starts at the controller and stays open through:

1. Path-variable load of `MemoryTracker` / `Note`
2. Focus-context reads (many notes) while building the OpenAI request
3. The OpenAI HTTP call
4. Then `persist` of `mcq`

That is the bug. A GET that only needs a short read plus a short write is
holding a connection (prod pool max 12) and any locks already taken for the
whole model round-trip.

Likely lock amplifiers inside that long transaction (not proven from the
stack, but they explain why an INSERT waits):

- Hibernate auto-flush of a dirty `note` / `memory_tracker` before the INSERT
  (Timestamp dirty checking and `DisplayNameConverter` allocating a new value
  object on every load are known Hibernate pitfalls). An UPDATE of `note`
  takes an X lock; the later INSERT needs an S lock on the same row — in
  *another* session.
- Overlapping GETs (second tab, remount, retry after a slow first call).
- A concurrent writer on the same note (edit, wiki rewrite, batch MCQ import).

Prod `question.regeneration.times` is 0, so the contest/regenerate loop after
the first persist is **not** what happened on this request. The OpenAI call
*before* persist is enough.

There is **no** `SELECT … FOR UPDATE` / `LockModeType.PESSIMISTIC_*` on this
path. The exception name is from InnoDB wait/deadlock, not from an intentional
pessimistic API.

### Relation to `PessimisticLockException`

Same family, usually **not** a second unrelated bug.

| MySQL | Hibernate / JPA |
|-------|-----------------|
| 1205 lock wait timeout | `LockTimeoutException` (this stack) |
| 1213 deadlock | `PessimisticLockException` |

Two long `getRecallPrompt` transactions that dirty-flush overlapping focus-context
notes in opposite order deadlock (1213). One waiting on the other’s uncommitted
note/mcq locks times out (1205). Ending the DB transaction before OpenAI
collapses both.

Do not treat a catch-and-retry of either exception as the fix.

### What not to do

Per [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md): **prevent**
the invalid long-running transactional state. Do not catch `LockTimeoutException`
to return 503, and do not use `Propagation.NOT_SUPPORTED` around OpenAI while an
outer transaction still exists — Spring *suspends* that transaction; InnoDB
locks stay held.

`Propagation.REQUIRES_NEW` for persist-only is fine *after* the outer
transaction has already committed.

## Live system (today)

- `MemoryTrackerController.getRecallPrompt` is `@Transactional` and calls OpenAI
  for non-spelling trackers when no unanswered prompt exists.
- `findUnansweredByMemoryTracker` is a non-locking native SELECT (LIMIT 1).
- `McqService.generateAFeasibleQuestion` calls OpenAI, then `persist`s the MCQ.
- `QuestionGenerationRequestBuilder` loads focus context in-process before the
  OpenAI call; that work currently shares the controller transaction.
- Frontend `useRecallPromptFetching` fetches due prompts **sequentially** (default
  eager window 5). Overlap still happens from another tab, CLI, remount, or retry.
- Sibling LLM-in-transaction sites (not this incident, same class of bug):
  `RecallPromptController` contest/regenerate, `McqController.refine`,
  conversation/AI controllers.

## Why no test caught this

The existing recall-prompt tests specify the **happy path** (`shouldGenerateMcqWhenNoUnansweredPromptExists`): no unanswered prompt → OpenAI stub returns an MCQ → HTTP result has an MCQ. That behavior is true with or without a surrounding DB transaction. They did not miss a wrong JSON shape; they never stated the property that actually failed in production.

Three test-harness facts make lock-wait **unobservable** there:

1. **OpenAI is instant.** `OpenAiStructuredResponseMock` answers `responses().create` in-process. Production holds the connection for tens of seconds; tests hold it for milliseconds. MySQL’s default wait is ~50s. Nothing waits.
2. **One thread, one transaction.** `ControllerTestBase` is `@Transactional`. The test starts a single Spring/DB transaction, runs the controller, rolls back. InnoDB lock wait needs a **second session** holding an incompatible lock. These tests never have that other session.
3. **The test transaction hides the production boundary.** Even a test that asked `TransactionSynchronizationManager.isActualTransactionActive()` during the stub would see `true` because of the class-level test TX — after the production fix as well as before, unless that test uses `propagation = NOT_SUPPORTED`.

A Cypress scenario has the same gap: one user, sequential `getRecallPrompt` fetches, Mountebank OpenAI, no second writer on the same `note` for 50s.

Reproducing `LockTimeoutException` in CI (two connections, delayed OpenAI, `innodb_lock_wait_timeout`) would be slow and flaky. The property to test is **not** “lock wait occurred”; it is “OpenAI HTTP does not run inside a DB transaction.”

## Prevention (similar bugs)

The defect is a **class**: `@Transactional` (or any open TX) around an OpenAI HTTP call, then a write. Sibling sites already exist (contest, regenerate, refine, conversation, other AI controllers). Slice 1 only hardens this GET.

Do **not** prevent it by catching lock timeout. Do **not** use `Propagation.NOT_SUPPORTED` around OpenAI while an outer TX still exists (locks stay held).

Effective guards, cheapest first:

1. **Endpoint invariant test** (slice 1) — `NOT_SUPPORTED` + stub records `isActualTransactionActive() == false`. Catches this GET if someone puts `@Transactional` back on it. Does not catch a new AI endpoint.
2. **Ratchet on production code** (slice below) — characterization of current `@Transactional` methods that reach OpenAI HTTP; CI fails on a **new** violator and on a stale allowlist entry. No ArchUnit dependency required if the test enumerates controller methods (annotation present vs allowlist). Shrink the list as slices land.
3. **Fail loud at `OpenAiApiHandler` network methods** (only after remaining HTTP sites are off the allowlist, or tests that hit those methods stop using class-level `@Transactional`). `if (isActualTransactionActive()) throw …` turns “fails after 50s under contention” into “fails on every generate.” Cannot enable while `ControllerTestBase` still wraps AI-hitting tests in a TX — those tests would go red even when production is correct. E2E would also 500 on still-unfixed endpoints.
4. **Rule note** in backend production rules: never annotate with `@Transactional` a method that calls OpenAI HTTP; persist in a separate bean method after the call returns.

Hikari `leak-detection-threshold: 60000` can log a held connection after 60s; lock wait is 50s, so the user already got a 500. Not a substitute test.

## Slices

### 0. Ratchet: no new OpenAI-inside-`@Transactional` controller methods — Structure — planned

**What it changes:** CI, not user-visible behavior. Existing tests still pass.

**What it enables:** Slice 1 can drop `getRecallPrompt` from the allowlist; later AI endpoints cannot reintroduce the same annotation pattern unnoticed.

Test: one characterization test that lists current controller (and any service) methods which are `@Transactional` and sit on the OpenAI HTTP path (`requestAndGetStructuredResponseResult`, streaming, embeddings, transcription — not batch JSON parse). The set must **equal** a named allowlist. Adding a new annotated method fails; removing a violation without updating the list fails.

Do not start a new library for this unless the enumeration is unmaintainable. Prefer method-literal allowlist next to the test.

Stop-safe: if we never execute slice 1, the ratchet still blocks **new** copies of the bug.

### 1. Recall-prompt GET does not hold a DB transaction during OpenAI — Behavior — done

**Pre-condition:** Logged-in user has a due understanding tracker with no
unanswered recall prompt.

**Trigger:** `GET /api/memory-trackers/{id}/recall-prompt` (OpenAI returns a
valid MCQ).

**Post-condition:** The HTTP call still returns a recall prompt with an MCQ, and
the OpenAI client runs with **no active Spring transaction**.

Implemented as:

- Removed `@Transactional` from `MemoryTrackerController.getRecallPrompt`.
- Added `@Transactional` to `MemoryTrackerService.getSpellingQuestion` (short
  TX around spelling persist).
- Added `@Transactional(readOnly = true)` to
  `QuestionGenerationRequestBuilder.buildQuestionGenerationResponseRequest(Note,
  String, Long, String)` — real cross-bean call from `NoteQuestionGenerationService`,
  so focus-context reads commit before `OpenAiApiHandler` runs with no TX.
- Added a bare `@Transactional` to `EntityPersister.save(T)` (joins an existing
  TX or opens a short one) so `McqService`'s own `entityPersister.save(mcq)`,
  which now runs right after OpenAI returns with no ambient TX, still works.
- New collaborator `RecallPromptPersister` (mirrors `EntityPersister` naming)
  with one `@Transactional` method `persistRecallPromptForMcq(Mcq,
  MemoryTracker)`: rechecks for a concurrently-created unanswered prompt and
  reuses it, or creates+saves a new one via `RecallPrompt.forMcq(...)`.
  Injected into `RecallQuestionService`, called as a genuine cross-bean call
  (avoids the same-class self-invocation trap that would silently no-op
  `@Transactional`). The pre-existing private `createARecallPromptFromMcq` in
  `RecallQuestionService` is unchanged and still serves the separate
  `/recall-prompts/{id}/regenerate` flow, which keeps its own
  controller-level `@Transactional` (out of scope for this slice — no race to
  recheck there).
- Post-change-refactor pulled the shared "new unanswered recall prompt for
  this MCQ + tracker" construction into `RecallPrompt.forMcq(...)`, used by
  both `RecallPromptPersister` and `createARecallPromptFromMcq`.

Test (controller boundary, "small test"):

- New test `shouldGenerateMcqWithoutHoldingATransactionDuringOpenAiCall` on
  `MemoryTrackerRecallPromptControllerTest`, `@Transactional(propagation =
  NOT_SUPPORTED)`, fixture built via a `TransactionTemplate`/
  `PROPAGATION_REQUIRES_NEW` helper (plain no-ambient-TX entity-graph build
  hit `EntityExistsException: Detached entity` on cascade).
- `OpenAiStructuredResponseMock` gained an `onBeforeCreate(Runnable)` hook so
  the test can record `TransactionSynchronizationManager.isActualTransactionActive()`
  at the moment OpenAI is called, without disturbing existing stub behavior.
- Asserts that flag is `false` on every recorded call, and the returned prompt
  still has an MCQ.
- Existing `shouldGenerateMcqWhenNoUnansweredPromptExists` untouched — still
  the canonical payload shape.
- Slice 0 (ratchet) has not landed yet, so no allowlist entry to remove.

Focused tests green: `MemoryTrackerRecallPromptControllerTest` (incl. the new
test), `RecallPromptRegenerateControllerTest`, `RecallPromptTest`, `McqTest`
regeneration/contest tests, plus a broader sanity sweep of related
recall-prompt/spelling/batch-import tests. `scripts/check_diff_whitespace.sh`
clean.

### 2. Contest and regenerate do not hold a DB transaction during OpenAI — Behavior — planned

Same user flow after the card is shown. Same split: read / OpenAI / short write.
Extend the existing contest/regenerate controller tests with the same
“OpenAI not in a transaction” assertion (NOT_SUPPORTED + TransactionTemplate).

Stop-safe: skip this slice if slice 1 is enough and these endpoints have not
failed in production.

## Out of scope (unless they fail next)

- Catching lock timeout / deadlock to retry or map to 503.
- Intentional `SELECT … FOR UPDATE` held across OpenAI.
- Changing `innodb_lock_wait_timeout` or Hikari pool size.
- Timestamp / `DisplayNameConverter` dirty-checking (optional later if slice 1
  is not enough).
- Unique constraint on unanswered prompts per tracker.
- Conversation / `AiController` / `McqController.refine` LLM-in-TX (same pattern;
  not this stack).

## Jidoka

If slice 1’s test cannot see a real transaction boundary because of test-class
`@Transactional`, that is expected — use NOT_SUPPORTED on the new test, do not
weaken the assertion.

If persist still needs the `MemoryTracker` / `Note` to be managed, re-load by id
inside the write TX rather than reopening a long outer TX.
