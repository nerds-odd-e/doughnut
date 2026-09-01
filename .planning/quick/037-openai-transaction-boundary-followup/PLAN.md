# Follow-up cleanup: OpenAI transaction-boundary work

**Status:** planned (do not execute until asked)

## Goal

Code-review follow-up on the just-shipped recall-prompt lock-timeout work
(commits `0e819ab7e8`, `559cc047fe`, `db7e6ec708`, `8cf67dd9fc`; plan retired
from `.planning/quick/035-recall-prompt-lock-timeout/`). Not a new feature —
closes gaps the review found in that work: a live test-coverage hole for the
exact regression class CI just caught, a missed duplication, a comment-style
miss, and adjacent dead code.

## Findings (source: code review of the four commits above)

1. **Test coverage gap.** The regression this session hit — CI failure on
   `question_contest.feature` ("No EntityManager with actual transaction
   available ... cannot reliably process 'merge' call"), fixed in
   `db7e6ec708` by adding `@Transactional` to `EntityPersister.merge`/`remove`
   — has no fast backend test covering it, and structurally can't be caught
   by one today:
   - `McqTest` is class-level `@Transactional`, so it always supplies an
     ambient transaction and can never observe a missing `@Transactional` on
     `EntityPersister.merge`/`remove` — the same masking effect
     `.planning/quick/035-.../PLAN.md` documented as the reason the
     *original* bug escaped test coverage.
   - `MemoryTrackerRecallPromptControllerTest#shouldGenerateMcqWithoutHoldingATransactionDuringOpenAiCall`
     (added in `0e819ab7e8`) registers only one OpenAI structured-response
     stub, so `McqService.generateAFeasibleQuestion`'s `contest()`/regenerate
     loop never runs (`contest()` returns `null` with no evaluation stub
     registered, short-circuiting before `entityPersister.merge(mcq)` is ever
     reached). It cannot exercise the call path that actually broke.
   - Net effect: only the ~2min Cypress e2e suite would catch a regression
     here today.

2. **Duplicated test helper.** `MemoryTrackerRecallPromptControllerTest`'s
   private `inCommittedTransaction(Supplier<T> action)` (added in
   `0e819ab7e8`) is a byte-for-byte copy of the existing helper in
   `QuestionGenerationBatchRowImportServiceAtomicTest` (same
   `TransactionTemplate` / `PROPAGATION_REQUIRES_NEW` setup). The
   implementer's own report said "same pattern as
   `QuestionGenerationBatchRowImportServiceAtomicTest`" but copied instead of
   extracting a shared helper — missed by that slice's post-change-refactor
   pass.

3. **Comment-convention violation.** `RecallPromptPersister` (new in
   `0e819ab7e8`) carries a 4-line Javadoc class comment. Repo convention
   (`CLAUDE.md` / `general.mdc`): no multi-line comment blocks, one short
   line max, only when the WHY is non-obvious. Missed by that slice's
   post-change-refactor pass.

4. **Dead code adjacent to the change.**
   `QuestionGenerationRequestBuilder.buildQuestionGenerationResponseRequest(Note,
   String, Long, String, User)` (5-arg overload with an explicit `viewer`
   param — in the exact class `0e819ab7e8` added
   `@Transactional(readOnly = true)` to, on its 4-arg sibling only) and its
   only caller
   `NoteQuestionGenerationService.buildQuestionGenerationRequest(Note,
   String, Long, String, User)` have zero callers anywhere in `backend/src`
   (main or test) as of this review. This overload is *not* covered by the
   new `@Transactional(readOnly = true)`, unlike its 4-arg sibling — a live
   footgun if it's ever wired up later (silently reopens the "focus-context
   read with no transaction boundary" problem this plan just closed for the
   4-arg path). Pre-dates this plan, but sits directly in the file the plan
   modified and shares its exact theme.

## Slices

### 1. Shared transaction test helper — Structure — done

**What it changes:** Extract `inCommittedTransaction` (both the
`Supplier<T>` and `Runnable` overloads) out of
`QuestionGenerationBatchRowImportServiceAtomicTest` into a shared, reusable
test-support helper (e.g. a small static utility or trait under
`com.odde.donut.testability`, injected/constructed the same way both test
classes already obtain their `PlatformTransactionManager`). Update
`QuestionGenerationBatchRowImportServiceAtomicTest` and
`MemoryTrackerRecallPromptControllerTest` to use it instead of their private
copies.

**What it enables:** Slice 2 extends
`MemoryTrackerRecallPromptControllerTest` and needs this helper again;
sharing it now avoids a third copy.

No behavior change; existing tests must stay green.

### 2. Regression test for the contest/regenerate transaction boundary — Behavior — done

**Pre-condition:** Logged-in user has a due understanding tracker with no
unanswered recall prompt.

**Trigger:** `GET /api/memory-trackers/{id}/recall-prompt`, with OpenAI
stubbed to first generate a question and then evaluate it as **not
legitimate** (so `McqService.generateAFeasibleQuestion`'s contest/regenerate
loop actually runs and calls `entityPersister.merge(mcq)`), then a second
generation for the replacement question.

**Post-condition:** The HTTP call returns a recall prompt with the
regenerated MCQ, and **no Spring transaction is active** at any of the
OpenAI calls involved (initial generation, evaluation, regeneration) — not
just the single-call happy path already covered.

Implementation:

- Extend `MemoryTrackerRecallPromptControllerTest` (new test, or extend the
  existing `shouldGenerateMcqWithoutHoldingATransactionDuringOpenAiCall`)
  using `OpenAiStructuredResponseMock` to stub an evaluation response
  (rejecting the first question) plus a regenerated-question response,
  mirroring `question_contest.feature`'s "Internally contested MCQs are
  replaced before recall" scenario as a fast backend test instead of only an
  e2e one.
- Keep asserting via the existing `onBeforeCreate` hook +
  `TransactionSynchronizationManager.isActualTransactionActive()` pattern —
  every recorded call must be `false`.
- Test observables through the existing controller entry point
  (`unit-testing.mdc`) — do not add a narrow unit test that calls
  `EntityPersister.merge` directly; that would test an implementation detail
  the production code path doesn't guarantee stays the same shape.

Stop-safe: if stubbing a rejection + regeneration through
`OpenAiStructuredResponseMock` turns out to need mock-infrastructure changes
beyond a small stub call, that infrastructure work is its own Structure
slice — do not silently expand this slice's scope past ~5–10 minutes.

### 3. Trim RecallPromptPersister's comment — Structure — planned

**What it changes:** `RecallPromptPersister`'s 4-line Javadoc class comment
is trimmed to at most one short line (or removed, if the class/method names
already carry the meaning), per the repo's no-multi-line-comment-blocks
convention.

**What it enables:** Nothing further; ride-along cleanup, safe to fold into
slice 1 or 2's commit if the coordinator judges that more efficient, since
it touches no behavior.

No test change needed; existing tests must stay green.

### 4. Remove dead `buildQuestionGenerationResponseRequest`/`buildQuestionGenerationRequest` overloads — Structure — planned

**What it changes:** Delete
`QuestionGenerationRequestBuilder.buildQuestionGenerationResponseRequest(Note,
String, Long, String, User)` (5-arg) and
`NoteQuestionGenerationService.buildQuestionGenerationRequest(Note, String,
Long, String, User)` (5-arg), and any now-unused private helper they alone
depended on.

**Pre-flight (required before deleting):** Re-run a repo-wide grep for both
exact signatures across `backend/src` (main and test) to confirm zero
callers still holds at execution time — this review's snapshot may be stale
by then.

**What it enables:** Removes a live footgun (an unused, uncovered
transaction-boundary variant sitting next to the one this plan just fixed).

Stop-safe: if the grep turns up a caller that was added since this review
(e.g. by unrelated work landing on `main`), abandon this slice — it's no
longer dead code, and removing it would be a behavior change requiring its
own plan.

## Out of scope

- Any change to `entityPersister.merge`/`remove`/`save` transactional
  semantics themselves — already fixed and verified in `db7e6ec708`.
- Slice 2 (contest/regenerate transaction boundary) from the retired
  `035-recall-prompt-lock-timeout` plan — that was explicitly stop-safe
  skipped there (no production failures reported on those endpoints) and
  stays out of scope here too.
- Broader dead-code sweep of `EntityPersister`/`RecallQuestionService`/etc.
  beyond the one overload pair identified above.

## Jidoka

If slice 2's stubbing needs more than a small addition to
`OpenAiStructuredResponseMock` (e.g. a new mock capability), stop and
scrutinize per the time-budget rule rather than growing the mock
infrastructure inside this slice.
