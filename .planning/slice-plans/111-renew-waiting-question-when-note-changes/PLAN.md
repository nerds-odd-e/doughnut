# Renew a waiting question when its note changes

**Identity:** SEED-073#renew-waiting-question-when-note-changes
**Source:** [Renew a waiting question when its note changes](../../seeds/SEED-073-learning-refinement-and-retrieval.md#renew-waiting-question-when-note-changes)
**Source review:** [Independent source review](source-review.md)

## Execution context

Story Branch Mode. Dashboard claim `1d5aace35c3890daad54fc736c540d0546c01d66` was accepted on `origin/main` and published to `origin/codex/renew-a-waiting-question-when-its-note-changes`. Publisher `dashboard-territory.local-doughnut`, agent `philip-chan`.

GitHub Actions `ci.yml` (donut CI) is observed from this execution checkout. Coordinator `renew-waiting-question-5429872a`, mailbox `/tmp/dough-ci-501/watch-2XulhF`, stream cell `g102:17`, session `11773`, PID `48144`. Claim CI on trunk was unobserved by this branch observer.

Slice 1 was accepted and registered on the story branch at `9ea958ddbf5ef712d9663b0e2a97a577cc118813`. During slice 2, its CI attempt hit the same shared canvas setup failure in two jobs. The [diagnosis and proof](diagnosis-canvas-apt-source.md) own the repair. Slice 2 remains planned.

## Goal and scope

The learner first sees an MCQ generated from the current authored title, body and properties of its focus note. A punctuation edit counts. Matching source content permits reuse even after a long interval. Once the card appears, its prompt ID, choices and key stay together through answering. Apply this to ordinary web and CLI MCQ recall, including stored, on-demand and batch questions. The focus note can be edited through any accepted publication path. Spelling, linked-note invalidation, semantic question quality and scheduling policy have separate owners.

## Design and decisions

Use the existing server operation that selects or generates a question for a tracker as the freshness authority. Capture a private, deterministic fingerprint of the focus identity, authored title and full stored Markdown (including frontmatter) from the same read that supplies generation. Distinguish the fields unambiguously. A notebook commit would renew questions after unrelated changes. A note update timestamp or batch import timestamp cannot identify the input used. The accepted publication and its projection already own note content. [North Star, One accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary) · [ADR 0002, Git-native Portable notebook tree synchronization](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)

Keep the fingerprint with the generated MCQ, including when multiple prompts share it. Carry it with a batch request from rendering through import. Compare at selection, including the persister's final recheck. Restrict reuse to eligible, unanswered, uncontested questions with matching evidence on this tracker, preserving newest-first selection among matches. If none qualifies, generate on demand. Unknown provenance on a legacy row is handled at its next selection. Avoid a queue sweep, age cutoff, semantic model call or update to a previously issued MCQ. A learner can still submit the ID already on screen or view an earlier answer and its original choices. [Recall question ownership and lifetime](../../../docs/recall-question-loading.md#question-ownership-and-lifetime)

Background fetch may warm the question. Foreground selection authorizes that card for display. Web can use the same endpoint twice and retain the foreground response for the active attempt. CLI can use the existing singular endpoint directly. A visible card stays bound to its returned prompt ID. This contract needs no cross-client clock or acknowledgement that a person actually read a card. Existing access checks, failure recovery, explicit answering and grading apply. [ADR 0003, Spaced-repetition scheduling policy](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md) · [ADR 0006, Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md)

## Decisive premises observed

Local observations on 2026-11-18 at `01c8a94235523f02d7ec8fb4a05434af02ad6825` used the isolated MySQL test profile, Chromium and the Ink test terminal. All scratch changes were restored. The observations establish the current paths and their test boundaries. The slices own proof of the new behavior.

| Consuming operation | Observation and consequence |
| --- | --- |
| Batch input → late import → MCQ selection | In a temporary case in `QuestionGenerationBatchRowImportServiceTest`, the real JSONL renderer emitted “Observatory opens at nine”. After saving “Observatory opens at ten”, the row importer accepted the prepared provider output. Normal selection returned the imported stem on the tracker whose note now said “ten”. Import time would misclassify it. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` succeeded with 3,202 cases including the witness, two skipped. The run also exercised ordinary generation against the current tracked note, concurrent prompt reuse, repeated answer handling and recovery. |
| Web prefetch → first visible card | A scratch Chromium spec mounted two MCQ trackers with eager fetch 2. After prefetch it changed the SDK response for tracker 2 from “nine” to “ten”. Advancing first displayed “nine”. A temporary foreground re-fetch made the same navigation display “ten”. With `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/recall tests/components/recall`, the new witness passed and one maintained assertion failed because it expected the next background request before revalidation settled. After restoration all 210 maintained tests passed. Slice 5 owns that assertion's navigation and prefetch purpose. |
| CLI history → visible terminal frame | A scratch Ink case supplied history “Choose Alpha?” and a distinct current MCQ “Observatory opens at ten”. The ordinary command displayed the history stem and skipped the singular operation. A scratch switch to the singular operation displayed “ten”. `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/scratchWaitingRecall.test.tsx` exercised the witness. With that switch, the `recallMcqInteractive`, `recallJustReviewInteractive` and `recallSpellingInteractive` suites had 12 failures in four MCQ files sharing a history-only fixture. The restored command `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run recallMcqInteractive recallJustReviewInteractive recallSpellingInteractive` passed all 37 cases. Slices 6–7 own the fixture and terminal change. |

## Ordered slices

### 1. Keep the source of an on-demand question with the question

Type: Structure
Status: done

Capture the input fingerprint within synchronous request construction and persist it with the resulting MCQ through the existing atomic question/prompt write. Keep it private in API output. Preserve legacy rows with unknown input. This supplies the evidence the next slice compares. Existing selection still returns the newest eligible unanswered prompt at this point.

**Proof.** From the controller, generate a question with the model stub, inspect the submitted focus and reload the selected MCQ. Check that the stored evidence corresponds to the authored title and Markdown used in that request, including frontmatter. Preserve the returned choices, solution omission, atomic write and read of a legacy MCQ. Where new columns are used, deliver compatible additive schema first and follow the repository's schema-before-reader release ordering. [Database migration guidance](../../../.agents/skills/db-migration/SKILL.md#release-safety)

**Accepted proof.** `MemoryTrackerQuestionPersistenceControllerTest.preservesTheGenerationFocusWithThePersistedQuestion` captured the model request for a Unicode title and full Markdown with frontmatter. Reloading the persisted MCQ confirmed the independently framed SHA-256 fingerprint, original choices and answer key. Jackson API serialization omitted the key and fingerprint from the visible question, and the legacy cases still selected questions. The concurrent-insert and atomic-write cases preserved their existing outcomes. The fingerprint lives in the existing private MCQ JSON payload, so it is compatible with the current schema.

The independent refactor unified focus hydration for request construction and returned a 224-line request builder. The focused implementation and refactor runs passed 116 and 113 cases. The final `CURSOR_DEV=true nix develop -c pnpm backend:test_only` run passed 3,209 cases with two skipped. Reports and consumer traces are in this checkout's Git evidence directory under `slice-1-`.

### 2. Offer a question made from the current focus note

Type: Behavior
Status: planned

A learner asks for an MCQ after an accepted title, body or property edit. Reuse an eligible question with current evidence, or generate and return a current one for the same tracker. Treat a missing fingerprint as unknown on demand. Keep older prompts and their keys available by ID. Batch results without input evidence use this same rule until slice 4.

**Proof.** At the normal tracker controller, store a question, publish an edit, then load recall. Capture the new focus sent for generation and assert the new returned prompt and choices. Distinguish a corrected fact, a punctuation-only edit, and a property change. A second load with matching input reuses its prompt. A legacy row is renewed on demand. Answer a previously issued ID after renewal and read back its original question, answer and Grade. Loading and retrying alone leave due time, log and prior answers intact. Stub a failed generation and check the existing unavailable response and successful retry.

### 3. Present the current input when an edit races with renewal

Type: Behavior
Status: planned

An edit can land during the model call. Recheck the candidate against the current projected focus before returning it. A concurrent reusable prompt must satisfy the same selection rule as other candidates. Keep provider calls outside write locks. If a current question cannot be obtained, ordinary load recovery applies.

**Proof.** Use the external model stub's before-response hook to commit an edit from “nine” to “ten” while a question is generated. The load or its retry must present a question derived from “ten”. Use the existing concurrent-insert example to distinguish reuse of a matching concurrent prompt from a stale one. Check persisted question/answer bindings and that the call holds no write transaction. [Atomic generation fixture](../../../backend/src/test/java/com/odde/donut/controllers/MemoryTrackerQuestionPersistenceControllerTest.java)

### 4. Reuse a batch question that still matches its generation input

Type: Behavior
Status: planned

An hourly batch can finish after a note edit. Persist the focus fingerprint when its request body is rendered and carry it to the imported MCQ, including on resume. The normal presentation operation can now reuse valid batch output and renew stale output. Older in-flight requests follow the unknown-source rule.

**Proof.** Use the real request renderer, importer and recall selection with a stubbed provider row. A matching focus serves the imported question without synchronous generation. Editing after rendering and before import serves a current replacement. Repeat the import and inspect the selected ID and persisted row count. Apply the same additive schema rollout rule as slice 1 if the batch request needs a column. [Batch operations](../../../docs/question-generation-batch-operations.md)

### 5. Check a prefetched web card when the learner reaches it

Type: Behavior
Status: planned

On the first foreground activation of an MCQ, display the current server selection. Prefetched or late background results cannot appear as the selected question while the foreground check is pending. Keep navigation, input on an active card, spelling and deliberate retry working. A failed MCQ load offers Retry and Self-review.

**Proof.** Mount the real recall card with two trackers. After background preparation, change the response for the second tracker and advance. Observe the loading state and first visible stem, answer that card, and check the submitted prompt ID. Exercise navigation while background or foreground work is pending and retry after failure. Update the existing prefetch assertion to observe the visible card and continued lookahead after the foreground request settles. Retain the spelling and self-review checks. [Web load states](../../../docs/recall-question-loading.md#question-load-states)

### 6. Make CLI recall fixtures support the question offered for display

Type: Structure
Status: planned

The four MCQ terminal suites share a fixture that serves history and rejects ordinary presentation. Give the answer, contest, escape and guidance scenarios equivalent pending MCQs through both fixture operations. Keep the distinguishing case for slice 7 separate. This enables its change of source while preserving each scenario's own assertions.

**Proof.** With existing product selection, run all four MCQ terminal files plus just-review and spelling. Preserve the displayed stem, available choices, submitted ID/index, contest result, keyboard behavior and pending-second-card feedback. Keep the asynchronous gates attached to the operation delivering the relevant card.

### 7. Use the current question in CLI recall

Type: Behavior
Status: planned

On opening an MCQ, request the server-selected prompt. The CLI uses that stem, choices and ID through answering. When no MCQ can be loaded it follows its existing just-review route. Prompt history continues to describe earlier attempts.

**Proof.** Start recall with different history and current responses. Assert the terminal first shows the current stem, submit its numbered choice, and inspect the exact prompt ID and choice index. Exercise just-review fallback. Retain the interactive outcomes from slice 6. Remove obsolete pending-question selection helpers and sweep for their names once at acceptance.

## Proof ownership and delivery

- Seed examples 1–2 belong to slices 2 and 4, including an existing queue, current-content reuse and late batch import. Slice 3 covers edits during on-demand generation. Example 3 belongs to slices 2, 5 and 7 at the answer, browser and terminal boundaries. Example 4 belongs to slices 2, 5 and 7 at their existing recovery boundaries.
- Execute backend changes with `CURSOR_DEV=true nix develop -c pnpm backend:test_only`, or `pnpm backend:verify` and the ERD export for migrations under the same Nix invocation. These whole-suite requirements come from the [backend testing](../../../.agents/skills/backend-testing/SKILL.md) and [migration](../../../.agents/skills/db-migration/SKILL.md) guidance. Regenerate the SDK only if the wire contract changes.
- Run the Chromium and CLI suites above. For frontend changes run both `vue-tsc --noEmit` and `tsc --noEmit` in the frontend package under [frontend guidance](../../../.agents/skills/frontend/SKILL.md). Follow the changed response through shared E2E setup and run the affected recall, contest and CLI recall journeys identified by the execution consumer trace. Preserve tracker-history and conversation readers' original prompt IDs. The North Star and ADRs already cover content ownership.
- The local observations ran against mocks at the model and client API boundaries. The backend owns proof of the actual source included in generation. Web and CLI own proof of first presentation and submission. No measurement of model quality or production latency is required.
- Apply normal slice acceptance, independent refactoring, formatting, commit and publication gates under [Dough Execute Plan](../../../.agents/skills/dough-execute-plan/SKILL.md). Application deployment uses the ordinary release workflow. This plan authorizes no deployment.

## Concerns

The consumer probes resolved the sequencing concern during planning. The CLI fixture change directly precedes the behavior it enables. Seven slices use one focus-content comparison. No remaining slice-specific concern requires a further refinement pass.
