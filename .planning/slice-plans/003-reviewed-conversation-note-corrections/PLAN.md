# Save reviewed conversation corrections to the subject note

**Identity:** SEED-073#story-2
**Source:** [Selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#story-2)

## Goal and scope

An author reviews a useful chat clarification into the existing subject note,
then reopens the conversation to inspect the proposal, decision, reviewed text,
and accepted Git revision. Terry selected review within the conversation on
2026-10-14. Cover direct-note and answered-MCQ conversations, content and title
suggestions, explicit decline, pending reopening, stale review, and retry of a
confirmed decision. Preserve ordinary edit rights and conversation audiences.

Keep private exchanges outside Portable content and preserve recall identities,
history, and scheduling. New-note creation, coordinated multi-note corrections,
a general review inbox, automatic rewriting, undo, a Git history browser, and
keystroke autosave are deferred. Ordinary title/reference rules remain owned by
existing note editing. No implementation or application release is authorized
by this preparation.

## Existing owners and design

- `Conversation`, `ConversationMessageController`, and `ConversationService`
  own conversation access, source messages, and reopening. Extend that domain
  with a durable correction proposal; do not store authoritative decisions in
  browser-only tool state or invent a general event/workflow framework.
- `NoteConversationAiReplyService`, `ConversationAiReplySseStream`, and the
  Responses-to-chat mapper own streamed assistant output. Preserve normal text
  and existing exclusion tools while capturing completed note suggestions with
  their reason, originating exchange, request-time subject and accepted base.
  Correlate a proposal by server identity and tool call within its response,
  not by the mock fixture's constant call ID across conversations.
- `Conversation.getSubjectNote()` already supplies the domain resolution for
  direct-note and MCQ subjects. The acceptance operation resolves and authorizes
  that subject on the server. The browser reviews the supplied subject; it does
  not choose an arbitrary note ID or infer an MCQ note from a direct-note field.
- `AiResponse`, `ToolCallHandler`, and the conversation reading surface own
  visible suggestions. Show one persisted proposal through its lifecycle;
  avoid duplicate transient and persisted cards for the same correction.
  Use ordinary Markdown editing/rendering and loading/error conventions.
- `WebNoteEditService`, `AuthoredNoteDocumentPersistence`, and
  `AcceptedWebChangeService` own note validation, mutation, notebook locking,
  projection, and Git publication. Reuse them. Keep proposal transition and
  publication in one transaction, capture the resulting head before releasing
  its lock, and return that stored decision on a repeated request. Do not save
  the note and then log acceptance with a second browser request.

A correction retains its source exchange, subject identity, proposed field/value,
reason, reviewed base Git revision, and pending/declined/accepted decision.
Acceptance retains reviewer, decision time, reviewed value, and actual resulting
revision. These are review evidence, not an alternative content-version store.
Git remains content authority. A later ordinary edit changes neither the decision
nor its original revision. An unchanged reviewed value may reuse the existing
accepted revision through ordinary publication semantics.

The common rule is one proposal lifecycle for either changed field and either
conversation subject. Body and title application use their existing domain
operations; title reference handling is not reimplemented. Conversation access
and current note edit permission are separate checks. Decline is explicit;
closing a view preserves pending state. Only completed server decisions render
as accepted/declined. After a stale refusal, retain the editor's current text,
load current note content/base, and require another explicit review/acceptance.
Never merge or silently resubmit against a newer head.

The ADR index and in-file statuses agree on Accepted records 0000–0007; no
supersession conflict was found. ADR 0001's non-suffixed filename is a naming
hygiene mismatch only, not a status ambiguity. Relevant constraints:

- [ADR 0002 — Git-native Portable notebook tree synchronization](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md):
  one atomic publication boundary, Git identities/revisions, stale refusal,
  private learning evidence outside Portable content.
- [ADR 0001 — Ubiquitous language](../../../docs/adrs/0001-ubiquitous-language.md#conversation):
  conversations have note or recall-prompt subjects; the proposal belongs to
  that conversation, not to a parallel review system.
- [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md#usage):
  deliberate stale/retry feedback owns its business recovery; unrelated failures
  retain ordinary loud failure behavior.
- [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  preparation observations and implementation proof use isolated test state.

Follow [One accepted-change boundary](../../NORTH-STAR.md#one-accepted-change-boundary).
No new North Star topic or ADR exception is needed.

## Preparation observations and limitations

Observed on 2026-10-14 at `3aceb37f36dfe3b1e1979026b78f30b658bb0c28`, in
`/Users/terryyin/git/doughnut/.worktrees/save-a-useful-chat-clarification-as-a-reviewed-n`.
All scratch changes were reverted before preparation writes. No paid model
calls, production data, or Development data were used.

1. **Current note suggestion and refinement handoff.** Ran
   `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/conversation/AiResponse.noteContentCompletion.spec.ts tests/components/conversation/ConversationComponent.refinement.spec.ts`.
   Eight tests passed. The mounted suggestion's Accept reached
   `TextContentController.updateNoteContent` with its proposed body. The mounted
   MCQ handoff sent the explicitly reviewed clarification and selected answer
   to refinement. These are mocked HTTP boundaries, not full-stack publication.
2. **MCQ acceptance gap.** Temporarily changed only the fixture construction in
   `frontend/tests/components/conversation/aiResponseTestSupport.ts` from
   `makeMe.aConversation.forANote(note).please()` to
   `makeMe.aConversation.forAnsweredQuestion(makeMe.anAnsweredQuestion.withNote(note).please()).please()`.
   Ran `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/conversation/AiResponse.noteContentCompletion.spec.ts`.
   Six tests passed; `accepts the completion suggestion and updates the note`
   failed with zero save calls. The suggestion was rendered and Accept was
   pressed, so this reaches the failing handoff rather than just locating it.
   Restored the fixture and repeated observation 1: eight passed. The selected
   solution resolves the subject in the server acceptance operation, rather
   than treating a browser subject lookup alone as the whole story.
3. **Existing publication and message persistence.** Ran
   `CURSOR_DEV=true nix develop -c pnpm backend:test_only` against isolated
   database `doughnut_wt_3201abce9e7f4164a54d9ec6c00eea24_test`:
   2,814 tests, zero failures/errors, two skipped. Inspected results and setup
   for `NotebookGitWebContentHistoryExposureControllerTest` (five passed) and
   `ConversationAiReplyControllerTest.GetAiReply` (one passed). The former
   performs authenticated controller content saves and reads the changed
   Markdown and parent commits from a downloaded Git bundle; the latter
   consumes a mocked Responses stream and reloads persisted assistant text.
   This supports reuse of these operations, not a claim that durable proposal
   capture or decision/publication atomicity already exists.
4. **Type compatibility after restoring scratch.** Both
   `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and
   `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` passed.
5. **Browser proof route.** Ran
   `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/messages/conversation_about_a_note.feature`
   at isolated origin `http://127.0.0.1:63503`, using
   `doughnut_e2e_wt_3201abce9e7f4164a54d9ec6c00eea24` and its runner-owned mock.
   The scenario passed: authenticated note conversation, streamed reply,
   follow-up, focus-context request, and conversation export. The wrapper exited
   successfully. This settles the existing conversation browser proof route;
   the planned correction scenario is new coverage, not claimed passed here.

New proposal persistence, atomic acceptance, and stale recovery are behavior to
implement and prove, not premises claimed satisfied by these baseline runs.
Model correctness is reviewed by the author; deterministic mocked suggestions
prove the workflow. No improvement in live-model quality is claimed.

## Proof ownership and consumers

| Final promise | Owning slice | Observable proof |
| --- | --- | --- |
| Stored proposal, reason, source exchange and correct subject survive refetch | 2 | Real controller stream → fresh conversation/proposal read, using fragmented mocked tool output; direct-note and MCQ data |
| Author sees current/proposed values, edits and reopens a pending review | 3 | Mounted conversation input/close/remount/refetch; unsent edits are not autosave |
| Decline stays visible and authored content is unchanged | 4 | Decline through controller then reload; mounted conversation shows the durable decline |
| One accepted reviewed body and its actual revision are committed together; repeated decisions return it | 5 | Controller acceptance → committed transaction → reload plus downloaded Git body/revision; retry after subsequent publication and concurrency/rollback cases |
| Reviewed acceptance and Open note work from note and MCQ conversations | 6 | Mocked-service Cypress journey plus mounted MCQ dialog, changed note and reopened accepted review; actual Git result from slice 5 |
| Title corrections reuse the same lifecycle and ordinary rename validation | 7 | Controller title acceptance/Git and mounted edited-title decision; existing referenced-title consumer coverage |
| Stale review refuses overwrite, preserves input and permits explicit rereview | 5 refusal, 8 recovery | Controller newer-head conflict; mounted refresh/current-base/accept and transient request failure |
| Initial review waits for fresh current content before accepting | 9 | Mounted cached older realm plus deferred fresh read; acceptance becomes available only after the fresh value is shown |
| Audience and edit rights hold; private transcript stays outside Git; recall identity/history/schedule preserved | 2 reads, 4 decline, 5 acceptance, 6 UI | Real controller authorization scenarios and Git content; original recall state before/after accepted correction; mounted readonly view |
| Later note changes do not change an earlier decision/reference | 5 persistence, 6 rendering | Accept, ordinary edit, reload decision; render original reviewed text/reference |

Consumer inspection: the shared conversation is rendered from direct note,
Messages, and recall help; mounted tests under `frontend/tests/components/conversation`,
`pages/ConversationReading.spec.ts`, `pages/NoteShowPageConversation.spec.ts`, and
recall help/exclusion tests observe it. `aiReplyState.ts` also handles title,
exclusion, and unknown tools; changing stream handling must preserve their
surviving behavior. `noteTextEditing` and standalone completion use the existing
completion type. Keep that API compatible where its purpose remains ordinary
editing. The E2E OpenAI composer fragments tool arguments and uses a constant
mock call ID; extend its domain fixtures without treating that ID as global.
Product-wide search found no CLI/MCP conversation correction consumer.

No product removal is prescribed. If implementation replaces a transient-only
note-review path, transitively remove only its orphaned code/fixtures, prove the
surviving durable review, and record a one-time removed-name sweep separately;
never add absence tests. Existing exclusion and standalone editing behavior stay
with their owners.

## Ordered slices

Execution: story-branch in
`/Users/terryyin/git/doughnut/.worktrees/save-a-useful-chat-clarification-as-a-reviewed-n`,
branch `codex/save-a-useful-chat-clarification-as-a-reviewed-n`, agent Rio-chan,
publisher `dashboard-territory.local-doughnut`. Originating and execution checkout
are this workspace; no integration checkout is supplied. Authorized remote is
`origin`, trunk `main`; increments publish to the execution branch.
Established claim/base: `f9b14b6f0f1824ad8e6fdd5dc65ba860941edd4c`;
starting revision: `9cc7652609b055a208e53a84d374f034a149cf8f`.
Replanning is allowed within the selected story under the existing plan's
overrun rule. Checkout setup `./scripts/run.sh bash scripts/worktree_setup.sh`
and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` passed
on 2026-10-14. The shared pre-commit hook checks staged components without
mutation through `./scripts/run.sh pnpm lint:changed`.

CI observer: GitHub Actions `nerds-odd-e/doughnut`, workflow `ci.yml` (verified
`donut CI`), target `codex/save-a-useful-chat-clarification-as-a-reviewed-n`,
coordinator `rio-story-2`, directory `/tmp/dough-ci-501/watch-ubDtkr`, PID 11074,
session 67431, yielded cell 9, armed from this execution checkout. The claim on
trunk is unobserved; this observer covers branch increments and owned repairs.

Each slice includes implementation, its proof, and local cleanup. Target roughly
five minutes; estimates below are hypotheses including ordinary focused checks.
The 6–9 minute leaves were scrutinized: their extra scope is the one stated
persistence or publication risk, not another user outcome. Over ten minutes of
active work requires finer decomposition; only actual focused-test/external
wait time is an exception. Reassess from evidence on an overrun, not a renamed
retry. Full backend verification is mandatory by the backend skill even when
one controller supplies the focused observation. No whole-product E2E gate is added.

### 1. Prepare storage for conversation correction evidence
Type: Structure
Status: done
Proof: Fresh/upgrade migration succeeds; existing conversation and deletion
behavior remains green in `pnpm backend:verify`; regenerated ERD shows deliberate
FK lifetimes. This enables slice 2 immediately.

Add the smallest private proposal storage needed for the lifecycle described
above, under the existing conversation/message identities. Do not add runtime
reads of that storage in this increment. Preserve existing conversations and
hard-delete behavior; include every new FK child in deletion fixtures as the
unit-testing skill requires. Estimate: 5–7 minutes plus migration test wait.

**Deployment checkpoint:** The db-migration skill documents migrations running
after application readiness. Publish this compatible schema-only increment as
a separately deployable revision. Before any release containing dependent code,
the release operator must deploy that schema revision and verify migration
completion on the target environment, then release the dependent code separately.
This is a release constraint, not authority to deploy from preparation or a
claim that production already has the schema. Development on isolated migrated
test databases can continue. Preserve the schema revision in execution evidence.

Execution evidence (2026-10-14): private `ConversationNoteCorrection` evidence
and migration `V300000354` added without runtime proposal reads. Source-message
and subject-note delete edges cascade; reviewer deletion sets its reference null.
`CURSOR_DEV=true nix develop -c pnpm backend:verify` passed: 2,814 tests,
zero failures/errors, two skipped, isolated schema
`doughnut_wt_a98a02cb445540b68fd7742fd49b8403_test`. Migration initially exposed
signed FK IDs; matching existing unsigned IDs corrected it. Compilation then
required the builder's `beforeCreate` hook; the final command passed after repair.
Inspected controller deletion observations: `NoteControllerPermanentDeleteTests`,
`NotebookFolderPermanentDeleteControllerTest` and their correction-inclusive
`dependentCounts`; `MemoryTrackerDeleteControllerTest`; Git deletion, rollback,
retry, recreation and copy publication consumers with `.withNoteCorrection()`.
Their terminal XML results and `DeletableEntityFkClosureTest` passed.
`CURSOR_DEV=true nix develop -c env DONUT_ERD_SCHEMA=doughnut_wt_a98a02cb445540b68fd7742fd49b8403_test pnpm export:database-erd`
passed and generated all three FK lifetimes. Independent refactor: already
clean; no edits or proof reruns. Canonical PLAN and generated ERD retained despite
the generic 250-line check to preserve their source/layout owners. Coordinator
selective formatting passed. Schema-only commit is the deployment checkpoint;
its accepted SHA will be retained with the next increment.

Accepted schema-only revision: `4347f39efdfd932c7bf5ffec578809bbc5c5dee4`
on `origin/codex/save-a-useful-chat-clarification-as-a-reviewed-n`, suffix base
`f9b14b6f0f1824ad8e6fdd5dc65ba860941edd4c`, CI observation reused. This is the
schema-first deployment checkpoint; no application release was performed.

### 2. Retain generated corrections with their originating exchange
Type: Behavior
Status: done
Proof: From the conversation controller, stream fragmented note-tool arguments,
then fresh-read the stored proposal with reason, source exchange and resolved
subject/base. Exercise note and MCQ subjects; conversation authorization governs
reads. Existing ordinary assistant-text persistence remains green.

Capture complete note correction suggestions server-side and expose them through
the existing conversation API ownership. Preserve text accompanying a tool call
and existing exclusion-tool behavior. A proposed correction writes review evidence,
not note content. Include generated client/fixture updates. Estimate: 7–9 minutes.

Safe boundary: authenticated API readers can inspect durable pending proposals;
the existing browser suggestion display remains until slice 3 replaces that
presentation. This boundary isolates stream-completion/persistence risk before
adding browser review, and does not claim story completion.

Execution evidence (2026-10-14): complete fragmented body/title tools are retained
with one originating assistant message, accompanying explanation, reason,
request-time resolved subject/base and response-scoped call identity. Ordered
conversation reads preserve the preceding request. Controller correction reads
use existing conversation authorization. Inspected
`ConversationAiReplyControllerTest.retainsFragmentedCorrectionAgainstTheRequestSubjectAndBase`
(body/title × direct/MCQ) flush/clear and reread, including a base advanced during
response; other cases cover repeated mock call IDs, empty recall start, exclusion
explanation, and read refusal. Private recall CORRECTIONS authorization cases
passed. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed on final
content: 2,824 tests, zero failures/errors, two skipped, same isolated schema as
slice 1. Two initial compile-only fixture/constructor issues were corrected.
`CURSOR_DEV=true nix develop -c pnpm generateTypeScript` passed; full
`CURSOR_DEV=true nix develop -c pnpm frontend:test` passed (350 files, 2,195 tests),
and both frontend `vue-tsc --noEmit` / `tsc --noEmit` and `pnpm openapi:lint`
passed with the Nix wrapper. Shared streaming, title/unknown/exclusion,
standalone completion, Messages and recall consumers were included.
Independent refactor consolidated private-help participant variants; inspected
test setup/assertions and its full backend rerun confirmed 31 private-recall
cases, retaining the capture/generation/frontend proof unchanged. Coordinator
selective formatting passed. Browser review and decisions retain their later
slice ownership below.

Accepted capture revision: `bcbc4c55b8924a0ef0310e1dea30535b6afb6ced`
on the same execution branch, suffix base `4347f39efdfd932c7bf5ffec578809bbc5c5dee4`;
CI observation reused.

CI observation: capture revision run `38495944351`, attempt 1, job
`115631600434` failed `record_live_audio_with_real_open_ai_service.feature`:
expected English `Please be quiet.`, observed retained note text
`Let's start请保持安静。`. Diagnostic and bounded log inspection establish a
real assertion failure in the unchanged audio transcription path
(`AiAudioController` → `OtherAiServices` → `OpenAiApiHandler.getTranscription`).
The claim-to-capture diff leaves those owners, fixture and feature unchanged;
conversation Responses mapping is a separate API path. This is an unowned
audio-test defect, not an infrastructure excuse or a passed CI result. No
repair or rerun was initiated; independent story implementation continues.

### 3. Review and reopen a pending correction in the conversation
Type: Behavior
Status: done
Proof: Mounted conversation receives stored body/title proposals, renders subject,
reason and exchange, edits the value, closes and reopens to the stored pending
proposal. Cover Messages and recall consumers through the conversation suite.

Render the durable proposal once adjacent to its exchange; expose editable
current/proposed values and explicit pending state. Reuse existing author/read-only
information. Until slices 6/7, the new review does not expose body/title acceptance; ordinary
note editing remains usable. This visible pending review is useful before a
publication decision. Estimate: 5–8 minutes.

Execution evidence (2026-10-14): stored reviews render beside their source
assistant message with note, reason, current and editable proposed body/title,
and pending state. `NoteRealm.notebookRealm.readonly` governs editing. Inspected
`ConversationInner.corrections.spec.ts` mounted direct/MCQ close/remount, title in
Messages, readonly rendering, and stream-completion/follow-up observations.
Stable proposal keys preserve unsent input while the view stays open; remount
restores stored proposal text. Full `CURSOR_DEV=true nix develop -c pnpm frontend:test`
passed (350 files, 2,190 tests); both frontend typechecks passed. Explicit empty
correction-read fixtures repaired the affected existing conversation consumers.
Independent refactor simplified exclusion/unknown handlers, removed orphan CSS,
and corrected form field IDs; its focused three-file conversation command passed
16 tests, with both typechecks. Unchanged full-suite proof covers other consumers.
The whole-product sweep found the existing Cypress completion consumer; returned
it to implementation before acceptance. Its current scenario now proves pending
review → edit → close → original authored content → reopened stored proposal.
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/ai_generated_content/note_content_completion.feature,e2e_test/features/messages/conversation_about_a_note.feature`
passed: two specs, three scenarios, origin `http://127.0.0.1:63203`, isolated
`doughnut_e2e_wt_a98a02cb445540b68fd7742fd49b8403`, runner-owned mocked OpenAI.
Unavailable-AI and ordinary stream/follow-up/export also passed. A fresh final
refactor found no further edits; accepted proof remained unchanged.
Declaration generation initially used `env VITEST=1` to defer checker formatting;
after coordinator selective formatting, ordinary
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vite build --emptyOutDir`
passed including its checker, closing that limitation.

Removal reading at acceptance: searched product sources, tests, fixtures, CLI,
MCP, E2E, docs and guidance for retired `formatCompletionSuggestion`,
`Suggested completion:`, `Suggested title:`, `completion-text`, `title-suggestion`,
`refreshNoteContent`, `AiResponseFixture`, `setupAiResponseFixture`,
`shouldShowCompletion`, `acceptCompletion` and their obsolete step phrases.
No product matches remain. Dated planning baseline provenance stays intact.
Replacement mounted/Cypress review and retained exclusion/unknown/standalone
completion proof establish the surviving behavior; the sweep is a reading.

Accepted pending-review revision: `c27d2aaf2b1b75a7fa82c7bd85477dfa11471a05`
on the execution branch, suffix base `bcbc4c55b8924a0ef0310e1dea30535b6afb6ced`;
CI observation reused.

### 4. Retain an explicit decline
Type: Behavior
Status: done
Proof: The author declines through the controller, rereads the same declined
proposal, and authored content stays unchanged. Mounted review reflects the
server result after reopening; closing without a decision keeps it pending.

Add the decline transition and its UI action to the same proposal. Enforce
conversation access and retain the deciding participant; keep pending state
on a failed decision request. Repeated decline returns the retained result.
Estimate: 5–7 minutes.

Execution evidence (2026-10-14): controller decline locks the proposal, authorizes
its conversation, and records participant/time only while pending; completed
dispositions are returned unchanged. Inspected
`ConversationMessageControllerTest.DeclineCorrection` fresh flush/clear/controller
read retains proposal/source/reason and original note content, plus retry by
another authorized participant, completed acceptance, and authorization/login
refusals. The initial retry fixture selected notebook creator rather than the
conversation ownership user; corrected that audience precondition.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed: 2,829 tests,
zero failures/errors, two skipped, five decline cases, same isolated unit schema.
`pnpm generateTypeScript`, full `pnpm frontend:test` (350 files, 2,194 tests),
both frontend typechecks and `pnpm openapi:lint` passed through the Nix wrapper.
UI uses confirmed server results; mounted decline cases cover direct/MCQ reopen,
failure/input preservation/retry, and readonly participant decline.
Independent refactor split pending/decline specs, shared their setup in
`conversationCorrectionTestSupport.ts`, and added `id`/`declinedBy` makeMe
operations. Inspected moved observations in
`ConversationInner.declineCorrections.spec.ts`; its two-spec replacement command
passed all nine mounted cases, with both typechecks. Backend, generated, other
frontend and Cypress boundaries remained unchanged. Coordinator formatting passed.

Accepted decline revision: `295589e9f1631ba6a0687ad8ea0ef037140e5e2c`
on the execution branch, suffix base `c27d2aaf2b1b75a7fa82c7bd85477dfa11471a05`;
CI observation reused.

### 5. Publish a reviewed body correction and its decision atomically
Type: Behavior
Status: done
Proof: A real controller acceptance followed by a fresh transaction and downloaded
Git bundle yields the edited body and exactly its retained revision/decision.
Prove stale-base refusal, unauthorized acceptance refusal, rollback on failed
publication, repeated acceptance after later ordinary publication, and competing
decisions through this same operation. Compare the recall identity/history/
schedule before and after, and read exported note content to establish privacy.

Expose the server acceptance operation using the current subject and note rights,
existing authored-document validation, and the notebook publication lock and
transaction. Check pending state and reviewed base inside that transaction. Capture
the actual resulting Git head and final reviewed value there. An already completed
proposal returns its recorded disposition; never apply a conflicting later decision.
Avoid an independent browser note-save path. Estimate: 7–9 minutes; stop and refine
if transaction integration requires more active work than that hypothesis.

Safe boundary: the authenticated API offers safe publication, while the browser
review remains pending/declinable until slice 6. This isolates the atomicity and
retry risk and its real-database proof from UI wiring. This is not a claim that the
complete author journey is delivered yet.

Execution evidence (2026-10-14): serializable acceptance locks the correction,
authorizes the conversation and resolves its current subject. Reviewed content
uses ordinary authored validation and `WebNoteEditService.saveReviewedContent`;
the base check runs inside the existing locked publication operation. The outer
transaction retains normalized reviewed value, actual reviewed base, reviewer,
time and resulting binding head. Completed decisions return their durable receipt.
Inspected all ten `ConversationCorrectionAcceptanceControllerTest` scenarios and
their committed test support (`NOT_SUPPORTED`, independent fixture/read transactions).
Direct/MCQ receipt equals downloaded Git head and exact Markdown; tracker identity,
counts, history, schedule, stability/difficulty and prompt/MCQ/answer stay equal.
Exact exported Markdown contains only reviewed authored content. Other cases prove
stale refusal/explicit new-base acceptance, separate audience/edit authorization,
late binding failure rollback/retry, receipts after later ordinary publication,
completed decline, queued real concurrent decisions and unchanged-content head reuse.
Successful rereview retains its actual requested base. Initial test runs exposed
detached fixture ownership and MCQ conversation/prompt cleanup issues; repaired
managed-state setup and complete owned cleanup, respecting answer cascade.
Final `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed: 2,839 tests,
zero failures/errors, two skipped; ten acceptance cases passed on the isolated
unit schema. `pnpm generateTypeScript`, full `pnpm frontend:test` (351 files,
2,194 tests), both frontend typechecks and `pnpm openapi:lint` passed with Nix.
Independent refactor extracted committed fixtures into
`ConversationCorrectionAcceptanceControllerTestSupport`; inspected unchanged
assertions and its full backend rerun (same passing counts). Generated/frontend
proof remains valid. Coordinator selective formatting passed. Server body proof
is complete; browser acceptance and title application retain later ownership.

Accepted atomic-body revision: `7211cc43252e39f8e92e1d46b4030321eb38b434`
on the execution branch, suffix base `295589e9f1631ba6a0687ad8ea0ef037140e5e2c`;
CI observation reused.

### 6. Accept an edited body and reopen its confirmed result
Type: Behavior
Status: done
Proof: Extend `e2e_test/features/messages/conversation_about_a_note.feature` with
a mocked body suggestion → edited review → accept → open changed note → reopen
accepted review journey. Mounted recall conversation drives the same operation
for its MCQ subject, including readonly access and rendering after a later note
edit. UI uses the server decision, not optimistic acceptance.

Connect Accept to slice 5, show confirmed decision/reviewed text/revision and the
existing note destination. Keep input and retry feedback on a request failure;
a lost response is recovered by reading/retrying the same proposal identity.
Keep ordinary text, exclusion proposals, and standalone completion consumers
working. Update `docs/recall-help.md` with the enduring reviewed-correction behavior.
Estimate: 6–9 minutes plus runner startup wait.

Execution evidence (2026-10-14): body Accept submits reviewed text and retained
base through the atomic operation. Only confirmed server dispositions render;
accepted value/revision remain inspectable after reopening and later edits.
Same-route Open note refreshes the existing note store; a failed read preserves
the accepted receipt. Inspected `ConversationInner.acceptCorrections.spec.ts`
deferred confirmation, actual request identity/base, named navigation and close,
later-edit reopening, same-ID lost-response retry, accepted read failure and
readonly MCQ controls. Full `CURSOR_DEV=true nix develop -c pnpm frontend:test`
passed: 352 files, 2,199 tests; both frontend typechecks passed on final content.
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/messages/conversation_about_a_note.feature,e2e_test/features/ai_generated_content/note_content_completion.feature`
passed both specs/four scenarios at isolated origin 63203 and the same E2E schema,
with runner-owned mocked OpenAI and real Donut acceptance. Both edited-body
Accept/Open changed note/reopen confirmed journeys passed; pending close/reopen,
unavailable AI and ordinary conversation/export remain covered. Initial fixture
failures came from ordinary-reply stub precedence and a raw Markdown assumption;
scoped the ordinary table to its scenario and observed the reviewed paragraph
within the complete normalized receipt, then reran both features successfully.
Enduring behavior is in `docs/recall-help.md#reviewed-note-corrections`.
Independent refactor extracted the message bubble, preserving DOM/API/adjacency,
and separated correction/refinement doc headings. Its conversation suite passed
12 files/50 tests including all acceptance cases; both typechecks passed.
Declaration-only Vite generation passed; after coordinator formatting, ordinary
`CURSOR_DEV=true nix develop -c pnpm -C frontend exec vite build --emptyOutDir`
passed including the checker. Other accepted boundaries remained unchanged.
Body UI obligations are fulfilled; title controls and explicit stale recovery
retain slices 7 and 8.

Accepted body-UI revision: `86d4963852ddb5ae42faa3ae22ea0f6883a519e2`
on the execution branch, suffix base `7211cc43252e39f8e92e1d46b4030321eb38b434`;
CI observation reused.

### 7. Apply reviewed title corrections through ordinary rename rules
Type: Behavior
Status: done
Proof: Controller acceptance and Git reload show the reviewed title on the same
note identity, with the retained decision/revision. Mounted conversation renders
and edits a title proposal; existing title/reference-validation tests cover its
ordinary decisions and actionable refusal.

Apply the same proposal lifecycle to the existing title-suggestion variant.
Delegate to ordinary title handling, including its reference-handling decision
when applicable; do not invent a second rename policy or silently rewrite other
notes. Enable title acceptance only with this slice. The body path remains green.
Estimate: 5–8 minutes.

Execution evidence (2026-10-14): title acceptance uses the ordinary annotated
title DTO and rename/reference owner with sorted notebook locks, sharing the
reviewed-base refusal with body acceptance. The normalized title, decision and
actual Git head are retained atomically. Inspected seven
`ConversationCorrectionTitleAcceptanceControllerTest` cases: committed fixtures
and fresh reads for direct-note/MCQ identity, downloaded renamed Markdown/head,
retry receipt, ordinary title refusals and both wiki-reference policies.
Inspected five `ConversationInner.acceptTitleCorrections.spec.ts` mounted cases:
edited title/base payload, confirmed receipt, required explicit reference choices,
and actionable validation feedback preserving input. Existing body/decline and
ordinary title/reference consumers remained covered. API generation and OpenAPI
lint passed. Full `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed:
2,846 tests, zero failures/errors, two skipped in the established isolated schema.
Full `CURSOR_DEV=true nix develop -c pnpm frontend:test` passed: 353 files,
2,204 tests. After the same-title UI adjustment, the conversation and ordinary
title-editor selection passed 15 files/67 tests, and both frontend typechecks
passed. The initial invalid-title fixture assumed `_trash` was reserved;
using the ordinary reserved `README` example repaired the fixture and the
full backend rerun passed. The generated enum type uses the existing store alias.
Independent refactor shared the reference-choice prompt and local display
predicate, parsing errors once; the same 15-file/67-test selection, both
typechecks and whitespace check passed. Backend/Git observations stayed unchanged.
Coordinator selective formatting passed. Enduring docs include title review.
One-time reading of the product for the replaced body-only refusal literal
returned no occurrences; this was a sweep reading, not an absence test.
G3 is discharged by the retained decline/body controls and this title control.

Accepted title revision: `c50d6689425171252e45f894c68bc55e6cd57243`
on the execution branch, suffix base `86d4963852ddb5ae42faa3ae22ea0f6883a519e2`;
CI observation reused.

### 8. Recover from a stale review without losing current input
Type: Behavior
Status: done
Proof: Mounted author review meets the stale response already proved in slice 5;
its edited text remains, current accepted content is loaded, and only a new
explicit acceptance sends the refreshed reviewed base. A failed refresh keeps the
existing input and reports failure. A controller acceptance against that rereviewed
base publishes through the same rule.

Finish the explicit current-content/rereview interaction. Keep the original
proposal and origin inspectable while the author decides what to submit against
current content. No automatic merge, silent retry, or second proposal lifecycle.
Update enduring recovery documentation in the same increment. Estimate: 5–7 minutes.

Execution evidence (2026-10-14): the current-review endpoint authorizes the
conversation and current note read, locks correction then notebook binding,
refreshes the managed subject and returns its NoteRealm with accepted head.
It refuses a subject that moved notebooks while acquiring that lock.
Inspected five `ConversationCorrectionReviewControllerTest` cases: direct/MCQ
current snapshots with pending disposition, queued ordinary author publication
before the snapshot lock, and separate conversation/read permissions. The
existing stale `ConversationCorrectionAcceptanceControllerTest` now uses that
real endpoint before publishing and retaining the explicitly rereviewed base.
Mounted `ConversationInner.staleCorrections.spec.ts` observes delayed body/title
refresh preserving the draft and source, then a second explicit Accept carrying
the fresh base; failed refresh preserves the draft and previous base.
`CURSOR_DEV=true nix develop -c pnpm generateTypeScript` and OpenAPI lint passed.
Final full `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed:
2,851 tests, zero failures/errors, two skipped in the established isolated schema.
The final run includes the queued snapshot case added after the first compile.
Full `CURSOR_DEV=true nix develop -c pnpm frontend:test` passed: 354 files,
2,207 tests; both frontend typechecks passed. Generated optional data/response
types were corrected through ordinary contract assertions and optional status.
Independent refactor kept original-proposal details in the editable pending
branch, retaining the visible proposal in completed/readonly views. Its mounted
selection actually ran three files/12 tests, then corrected two mistaken file
names and ran the remaining two files/10 tests; all five required consumers,
both typechecks and whitespace passed. Backend/API proof remained unchanged.
Existing Cypress selectors scope the accepted receipt and editor, whose flow
is unchanged; slice 6 integrated evidence remains applicable. Coordinator
selective formatting passed; enduring recovery docs were updated.
Initial cached-content acceptance is explicitly unproved here and assigned to
G7/slice 9; this increment completes explicit rereview recovery only.

Accepted recovery revision: `2a66bd77331f289d5cb030c12c686b1e8bbf80b0`
on the execution branch, suffix base `c50d6689425171252e45f894c68bc55e6cd57243`;
CI observation reused.

### 9. Wait for fresh current content before initial acceptance
Type: Behavior
Status: done
Proof: Mounted conversation begins with an older cached note and a deferred fresh
read. The review cannot accept until the read succeeds and the fresh content is
shown; a failed read keeps acceptance unavailable and permits retry without
losing the draft. Ordinary loader consumers remain covered.

Expose initial read progress through the existing NoteRealmLoader slot and use
it to gate this review's acceptance, or use an equivalent existing loader-owned
fresh-read contract. Retain the proposal base until explicit rereview. Cached
content may help reading but cannot authorize overwriting unseen content.
Estimate: 3–5 active minutes. This remains the same review/version safety rule,
with its own initial-read proof loop; it adds no product scope.

Execution evidence (2026-10-14): the loader exposes read progress, successful
freshness and explicit retry, with recoverable failure opted into by review.
Its successful-read event supplies the review's observed realm. That view is
updated by its own fresh read, explicit paired rereview or accepted-note refresh;
an older overlapping response may replace the shared cache without replacing
the displayed review. Accept remains unavailable before initial freshness and
after a failed initial read; retry keeps the draft and original proposal base.
Inspected four `ConversationInner.initialCorrectionReview.spec.ts` mounted
observations: no-cache loading, cached older content/deferred fresh read and
explicit original-base acceptance, failure/retry with retained draft, and an
actual older HTTP read finishing after the review's own read. The last case
observes old data in the shared cache while the review still shows its fresh
read and sends the original proposal base. Initial red proof failed at enabled
Accept on cached data. A template adjacency error was repaired before final
proof. The response-order gap found in acceptance was corrected in the same
attempt, about eight cumulative implementation minutes.
Final `CURSOR_DEV=true nix develop -c pnpm frontend:test` passed: 355 files,
2,211 tests; both frontend typechecks passed. Initial/stale selection passed
two files/seven tests. NoteShow and NoteContextReader retain their existing
live-cache display and default loud failures; their full-suite proof passed.
Existing Cypress SDK calls, labels, scoped receipt selectors and app-busy
barriers are unchanged; prior integrated proof remains applicable. No API or
component registration changed. Independent refactor consolidated the repeated
read-failure feedback; the same two-file/seven-test selection, both typechecks
and whitespace check passed. Backend and ordinary-loader evidence remained
unchanged. Coordinator selective formatting passed. G7 is fulfilled.

## Story obligations

### G1. Durable proposal capture and reads
Reported: slice 1 — "No runtime proposal reads or API changes were added; their proof belongs to slice 2."
Story clause: "Retain the proposal and its decision with the conversation."
Disposition: proved by slice 2: ConversationAiReplyControllerTest.retainsFragmentedCorrectionAgainstTheRequestSubjectAndBase fresh controller proposal read

### G2. Conversation presentation
Reported: slice 2 — "Durable browser review belongs to slice 3"
Story clause: "Show an editable correction within the conversation, with the subject note, reason, and originating exchange."
Disposition: proved by slice 3: ConversationInner.corrections.spec.ts mounted proposal review, title Messages and reopened direct/MCQ views

### G3. Confirmed decision controls
Reported: slice 2 — "no browser review, decision, or acceptance UI was wired here."
Story clause: "The author explicitly accepts or declines it; discussion and proposal generation alone do not change the note."
Disposition: proved by slice 7: ConversationInner.acceptTitleCorrections.spec.ts explicit edited-title confirmation and reference choice, with retained slice 4 decline and slice 6 body confirmation proof

### G4. Existing Cypress conversation consumer
Reported: slice 3 — "The Cypress completion consumer still reads the retired conversation UI."
Story clause: "Show an editable correction within the conversation, with the subject note, reason, and originating exchange."
Disposition: proved by slice 3: note_content_completion.feature pending review/edit/close/reopen and conversation_about_a_note.feature, 3 scenarios passed

### G5. Existing generated-content acceptance journey
Reported: slice 3 — "The final accepted correction journey remains an explicit interim obligation for slice 6"
Story clause: "The author explicitly accepts or declines it; discussion and proposal generation alone do not change the note."
Disposition: proved by slice 6: both mocked OpenAI Cypress features pass edited review/Accept/Open changed note/reopen retained receipt using the real acceptance API

### G6. Atomic acceptance operation
Reported: slice 4 — "Acceptance remains slices 5–7."
Story clause: "The saved correction and its accepted decision must agree."
Disposition: proved by slice 5: ConversationCorrectionAcceptanceControllerTest committed receipt/Git, rollback, stale, authorization and queued-decision observations

### G7. Initial review needs fresh current content
Reported: slice 8 — "NoteRealmLoader exposes cached content immediately while loading; Review can enable initial Accept from cached A while proposal base is B."
Story clause: "Do not merge or overwrite unseen accepted work."
Disposition: proved by slice 9: ConversationInner.initialCorrectionReview.spec.ts cached/deferred fresh-read gating, failed-read retry and older overlapping HTTP response with retained observed content and original-base acceptance

## Verification and delivery

- Backend increments: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`;
  use `pnpm backend:verify` when a migration changes, per the backend skill.
  Controller tests use real database/domain owners and only external AI mocks.
  Atomicity proof must commit and reload across real transaction boundaries;
  an ambient rollback-only test or mocked publication does not prove the receipt.
- Frontend increments: `CURSOR_DEV=true nix develop -c pnpm frontend:test`, plus
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit` and
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` on the same
  generated types. Use focused files while iterating; include shared conversation,
  streaming, ordinary editing, and recall consumers before acceptance.
- Browser journey: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/messages/conversation_about_a_note.feature`.
  Keep mocked OpenAI output deterministic. Extend existing steps/page objects
  with domain outcomes and app-busy waits; do not mock Donut's acceptance API.
  No manual testing or paid quality evaluation is required by this plan.
- Generate the API client after wire changes, and regenerate the ERD after the
  schema change. Inspect generated contracts through the ordinary SDK owners.
- Follow AGENTS.md execution delivery: Jidoka, fresh post-change-refactor agent,
  API generation as needed, coordinator `./scripts/run.sh pnpm format:changed`
  once, plan evidence update, agent commit, push, and owned asynchronous CI repair.
  This preparation neither Takes the story nor starts that execution workflow.

## Current decisions and review

The structure increment prepares only the next behavior. API-to-UI boundaries
in slices 2/3 and 5/6 deliberately isolate durable stream capture and atomic
publication risks; each boundary keeps the product usable and names its remaining
UI obligation. Other slices extend one lifecycle instead of adding recognizers
for each example. All story promises map above; no slice grants broader edit
rights, imports private exchanges, or changes scheduling.

The schema-first deployment constraint is an external release checkpoint, not
an unobserved assertion of production readiness. It requires the release owner's
ordinary authorization at deployment time. The story remains a single author
outcome within its L hypothesis. Revise slice boundaries after actual overrun;
revisit story scope with Terry if evidence exceeds that hypothesis.

## Learnings

The MCQ acceptance issue is observed, but fixing only the note lookup cannot
satisfy reopened decisions or atomic attribution. Existing direct-note acceptance
and Git publication can be reused; durable review and its receipt are new work.

Plan refinement was not needed: construction already separated the two material
integration risks, retained cohesive decisions, and mapped their interim UI
obligations to the immediate consuming slices. No remaining slice-specific
concern was identified in this review. Estimates remain hypotheses, and the
recorded release checkpoint is not a claim of production readiness.

Execution refinement (2026-10-14): slice 8 reached about nine active minutes
with explicit recovery and its terminal proof complete. Acceptance inspection
then exposed a separate initial-read gap: the ordinary loader renders its cached
realm while a fresh request is pending. The recovery proof remains compatible;
continuing initial-load implementation in that attempt would exceed the hard
limit. Retain slices 1–8 and add bounded slice 9 with G7 ownership. No attempt
edits were discarded and no over-limit implementation was continued. The
cumulative model still has one proposal lifecycle, current-note read owner and
accepted-change owner. Nine slices need no story resplit; source scope, North
Star and ADR constraints remain unchanged.
