# Resolve a confusing recall answer privately and check understanding

**Identity:** SEED-073#story-1

**Source:** [refined story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#story-1)

## Goal and scope

A learner can open help from an answered MCQ, discuss the actual reasoning privately, try an optional related question, and continue recall or revisit that exchange. Preserve the attempt, memory tracker, and original Grade.

The owner accepted on 2026-10-12:

- learner-private help, with deliberate sharing reserved for later author correction work;
- explanation and conversational practice together; and
- separate reason-aware contest and grade repair, recorded as unqueued SEED-073#story-6 and #story-7. An invalid attempt's neutral exclusion is the accepted direction for that later policy, not a scheduling change here.

This plan includes usable entry, same-attempt continuation, private access, reasoning-aware explanation, ungraded conversational practice, and contextual exit to existing authorized refinement. It adds no mastery tracker, diagnostic taxonomy, new chat system, sharing mechanism, or grade-repair policy. Story 2 owns repairing recall-chat correction acceptance and durable proposal decisions. Existing shared discussions keep their audience. Execution retains the established Taken claim; this refinement adds no scope or execution authority.

## Architectural approach and existing solutions

One conversation owns the exchange; its subject identifies the attempt and its access ownership identifies its audience. Existing `Ownership` already supports a person's personal ownership independently of a notebook. The Java/API concept `accessOwnership` names access independently of subject, retaining its physical FK mapping and stored values. New help will use the initiating learner's personal ownership. There is no schema migration or separate help-mode flag.

Authorize help creation through the prompt's MemoryTracker owner, not merely note readability. Continue a conversation for the same prompt and initiator only when its access ownership is that initiator's personal ownership. An old own-note conversation with that audience is compatible; an old shared thread remains shared. Existing authorization, listing, unread and message owners then enforce one audience rule. Repeated entry uses that same thread; this is not a universal one-conversation-per-note rule.

Use the existing Modal and conversation content for contextual help at the answered attempt. Closing returns to the underlying recall/history view, which avoids introducing a second navigation/resume mechanism. Keep the Message Center as an additional later-reading surface. Reuse the question display and ordinary composer; learner reasoning is optional, with an explicit explanation shortcut. A practice request, response, and feedback are ordinary messages. No regenerated RecallPrompt or answer submission represents practice.

The existing attempt serialization already carries choices, selected answer, marked result, tested focus, and rationale. The common focus-context service provides current accessible notes. Preserve that distinction between the original attempt and present source context. Guidance addresses learner reasoning, allows disagreement and uncertainty, and offers one related check without exposing its answer before the learner responds.

Refinement context can extend the existing question-context contract with the selected answer and an explicit learner-chosen clarification. Do not implicitly publish a private transcript. Existing author permissions and previews own any content change. Retain question objections as discussion. The formal post-answer contest action, its verdict and actual consequences belong to story 6; grade repair belongs to story 7. Neither is smuggled into conversational agreement.

Applicable Accepted decisions:

- [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md): distinct MCQ, prompt, Answer, Grade, and Conversation concepts.
- [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md): Grades are scheduling events; explanation/practice keeps the original Grade, schedule and tracker. No compensating Grade.
- [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md) and [NORTH-STAR: one accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary): private learning evidence stays server-side; reviewed note changes use the existing publication owner.
- [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md): use named routes for the retained Message Center/navigation integration.
- [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md): failures remain visible. An interrupted reply is not a successful check. Reuse current request/error handling; add recovery only for a promised business outcome.

No new North Star topic or ADR is warranted for this bounded reuse. Future grading correction must settle its own ADR 0003 policy and historical reach.

## Outside-in proof ownership

| Promise from the source | Owner | Observable proof |
| --- | --- | --- |
| Explanation addresses the chosen distractor and supplied reasoning; learner can correct a tentative interpretation | 2, 9 | Real model transcript rubric in the early probe; controller captures the consumed AI request; mounted conversation sends reasoning and disagreement and shows returned replies |
| Private help is for the attempt owner; notebook owner/circle membership does not grant it | 5 | Controller creation, direct subject/messages/send/AI/export/mark-read authorization and inbox/unread exclusion for learner, notebook owner and another member; retained legacy shared access proves compatibility |
| Private help is identifiable for later reading; incoming replies remain unread until marked read | 6 | List-controller audience labels plus actual streamed assistant-message persistence, UserController unread menu and mark-read, retaining existing human-message behavior |
| Same-attempt continuation and retained exchange | 7, 10 | Repeated start returns the compatible thread; shared older thread retains its audience; DB reload returns question, practice answer and feedback |
| Clear entry from answer/history, original context, optional reasoning, easy return to unchanged recall progress | 8 | Mounted real recall/history parent with backend HTTP mocked opens help for the selected attempt; two history items have distinct controls; closing restores the same answer/session and focus |
| Related practice waits for response, then gives feedback; original Grade and schedule remain authoritative | 3, 10 | Real transcript rubric plus controller message loop with actual pre/post tracker and RecallLog; mounted explicit check action, response, feedback and close/reopen journey |
| A note problem can reach existing refinement with relevant context under author permission | 11 | Mounted handoff supplies selected answer and reviewed clarification; AiController request capture observes those values at the refinement model boundary; existing ordinary refinement remains available |
| Suspected question ambiguity is examined and retained without presenting conversation as formal adjudication | 2, 9 | Ambiguity rubric and persisted reasoning/disagreement exchange; formal post-answer action is owned by story 6 |
| Evidence stays private/server-side, and source uncertainty is not portrayed as learner failure | 5, 9, 11 | Access refusal and explicit refinement payload boundaries; request contract and real model ambiguity case; no new Portable-content store or auto-write path |
| Reading visible messages clears unread state through one owner | 12 | Mounted help, Message Center and NoteConversation fetch/stream completion consume the read response and update the same store |
| Refinement dismissal retains contextual help and recall state | 13 | Real parent with nested native dialogs observes child Escape dismissal then parent close/focus |
| Spelling refinement stays ordinary; MCQ handoff remains truthful | 14 | Positive spelling history/help/refinement consuming request and preserved reviewed MCQ request |

These are surviving behavior and active authorization-refusal proofs. No removal or permanent absence-check project is part of this plan. Existing tests are extended at their real boundaries, not copied into tests of internal helpers.

## Decisive premises and preparation observations

Historical preparation observations remain inspectable in [PREPARATION-OBSERVATIONS.md](PREPARATION-OBSERVATIONS.md), with their exact dated revisions, commands, results and limitations. They establish their stated baseline boundaries rather than planned feature or model quality.

## Ordered slices

Slice statuses record accepted execution proof; publication receipts remain in the execution conversation. Target about five minutes of implementation, focused proof and cleanup. Estimates above five minutes are scrutinized below; ten minutes is the hard decomposition threshold except a stated verification wait. The measured full backend suite is roughly two minutes; a required suite or real-service wait is not reduced by splitting a cohesive audience or transcript contract. Time code work and waits separately, and reassess an actual overrun instead of silently extending it.

### 1. Prepare fixed answered attempts in the isolated route

Type: Structure
Status: done
Proof: Accepted behavior, sizing and consuming evidence remain at [slice 1](ACCEPTED-SLICE-PROOF.md#slice-1); its proof boundary is unchanged.

### 2. Validate explanation, correction and ambiguity

Type: Behavior
Status: done
Proof: Accepted behavior, sizing and consuming evidence remain at [slice 2](ACCEPTED-SLICE-PROOF.md#slice-2); its proof boundary is unchanged.

### 3. Validate ordinary changed practice and response feedback

Type: Behavior
Status: done
Proof: Accepted behavior, sizing and consuming evidence remain at [slice 3](ACCEPTED-SLICE-PROOF.md#slice-3); its proof boundary is unchanged.

### 4. Name conversation access ownership coherently

Type: Structure
Status: done
Proof: Accepted behavior, sizing and consuming evidence remain at [slice 4](ACCEPTED-SLICE-PROOF.md#slice-4); its proof boundary is unchanged.

### 5. Protect access to the learner's private attempt help

Type: Behavior
Status: done
Proof: Accepted behavior, sizing and consuming evidence remain at [slice 5](ACCEPTED-SLICE-PROOF.md#slice-5); its proof boundary is unchanged.

### 6. Recognize private help and incoming replies in the inbox

Type: Behavior
Status: done
Proof: Accepted behavior, sizing and consuming evidence remain at [slice 6](ACCEPTED-SLICE-PROOF.md#slice-6); its proof boundary is unchanged.

### 7. Continue the same private attempt discussion

Type: Behavior
Status: done
Proof: Accepted behavior, sizing and consuming evidence remain at [slice 7](ACCEPTED-SLICE-PROOF.md#slice-7); its proof boundary is unchanged.

### 8. Open contextual help and return to the same recall

Type: Behavior
Status: done
Proof: Accepted behavior, sizing and consuming evidence remain at [slice 8](ACCEPTED-SLICE-PROOF.md#slice-8); its proof boundary is unchanged.

### 9. Explain the learner's reasoning and accept clarification

Type: Behavior
Status: done
Proof: Accepted behavior, sizing and consuming evidence remain at [slice 9](ACCEPTED-SLICE-PROOF.md#slice-9); its proof boundary is unchanged.

### 10. Check the distinction without another Grade

Type: Behavior
Status: done
Proof: Accepted behavior, sizing and consuming evidence remain at [slice 10](ACCEPTED-SLICE-PROOF.md#slice-10); its proof boundary is unchanged.

### 11. Carry a useful clarification into authorized refinement

Type: Behavior
Status: done
Proof: Accepted behavior, sizing and consuming evidence remain at [slice 11](ACCEPTED-SLICE-PROOF.md#slice-11); its proof boundary is unchanged.

### 12. Acknowledge messages read through every conversation viewer

Type: Behavior
Status: done

Sizing: about five active minutes; hard ten, excluding required suite waits. One read-success owner covers contextual help, Message Center and direct NoteConversation consumers.

Behavior: Successfully fetching visible stored messages acknowledges them through the existing backend read rule and updates the common unread store. Fetch failure does not acknowledge unseen messages. Existing audience rules and unrelated unread replies remain authoritative.

Proof: The [runtime baseline](CONSUMER-BASELINE-OBSERVATIONS.md#read-acknowledgement) observes reply 75 staying unread. Accepted `ConversationInner` successful fetch → read HTTP → authoritative unread-store replacement is observed by `RecallPage.privateHelp`, `RecallPage.practice` and `ConversationReading`: stored replies, SSE completion, actual Message Center/NoteConversation, unrelated unread retention and failed-fetch preservation. `CURSOR_DEV=true nix develop -c pnpm frontend:test`: 344 files / 2,161 tests; both frontend typechecks pass. Initial setup/import and unmocked-request failures were corrected before the passing rerun. Fresh refactor: none — already clean, proof unchanged. Active work eight–nine minutes excluding verification waits; single formatter passed. One-time product reading found zero obsolete fetched-event relay consumers; no permanent removal assertion was added. Existing controller read/privacy proof remains applicable.

### 13. Dismiss refinement while retaining contextual help

Type: Behavior
Status: done

Sizing: about five active minutes; hard ten, excluding required suite waits. One top-dialog dismissal rule at the existing modal owner.

Behavior: Escape dismisses the active refinement dialog while contextual help remains open; subsequently closing help preserves recall/history state and returns focus. Standalone refinement close/extraction stays usable.

Proof: The [runtime baseline](CONSUMER-BASELINE-OBSERVATIONS.md#nested-dismissal) observes both dialogs closing. Accepted `RecallPage.privateHelp` now drives actual answer → help → refinement → child Escape, observes child detachment and retained conversation/answer, then parent Escape preserving queue/progress/route/focus. `RefineNoteModal.extractNote.close` drives actual checkbox/preview/Create API, distinct created-note navigation and native host closure; loading cancellation retains the host and retry. `CURSOR_DEV=true nix develop -c pnpm frontend:test`: 344 files / 2,163 tests; final strengthened assertions: two files / five tests; both final typechecks pass. `pnpm cy:run --spec e2e_test/features/assimilation/note_refinement.feature` under Nix passed all four scenarios and retired its runner. Initial focused capitalization mismatch was corrected. Fresh refactor: none — already clean, all proof unchanged. Seven active minutes excluding waits; single formatter passed. One-time consumer reading found no refinement marker selectors coupled to Daisy open classes; shared Daisy helpers retain book/admin/loading callers.

### 14. Preserve truthful context for retained MCQ and spelling conversations

Type: Behavior
Status: planned

Sizing: about five active minutes; hard ten, excluding required suite waits. Actual MCQ context governs the new UI practice action and reviewed handoff; the existing spelling request owner is preserved.

Behavior: A real MCQ supplies the validated guide, optional check and reviewed selected-answer handoff. A supported spelling conversation retains its ordinary user-history request and authorized ordinary refinement flow. The new MCQ-specific UI actions use actual MCQ context; ordinary conversation remains available.

Proof: The [spelling refinement baseline](CONSUMER-BASELINE-OBSERVATIONS.md#spelling-refinement) consumes a clarification-only DTO; the [actual controller export](CONSUMER-BASELINE-OBSERVATIONS.md#actual-spelling-conversation-export) settles the existing ordinary spelling request as user history only. Positively observe corrected ordinary spelling refinement plus the MCQ explanation → optional check → feedback action and original stem/choices/selected answer/final reviewed clarification at the mounted consuming boundary. Reuse unchanged controller export/MCQ request proof; backend request applicability is not a reproduced defect. Preserve exact validated MCQ guide; no paid calls, new spelling source context or model-quality promise. Run frontend suite and both typechecks; backend suite/generation only if those boundaries change. Proof names retained behavior, not inapplicable artifacts' absence.

## Preparation assessment for remaining consumers

The coordinator accepted four [actual baselines](CONSUMER-BASELINE-OBSERVATIONS.md) at `5c6243073aa710c6d39db0decc44fc0dfa3ba017`: contextual unread state, nested Escape, spelling refinement and controller export. They settle the cheap runtime premises and correct the earlier MCQ-guide reach inference; the initial console-log setup failure remains separately recorded. Correction proof, including actual Message Center/NoteConversation consumers, remains with 12–14. Scope, source, claim, accepted 1–11 evidence and quality protocol are unchanged; no remaining preparation concern was identified.

## Verification and delivery

Follow the repository's unit-testing, backend/frontend, generated-API and E2E skills for touched paths. No E2E feature is prescribed merely to duplicate controller and mounted-parent proof. The early real-model observation is a distinct quality claim and cannot be replaced by those tests.

- Backend slices: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`; the backend skill requires the complete backend suite, not a selected class.
- Frontend iteration: focused `pnpm frontend:test <specs>` under the same Nix prefix; before accepting changes, run the frontend suite and both `pnpm -C frontend exec vue-tsc --noEmit` and `pnpm -C frontend exec tsc --noEmit`, per the frontend skill. Reuse identical accepted typecheck evidence rather than rerunning it without a change.
- Public contract changes: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; never hand-edit generated files. Added/removed Vue components require the frontend skill's Vite build to refresh `components.d.ts`.
- Acceptance reading for any removed event relays/wrapper: sweep the product once for removed names and classify retained shared callers; record this separately from positive read/dismissal/refinement tests, without a permanent absence test.
- Execution delivery follows AGENTS.md: Jidoka, fresh post-change-refactor agent, API generation when needed, coordinator's single `./scripts/run.sh pnpm format:changed`, plan update, independent check-only commit hook, commit and authorized push. This plan supplies no execution mode or publication claim.

## Current decisions and learnings

Review on 2026-10-12 at `5c6243073aa710c6d39db0decc44fc0dfa3ba017` found joined consumers missing from accepted proof: contextual unread acknowledgement, nested refinement Escape, and spelling refinement receiving a clarification-only MCQ context. The coordinator retained this active execution and authorized leaves 12–14 before final handoff. A provisional unqueued follow-up draft/SEED-073#story-8 was removed only from attempt-owned paths and superseded by this decision; no new identity, queue item or execution was retained. Slices 1–11 keep their accepted evidence; no completion record exists. CI remains pending under the retained observer.

Earlier sizing decisions, both bounded overrun refinements, restored prototypes and retained architectural limits remain in [accepted execution history](ACCEPTED-SLICE-PROOF.md#earlier-execution-refinements). Their dates, revisions and failed/partial-proof limits are unchanged; leaves 12–14 add only the existing consumers needed for the same outcome.

## Execution observation: real-model gate

Observed 2026-10-12 at `5d6307f63d716bd99b275bd2df88f3cca1f5499b`, the established Story Branch claim on `origin/main`; branch `codex/resolve-a-confusing-recall-answer-with-an-explan`, publisher `dashboard-territory.local-doughnut`, agent Ai-chan, starting revision `05b3651f2d9fb7e6f16a3e34447be602db4cc3b3`.

The [original observation](REAL-MODEL-OBSERVATION.md) and [six-reply transcript](REAL-MODEL-TRANSCRIPT.md) retain all dated failed-run inputs, guidance and returned tool arguments. Explanation, corrected interpretation and ambiguity passed; original c3 returned only a `GeneratedMcq` payload with rationale, persisted no question, and left feedback unobserved. The [authorized two-reply follow-up](PRACTICE-FOLLOWUP-OBSERVATION.md) added only an ordinary-text practice directive: a changed umbrella/traffic-accident question withheld its key, then specific feedback on actual learner choice 2 persisted with the question and response. All eight replies used `gpt-4.1-mini`; none remain authorized.

Earlier explanation/ambiguity results retain their original-guidance provenance and were not rerun. Follow-up seeded history proves no new explanation persistence. Both observations preserve matching pre/post Answer/RecallLog/tracker snapshots at their respective dates. The temporary product patch is restored and held stacks stopped. Coordinator accepted slices 1–3 from these inspected observations; G1's HTTP quality gap is proved, while production guidance, privacy, mounted interaction and joined continuation still need their assigned proof. Neither observation establishes representative-user efficacy or lasting mastery.

## Story obligations

### G1. Persisted conversational practice and feedback (first reported in original slice 1)
Reported: slice 3 — "Practice returned only a GeneratedMcq tool call containing its rationale; the question never became a persisted ordinary message. Feedback was therefore unobserved."
Story clause: "Keep the question, learner response, and feedback in the conversation."
Disposition: proved by slice 3: PRACTICE-FOLLOWUP-OBSERVATION.md, actual c3/c4 consuming requests, persisted new question/answer/feedback and unchanged Grade snapshots

### G2. Mounted interaction remains unobserved by this probe (first reported in original slice 1)
Reported: slice 1 — "HTTP evidence proves the returned payload and missing persistence; UI rendering was not inspected."
Story clause: "Returning to recall preserves the learner's progress."
Disposition: proved by slice 8: RecallPage.privateHelp.spec.ts actual answer/open/close and Escape preserve rendered answer, progress, queue, route and focus in the same mounted parent

### G3. Illustrative observation has no efficacy inference (reported in original slice 1)
Reported: slice 1 — "Illustrative fixed cases, one run; no representative learner or efficacy claim"
Story clause: "A learner confused by an answered MCQ can understand the disputed distinction, try applying it, and return to recall without having to reconstruct the attempt or immediately reorganize the note."
Disposition: no user cost "A learner confused by an answered MCQ can understand the disputed distinction, try applying it, and return to recall without having to reconstruct the attempt or immediately reorganize the note.": the story promises a usable explanation/practice workflow, not demonstrated population efficacy or lasting mastery.

### G4. Startup cause remains provisional (reported in original slice 1)
Reported: slice 1 — "Startup first attempt cache packing failed during concurrent watcher/bootRun output; second sequentially precompiled start passed"
Story clause: "A learner confused by an answered MCQ can understand the disputed distinction, try applying it, and return to recall without having to reconstruct the attempt or immediately reorganize the note."
Disposition: no user cost "A learner confused by an answered MCQ can understand the disputed distinction, try applying it, and return to recall without having to reconstruct the attempt or immediately reorganize the note.": the held app reached owned readiness for the observation; a permanent development-runner repair is outside this product outcome, and the uncertain cause grants no waiver of future setup or proof.

### G5. Incoming AI replies are omitted from unread messages (reported in pre-refinement slice 5)
Reported: slice 5 — "unread JPQL has cm.sender != :user, which excludes null-sender AI replies"
Story clause: "Retain the explanation and practice exchange for later reading."
Disposition: proved by slice 6: PrivateRecallConversationControllerTest.learnerRequestsAndReadsAnAiReply, persisted AI reply in UserController menu followed by mark-read and empty menu; author/member assistant discovery refused

### G6. Explicit private audience label awaits the inbox suffix (reported in pre-refinement slice 5)
Reported: slice 5 — "The private-access prefix retains existing partner labels; explicit private-help labelling remains for the inbox slice."
Story clause: "From the answered MCQ, request help with the original attempt visible and the conversation audience clear."
Disposition: proved by slice 6: PrivateRecallConversationControllerTest list-controller observations distinguish private help from legacy circle and two-person participant labels

### G7. Contextual reading leaves retained replies unread (retrospective of slices 6/8)
Reported: slice 8 — "The contextual help entry does not consume conversation-fetched, so visible stored replies remain unread."
Story clause: "Retain the explanation and practice exchange for later reading."
Disposition: proved by slice 12: RecallPage.privateHelp.spec.ts, RecallPage.practice.spec.ts and ConversationReading.spec.ts actual successful visible fetch/SSE persisted refetch consume read HTTP and update the common store while retaining unrelated unread replies; failed fetch preserves unread state

### G8. Nested refinement Escape closes contextual help (retrospective of slices 8/11)
Reported: slice 11 — "The raw refinement dialog is outside modalStack; Escape pops contextual help and discards both dialogs."
Story clause: "Returning to recall preserves the learner's progress."
Disposition: proved by slice 13: RecallPage.privateHelp.spec.ts actual nested native child Escape retains help and later parent Escape preserves answer, queue, progress, route and focus; RefineNoteModal.extractNote.close.spec.ts actual extraction/create closes native host and loading cancellation retains retry

### G9. Retained spelling refinement receives an incomplete MCQ context (retrospective of slice 11)
Reported: slice 11 — "Spelling help enables clarification review without an MCQ and sends a clarification-only DTO described as an answered MCQ."
Story clause: "Ordinary authorized refinement remains available."
Disposition: receiving slice 14

### G10. Ordinary spelling export supplies user history only (preparation for slice 14)
Reported: slice 14 — "The valid spelling conversation export contains only the learner's user message; the current subject-note resolver supplies no note/developer context."
Story clause: "A learner confused by an answered MCQ can understand the disputed distinction, try applying it, and return to recall without having to reconstruct the attempt or immediately reorganize the note."
Disposition: no user cost "A learner confused by an answered MCQ can understand the disputed distinction, try applying it, and return to recall without having to reconstruct the attempt or immediately reorganize the note.": the selected outcome concerns an answered MCQ. The observed ordinary spelling request is existing behavior, preserved by this execution; broader spelling source/model quality is outside that outcome. This does not waive slice 14's positive ordinary spelling refinement compatibility proof or add a new teaching-quality obligation.
