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

These are surviving behavior and active authorization-refusal proofs. No removal or permanent absence-check project is part of this plan. Existing tests are extended at their real boundaries, not copied into tests of internal helpers.

## Decisive premises and preparation observations

Historical preparation observations remain inspectable in [PREPARATION-OBSERVATIONS.md](PREPARATION-OBSERVATIONS.md), with their exact dated revisions, commands, results and limitations. They establish their stated baseline boundaries rather than planned feature or model quality.

## Ordered slices

Slice statuses record accepted execution proof; publication receipts remain in the execution conversation. Target about five minutes of implementation, focused proof and cleanup. Estimates above five minutes are scrutinized below; ten minutes is the hard decomposition threshold except a stated verification wait. The measured full backend suite is roughly two minutes; a required suite or real-service wait is not reduced by splitting a cohesive audience or transcript contract. Time code work and waits separately, and reassess an actual overrun instead of silently extending it.

### 1. Prepare fixed answered attempts in the isolated route

Type: Structure
Status: done

Sizing: at most five minutes active fixture/setup work; external startup waits remain separate. Reuse the inspected setup rather than repeat it.

Change: Stage the exact proposed guidance temporarily, establish the existing isolated held-app route, and bind the two fixed MCQs to owned trackers. Answer both through the actual answer controller. This immediately enables slice 2; add no production fixture service or practice state.

Proof: The recorded route reaches readiness; exported conversation requests consume the original answered correlation and ambiguous-even-number fixtures. Preserve the actual Answer/RecallLog/tracker baseline and restore the temporary patch before delivery. [First-run setup](REAL-MODEL-OBSERVATION.md#route-and-setup) and [follow-up setup](PRACTICE-FOLLOWUP-OBSERVATION.md#route-and-starting-context) supply inspectable commands, allocation and inputs; coordinator judges reuse.

### 2. Validate explanation, correction and ambiguity

Type: Behavior
Status: done

Sizing: at most five minutes active transcript/rubric work; model waits are separate.

Behavior: Given the fixed answered attempts and learner reasoning, the real conversation reply explains the causal distinction, adapts to corrected interpretation, and examines both defensible answers to the ambiguous even-number question.

Proof: The original c1/c2 and a1–a3 [transcript](REAL-MODEL-TRANSCRIPT.md) and [rubric](REAL-MODEL-OBSERVATION.md#rubric-and-retained-limits) retain their exact original guidance/revision. The follow-up changes only the practice directive, so these explanation/ambiguity observations remain compatible within that boundary; they were not rerun under revised guidance. No efficacy inference. Failed/unavailable quality stops dependent slices 3–11.

### 3. Validate ordinary changed practice and response feedback

Type: Behavior
Status: done

Sizing: at most five minutes active practice protocol/rubric work; model waits are separate. Reuse the two already observed replies.

Behavior: After an explanation, a requested changed example is an ordinary assistant message, withholds the key/reasoning until the learner responds, then gives feedback on that actual response and retains the exchange without changing the original Grade.

Proof: [Follow-up evidence](PRACTICE-FOLLOWUP-OBSERVATION.md) records the exact revised guidance, actual c3 question → c4 learner answer → feedback, later controller message reads and byte-identical Answer/RecallLog/tracker snapshots. Its four seeded c1/c2 messages are starting context only. Coordinator accepted the consuming export, actual question/response/feedback controller reads and unchanged snapshots; G1 is proved here; mounted interaction remains with slices 8–10. Failed/unavailable quality stops production slices 4–11.

The isolated paid route is `CURSOR_DEV=true nix develop -c pnpm e2e:hold --paid-openai`, using the existing conversation flow and manual-testing skill. Both runs were explicitly authorized; six original plus two follow-up AI replies exhaust the eight-reply cap. No further paid call is authorized. Preserve all failed-run evidence and use compatible observations; do not repeat them. Temporary guidance is restored; slice 9 installs the exact base plus ordinary-text practice directive. A material later prompt change needs its relevant authorized real-service observation, without waiving the cap or quality premise.

### 4. Name conversation access ownership coherently

Type: Structure
Status: done

Sizing: about five minutes plus measured required verification/API generation.

Change: Express ownership of conversation access independently of its subject through the existing Ownership association. Preserve the physical column and all existing audience values. Update the callers, builders and generated public contract together. This immediately enables slice 5; introduce no new visibility resource or schema.

Proof: Existing start/message/listing/menu/AI/export controller behavior remains green; regenerate the API if its public property changes and verify affected frontend types. Inspect consumers of the old field and partner labels, including generated types; use one existing access model rather than a parallel predicate. Accepted: `pnpm generateTypeScript`, full `pnpm backend:test_only` (2737 tests; 0 failures/errors, 2 skips), full headless `pnpm frontend:test` (338 files/2151 tests), both frontend `vue-tsc --noEmit`/`tsc --noEmit` and `pnpm openapi:lint`, all under `CURSOR_DEV=true nix develop -c`. Inspected the unchanged audience assertion in `ConversationStartControllerTest.StartConversationAboutRecallPrompt.setsRecallPromptAsSubject`, access/list JPQL, authorization getter and generated property. One-time consumer reading found no old camel/method consumers outside historical planning; the physical FK remains unchanged.

### 5. Protect access to the learner's private attempt help

Type: Behavior
Status: planned

Sizing: about five active minutes to restore scoped audience/entry changes and one confidentiality proof loop, plus the required backend suite. Reuse the parked prototype without restoring its label/unread-reply suffix.

Behavior: Given an answered prompt owned by the learner, starting help assigns their personal access ownership. The learner reads and replies; notebook authorship or membership grants neither direct access nor discovery. Existing shared conversations retain their audience.

Proof: Exercise actual creation, subject/messages, send, export, AI request, mark-read authorization, inbox listing and unread exclusion for learner, author and another circle member. Refuse creating help from another learner's prompt; retain legacy shared reads/replies. One canonical refusal shape plus each channel's delta proves the common access rule. For unread confidentiality use a stored non-null learner message against unauthorized actors, so existing null-sender filtering cannot conceal a leak. Positive incoming-AI unread and truthful private labels belong to slice 6. Fresh tests must prove this actual prefix; the [complete prototype](PRIVATE-ACCESS-PROTOTYPE-OBSERVATION.md) is reusable evidence, not verified restored code.

### 6. Recognize private help and incoming replies in the inbox

Type: Behavior
Status: planned

Sizing: about five active minutes to restore the scoped inbox changes and one list/menu/read proof loop, plus the required backend suite.

Behavior: For the learner's private recall conversation, the inbox identifies its private audience. An actual stored incoming AI reply appears unread to its authorized learner until they mark it read; other notebook participants still cannot discover it. Existing shared/personal conversations keep truthful participant labels.

Proof: Drive the real streamed reply with only OpenAI mocked, then observe the persisted reply, actual UserController menu and mark-read result. List-controller observations distinguish private help from legacy shared/personal audience labels. Correct the common incoming-message predicate to include null-sender AI replies while retaining existing human-message/read semantics and the same access predicate. G5/G6 are receiving obligations: until this slice, ordinary messages remain readable but the existing unread predicate excludes AI replies and the existing partner-label behavior remains. No second visibility rule or notification subsystem. Prove the actual restored suffix, not only the complete earlier prototype.

### 7. Continue the same private attempt discussion

Type: Behavior
Status: planned

Sizing: 5–8 minutes including repeat/overlapping-request handling and the required backend suite; reuse and creation are one identity-consistency rule.

Behavior: Starting help again for the same attempt finds the learner's compatible private conversation and retained messages. An existing shared thread is not misrepresented or repurposed. A different attempt has its own discussion.

Proof: Start-controller fixtures contain private and legacy shared threads; repeated start/reload observes the same private identity and messages and retained shared access. Observe the actual create/reuse operation, including overlapping requests if the chosen implementation admits duplicate creation. Resolve that at the existing transactional owner, without a second conversation registry.

### 8. Open contextual help and return to the same recall

Type: Behavior
Status: planned

Sizing: 5–8 minutes including mounted proof/typechecks; one shared entry journey.

Behavior: From an answered MCQ or a particular history item, the learner opens clearly labelled private help with the original attempt visible. Closing it returns focus and preserves the underlying recall/history position. Multiple history items each target their own attempt.

Proof: Mounted real parent pages/components use real conversation content and mock only HTTP. Observe chosen prompt ID, original question/selected answer, audience label, distinct history controls, and preserved current answer/queue after close. Reuse Modal and existing answer display; keep the same interaction keyboard-operable and give it a useful accessible name. Message Center subject rendering continues to work. A fixture that pre-supplies restored state is not proof of restoration; drive open and close in the same mounted parent.

### 9. Explain the learner's reasoning and accept clarification

Type: Behavior
Status: planned

Sizing: about five minutes of change plus backend/frontend verification.

Behavior: The learner can request an explanation immediately or provide why a choice seemed right. The reply addresses that reasoning and the original attempt; the learner can correct a tentative interpretation in a further turn.

Proof: Install the guidance observed in slices 2 and 3 at the existing recall conversation request owner. Controller request capture proves original question, selected answer, tested focus, rationale, current note context and actual learner message reach the external boundary. A mounted conversation exercises both the explanation shortcut and free reasoning/disagreement through ordinary send/reply. Persisted replies survive reload. Guidance distinguishes uncertain evidence from learner failure. Any material prompt change from the retained observations repeats its relevant real-service observation; keyword assertions alone prove no teaching quality.

### 10. Check the distinction without another Grade

Type: Behavior
Status: planned

Sizing: about five minutes of change plus joined-flow verification.

Behavior: After an explanation, an explicit optional check asks one changed example about the same distinction. The learner responds before feedback, can discuss it further or leave, and can later reopen the question/response/ feedback exchange. Their original Grade, tracker and schedule stay authoritative.

Proof: Mounted conversation/recall parent drives explanation → check request → question → learner response → feedback → close/reopen, with scripted HTTP/model responses only for interaction proof. Controller message-loop proof reloads the transcript from storage and observes the same original Answer/RecallLog and tracker state across practice. The real model rubric in slice 3 owns question quality and answer withholding. Practice uses ordinary messages, not the recall answer endpoint, a regenerated prompt, or a structured exercise state machine.

### 11. Carry a useful clarification into authorized refinement

Type: Behavior
Status: planned

Sizing: 5–8 minutes; one contract change across its UI and AI consumer.

Behavior: When the learner wants to refine a note they may edit, the existing refinement receives the original selected answer and a clarification the learner explicitly chooses to carry, along with the existing question/focus context. Existing previews and author permissions govern the resulting content changes.

Proof: Mounted handoff observes the reviewed clarification and selected answer in the actual refinement request; AiController-level capture reaches the refinement model boundary with those values. Retain direct refinement with its ordinary question context and existing permission proof. Regenerate the API for a changed DTO. Do not automatically include the whole private transcript, implement sharing, or repair proposal acceptance/decision storage owned by story 2.

## Verification and delivery

Follow the repository's unit-testing, backend/frontend, generated-API and E2E skills for touched paths. No E2E feature is prescribed merely to duplicate controller and mounted-parent proof. The early real-model observation is a distinct quality claim and cannot be replaced by those tests.

- Backend slices: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`; the backend skill requires the complete backend suite, not a selected class.
- Frontend iteration: focused `pnpm frontend:test <specs>` under the same Nix prefix; before accepting changes, run the frontend suite and both `pnpm -C frontend exec vue-tsc --noEmit` and `pnpm -C frontend exec tsc --noEmit`, per the frontend skill. Reuse identical accepted typecheck evidence rather than rerunning it without a change.
- Public contract changes: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`; never hand-edit generated files. Added/removed Vue components require the frontend skill's Vite build to refresh `components.d.ts`.
- Execution delivery follows AGENTS.md: Jidoka, fresh post-change-refactor agent, API generation when needed, coordinator's single `./scripts/run.sh pnpm format:changed`, plan update, independent check-only commit hook, commit and authorized push. This plan supplies no execution mode or publication claim.

## Current decisions and learnings

On 2026-10-12, original slice 1 accumulated about 10–11 minutes active work: roughly seven for the first setup/observation and 3–4 for practice recovery. Fixture preparation, route recovery and two quality loops disproved its five-minute setup/rubric premise and crossed the ten-minute hard limit; external startup/model waits are separate. Attempt-owned Java was restored and both holds shut down before refinement. Split that leaf into setup Structure (1), explanation/ambiguity (2), and practice (3); retain original production leaves 2–8 as 4–10. The common ordinary-transcript/access design and source scope are unchanged; no new exception, responsibility, or story split is justified.

- Keep the L-sized story: accepted slices 1–4 delivered isolated setup, compatible model proof and coherent access naming. The remaining common transcript/audience model is understood. This separate access/inbox overrun justifies one bounded split; no numeric repeated-overrun threshold is supplied. Eleven leaves do not add scope or machinery.
- Slices 2 and 3 have accepted quality proof. Preserve it; any material later prompt change still needs relevant authorized real-service observation.
- Adding dedicated practice cards, mastery state, or author sharing changes this sizing premise. Return to story refinement and consider the vertical split "understand privately and resume" / "verify understanding with a follow-up" if needed; do not enlarge the plan silently.
- The later contest/repair candidates are unqueued and unplanned. Their unresolved verdict authority and historical reach do not hold this story's scope open.

On 2026-10-12, the later private-audience slice 5 accumulated 10–11 active minutes across eight access consumers, proof writing and the existing unread defect. The [parked complete prototype](PRIVATE-ACCESS-PROTOTYPE-OBSERVATION.md) passed 2763 backend tests (0 failures/errors, 2 skips), but its overrun prevents acceptance/delivery. The coordinator parked only six attempt-owned paths and restored clean published `98996efafdb7847e36602aa5e477fe70d8d33255` before this refinement. Split confidentiality into slice 5 and the cohesive inbox label/incoming-reply loop into slice 6; previous remaining slices 6–10 become 7–11. No privacy channel is deferred. G5/G6 honestly own the unread/label suffix; each restored prefix needs current proof.

The smallest authorized outcome remains private explanation/practice with later reading and recall return. Audience-only delivery without the inbox suffix would abandon its planned observation. Reuse the common incoming-message predicate and existing labels, rather than introducing per-help rules or a notification resource. The source, scope, queue and architectural direction are unchanged; the unrelated fixture and inbox overruns are preserved, not erased by splitting.

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
Disposition: receiving slice 8

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
Disposition: receiving slice 6

### G6. Explicit private audience label awaits the inbox suffix (reported in pre-refinement slice 5)
Reported: slice 5 — "The private-access prefix retains existing partner labels; explicit private-help labelling remains for the inbox slice."
Story clause: "From the answered MCQ, request help with the original attempt visible and the conversation audience clear."
Disposition: receiving slice 6
