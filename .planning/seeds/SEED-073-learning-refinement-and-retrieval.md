---
id: SEED-073
status: dormant
planted: 2026-10-12
planted_during: owner-directed backlog shaping after the OpenKB implementation assessment
trigger_when: improving note quality and retrieval quality through everyday learning and use
scope: large
---

# SEED-073: Improve knowledge through learning, refinement, and retrieval

## Epic Outcome

For people learning from deliberately authored Donut notes, a confusing recall
answer or conversation should lead to better understanding or a reviewed note
correction, and later questions should find and use the relevant evidence.
Preserve the learner's history and the author's control over knowledge changes.

This is the parent epic for the combined learning/refinement and chat work,
question-context quality, evidence discovery, and cited answers. It is not one
executable story and is not queued in addition to its children. The independent
[knowledge-to-capability experiment](SEED-074-knowledge-to-capability.md#story-1)
is outside this epic. Its outcome is useful even if this epic stops.

## Why This Matters

The owner wants to borrow OpenKB ideas for improving and using accumulated
knowledge. Donut's starting point remains a person noticing and capturing
something worth learning; bulk document compilation is not the priority.

The 2026-10-12 assessment read local main at `e851a7682d`, including code,
tests, and Accepted ADRs. It did not run the app or measure model quality:

- Answered MCQs already open refinement with the question and tested focus,
  but that refinement input omits the learner's selected answer.
- Refinement already decomposes content and previews extraction; contesting a
  question and conversing about a recall prompt are existing actions.
- Chat already proposes title/content updates for acceptance. The recall-chat
  proposal handoff needs verification: frontend acceptance reads the direct
  note subject, while a recall conversation has a recall-prompt subject.
- A shared bounded focus context already serves question generation, chat,
  automation, exports, and the graph API. Task information does not guide
  candidate selection. Related-note bodies are truncated to short prefixes.
- Search matches titles and aliases. MCP returns one matching note's metadata,
  then offers graph context; it does not expose a candidate list or body search.
- Git preserves accepted notebook content; recall history preserves learner
  events. Neither alone records the diagnosis, proposal decision, and learning
  follow-up as one resolved difficulty.

These are starting observations, not promises that the current UI works in
every cited journey. Verify the selected story's decisive premises when refining it.

## Alternatives and Decision

- **Defer everything:** Keeps the present disconnected actions and gives no
  feedback about whether a difficulty is caused by knowledge, understanding,
  the question, or retrieval.
- **Existing tools and manual editing:** The strongest smaller alternative is
  to discuss the question, manually edit the note, and continue recall. Reuse
  this workflow, but reduce the lost answer context and manual handoff, then
  make an accepted correction and its origin inspectable.
- **Build a general review queue or new chat system:** Premature. Start from
  an explicit confusing MCQ and the existing conversation/refinement surfaces.
- **Selected direction:** Deliver the useful child outcomes below. Diagnose
  before changing knowledge; use measured question-quality cases to choose
  retrieval improvements. Preserve simple search alongside richer answering.

## Shared Constraints

- Assimilation creates a new memory tracker. Revisiting understanding uses
  the existing tracker and history; it does not silently reset or recreate it.
  Grading follows [ADR 0003](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md).
- A wrong answer is evidence of a difficulty, not an established diagnosis.
  Learners can correct a suggested diagnosis. Ordinary forgetting does not
  automatically create a task or modify a note.
- Accepted content changes use the existing publication boundary and Git
  history under [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md).
  Use existing identities and revisions for attribution, not another content
  versioning system. Private learning evidence stays outside Portable content.
- Existing ownership applies to corrections. A learner who can read a shared
  note does not gain permission to edit it through these flows.
- Retrieved content is evidence, distinct from application instructions.
  Search, expansion, citations, and later reads respect the viewer's access.
- Bulk ingestion, embeddings, a universal review inbox, automatic knowledge
  rewrites, skill factories, and remote MCP hosting are deferred promises.
  These deferrals do not prescribe runtime rejection of naturally supported behavior.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery, following
[SEED-039](SEED-039-faster-ci-feedback.md#story-decomposition). Estimates are
hypotheses. Refine or resplit work likely to exceed L before execution.
Each story records its preparation state below.

### Generate focus-note questions with relevant supporting passages

The canonical story is [SEED-007#story-1](SEED-007-per-concept-prompt-splitting-single-note-qg.md#story-1).
It is the next epic increment after the two learning-loop stories. Keep its
scope, examples, preparation state, and identity in that existing seed only.

<a id="story-3"></a>

### Find a note from wording remembered in its body

**Identity:** SEED-073#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner who remembers a useful passage but not its note
  title can find the note through ordinary Donut search.
- **Scope:** Extend the existing search journey to body-text evidence, with
  a matching excerpt and a link to the note. Preserve fast title/alias search
  and its scope controls. Search remains evidence discovery, with no generated
  answer required. The existing search API serves the same behavior.
- **Evaluation:** A phrase occurs only in a note's body. Searching it shows
  that note and the matching passage; opening the result reaches the note.
  Exact title/alias searches remain useful, and unavailable notes do not appear.
- **Value / learning:** Discover how much missing knowledge is actually a
  title-only search limitation before investing in richer query orchestration.
- **Effort hypothesis:** L, medium confidence; ranking, representative corpus
  size, and response-time expectations need refinement.
- **Depends on:** No new story. Search and graph expansion have different
  responsibilities, so question-context changes do not block this outcome.
- **Safe stopping point:** Learners can find and inspect additional evidence
  with the existing search interaction even if synthesized answers are deferred.

<a id="story-4"></a>

### Let an external agent inspect several candidate notes and cite its evidence

**Identity:** SEED-073#story-4
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A person using an external agent can have it compare plausible
  matches and read the supporting knowledge rather than trust the first hit.
- **Scope:** Extend the current MCP evidence journey to return multiple
  candidates and directly read the chosen notes or bounded context. Results
  provide unambiguous navigation and source revision attribution so the agent
  can cite the notes actually read, including equal titles in different places.
  Reuse HTTP read/search/context capabilities and notebook Git authority.
  Tool descriptions must accurately describe the content they return.
- **Evaluation:** Two notes match a term; the useful evidence is in the second.
  An agent lists the candidates, reads both, and returns an answer identifying
  the passage and revision it used. A later notebook edit does not silently
  relabel the old evidence as current. Reads remain within the user's access.
- **Value / learning:** Enable evidence-driven external skills without forcing
  them to call another answer-generating model inside Donut.
- **Effort hypothesis:** L, medium confidence; reuse stdio MCP and existing
  reads, with no hosting/transport change.
- **Depends on:** No new story; today's multi-hit title/alias HTTP search can
  supply candidates. Consume body matches when available without waiting for
  them or duplicating search. Remote hosting remains in SEED-002.
- **Safe stopping point:** Existing agents can discover, read, and attribute
  evidence independently of app query or the capability experiment.

<a id="story-5"></a>

### Answer a comparison across notes with inspectable supporting evidence

**Identity:** SEED-073#story-5
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner comparing two concepts can obtain an answer grounded
  in their notes and examine what supports it.
- **Scope:** Start with a comparison naming two concepts in one accessible
  notebook. Find candidate notes through existing search, read relevant
  evidence with bounded expansion, and answer with attributable references.
  State uncertainty when the notebook does not support a conclusion. A
  conversational follow-up can request another relevant note as the question
  changes; reuse the current conversation surface and common evidence access.
  This is the first richer query journey, not a commitment to arbitrary
  research across the entire catalog.
- **Evaluation:** Two notes separately explain concepts and a third supplies
  a qualification. A comparison cites the supporting passages, and a follow-up
  about the qualification retrieves that evidence. If the qualification is
  missing or sources disagree, the answer exposes that limit instead of inventing
  agreement. Simple search remains available.
- **Value / learning:** Test whether bounded synthesis helps users apply their
  knowledge and whether conversationally chosen context improves that answer.
- **Effort hypothesis:** L, low confidence; refine the entry interaction and
  comparison cases, and resplit if one end-to-end comparison exceeds L.
- **Depends on:** No hard story block established. Current HTTP search and
  focus context support a bounded comparison; reuse earlier evidence improvements
  when delivered. Shared code and preferred delivery order alone impose no wait.
- **Safe stopping point:** One useful, cited comparison journey with honest
  evidence limits; broader chat, catalog-wide research, and new-note proposals
  can remain deferred.

<a id="story-6"></a>

### Contest an answered question with a reason and an inspectable decision

**Identity:** SEED-073#story-6
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner who believes an answered MCQ was ambiguous or wrong
  can have the actual objection considered and later see what was decided.
- **Scope:** Extend the existing contest responsibility with the original
  attempt and learner's reason, an explanation of the verdict, and durable
  attribution to the attempt. Distinguish a conversational hypothesis from a
  formal upheld contest. State the actual effect on the question and recorded
  Grade; a learner-confirmed ambiguity exclusion repairs grade and schedule
  ([recall help](../../docs/recall-help.md#learner-confirmed-ambiguity-exclusion)).
  Learner reasoning remains private
  unless deliberately shared. Existing author permission still governs notes.
- **Evaluation:** After choosing an option defensible under the wording, the
  learner contests with that interpretation. The result addresses the objection,
  can be reopened, and states whether the question was invalidated and whether
  the attempt's learning effects were corrected. A declined contest gives an
  inspectable reason, not instructions to answer an already answered question.
- **Value / learning:** Restore trust through a meaningful challenge and learn
  whether disagreements concern wording, evidence, or the marked answer.
- **Effort hypothesis:** M–L, low confidence until verdict authority, persistence,
  and shared-MCQ effects are refined. Separate from explanation/practice by
  the owner's accepted 2026-10-12 decision.
- **Depends on:** Existing contest and recall identities. Private recall help
  can supply context when present but is not a hard implementation prerequisite.
- **Safe stopping point:** The learner can inspect the decision and its actual
  consequences without any implied repair of the recorded Grade.
- **Open decisions:** Who finalizes a verdict, what an accepted alternative
  answer means, and whether one MCQ's invalidation affects other learners.
  Refine before execution planning; this candidate is not queued.

**Preparation observation:** On 2026-10-12 at `e1bbd1d2727fced534f473bcba709407a46d91c9`
in story 1's owned workspace, a disposable controller probe answered an MCQ
incorrectly, reloaded persisted state, then contested with a mocked infeasibility
result. Reload showed a contested MCQ, the original incorrect Answer and AGAIN
event, and unchanged Stability, Difficulty, last recall and due time.
`CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed with 2738 tests,
zero failures/errors and two skipped in 51s. An initial timestamp comparison
mixed in-memory milliseconds with MySQL rounding; comparing persisted before/after
values corrected the probe without changing product code. Scratch files were
removed. This establishes the existing API mechanism, not a working user-facing
post-answer journey, model adjudication quality, or scoring repair.

## Ordering and Scope Reduction

The backlog owns global priority. Within this epic: resolve an actual learning
difficulty, retain a useful note correction, improve question evidence through
SEED-007, then improve body search, external-agent evidence access, and cited
comparison. This order favors immediate learning value before broader query
machinery; it does not impose serial technical dependencies.

Drop or defer broader comparison first if capacity is tight, then expanded
external-agent access. Keep the existing workflows usable at every stopping
point. The separately queued capability experiment can use a current notebook
checkout and is neither a supplier nor a consumer blocking this epic.

<a id="manual-reviewed-correction-evaluation"></a>

### Observe reviewed conversation note corrections

**Identity:** SEED-073#manual-reviewed-correction-evaluation
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** Observe the delivered “Save a useful chat clarification as a reviewed note correction” journeys within the owner's 30-minute manual-testing budget.

**Scope:** Manual observation only. Recover the completed story from Git and current recall help; cover direct-note and MCQ content/title proposals, pending/declined/accepted decisions, durable receipts, stale-review recovery, and existing audience/edit rights. No diagnosis, product repair, or permanent test changes. Record material coverage gaps when time or supported setup prevents observation.

**Known expectations:** Explicit reviewed acceptance publishes the exact wording with a stable revision receipt; proposals, decline, and closing preserve authored content. Stale or failed reviews preserve input and permit explicit recovery. Private review evidence and recall state retain their existing ownership and meaning.

**Session plan:** Preparation 7 minutes; breadth 10 minutes; selective depth 8 minutes; confirmation and cleanup 5 minutes. Prioritize unintended publication, lost wording, durable receipts, and stale review. Use isolated held E2E services with deterministic model replies when supported.

**Observed outcome (2026-10-14):** Bounded browser and public-HTTP observation found one title-refresh discrepancy. Expected: after accepting a reviewed title, the visible subject-note title is consistent. Actual: the heading and wiki links use the accepted title, while the sidebar keeps the previous title across in-app navigation until a full reload. Reproduced with both reference-handling choices: “Japan geography” → “Japan prefectures — reviewed”, then “Japan prefectures — reviewed” → “Japan’s 47 prefectures”. The screenshot `manual-correction-sidebar.jpg` below shows the second mismatch. Reload refreshed the sidebar. No diagnosis or repair was performed.

**Coverage:** Direct-note body and title reviews; current and proposed values, reasons and originating messages; edited acceptance; pending state across closing (unsent edits discarded); durable decline; ordinary invalid-title refusal preserving input; both wiki-reference choices; stale title acceptance refusal after an ordinary body edit, draft preservation, explicit current-note reload and renewed acceptance; original body receipt after later editing and renaming; MCQ-help content acceptance and Messages reopening; Open note; failed browser save preserving input; browser retry recovering an independently recorded acceptance with the original revision. Public HTTP snapshots showed unchanged MCQ answer, recall history, tracker identity, stability, difficulty, recall count, last-recall and due times. A foreign learner received 403 for private conversation, messages, corrections and current review. With the owner's explicit local-fixture permission, a subscribed read-only learner could discuss and decline, while acceptance returned 403.

**Evidence:** Browser-visible accepted body revisions were `ff2ba6d0ae2e9fba9ed6b24f76527416d9be3667` (direct note) and `52bfcaa9dd22ae3a4664599c2309e7f27809207e` (MCQ help). The latter stayed the same on browser retry. Dated persisted before/after snapshots, the accepted receipt, and screenshots are in `.planning/manual-testing-evidence/manual-reviewed-correction/` in this recoverable revision.

**Provenance and limitations:** Session started 2026-10-14 20:18 JST at product base `5fe875a590549806a42441fa2e721358c02fb5c6`; app health reported E2E profile and tracking claim `1a4ae86bd234f9181d14a0a70fed00bbc0d102e3`. Created owned workspace `/Users/terryyin/git/doughnut/.worktrees/manual-reviewed-correction`, branch `codex/manual-reviewed-correction`; origin/integration `/Users/terryyin/git/doughnut`; target `origin/main`. Origin `http://127.0.0.1:59036`, isolated schema `doughnut_e2e_wt_fc4f10a05f624237b6a9ab1b161cccf2`; seeded `old_learner` and `another_old_learner`. Supported held E2E services and the repository's private Mountebank lifecycle supplied deterministic Responses tool streams. A restart supplied a dummy SDK key; testability injected notes/MCQ and a disposable SQL prompt tied MCQ 1 to tracker 1. The missing-key request and the initial MCQ-generation/mock mismatch were setup failures, not acceptance failures. Browser networking was blocked only for correction 4 acceptance to observe failure, then restored; a public-API acceptance followed by browser retry represented recovery after an unobserved response. No real model calls, model-quality assessment, production data, product changes, or permanent tests. Failed current-note loading/retry was not exercised.

The backlog owns global priority. Within this epic: resolve an actual learning
difficulty, retain a useful note correction, improve question evidence through
SEED-007, then improve body search, external-agent evidence access, and cited
comparison. This order favors immediate learning value before broader query
machinery; it does not impose serial technical dependencies.

Drop or defer broader comparison first if capacity is tight, then expanded
external-agent access. Keep the existing workflows usable at every stopping
point. The separately queued capability experiment can use a current notebook
checkout and is neither a supplier nor a consumer blocking this epic.

## Open Decisions

- Select real learner cases during evaluation; human comprehension,
  retention, and retrieval-quality gains remain unmeasured.
- Define how long proposal decisions and their private conversation evidence
  should remain available, using existing deletion/ownership rules.
- Agree practical latency and cost expectations for retrieval/query evaluation.
- Agent-reported gaps, automatic repeat-failure triggers, and claim-level
  provenance remain later decisions. Detailed contest and invalid-question
  correction policies are owned by story 6.

**Learner-confirmed ambiguity exclusion (owner decisions, deferred):**

- Whether an attempt marked correct can be excluded. The operation accepts
  one today; only the tutor instruction keeps proposals to wrong marks.
- Whether a recall-prompt conversation with an audience beyond its learner
  should show the exclusion reason or offer the proposal. It is unchecked
  whether any such conversation exists.
- Whether a day whose only answer was excluded keeps its place in the review
  calendar and streak. Recall statistics leave an excluded attempt out
  entirely today.

## When to Surface

When choosing work for the new learning-to-refinement and knowledge-quality
direction, or when repeated question, search, or chat difficulties provide
concrete evaluation cases.

## Breadcrumbs

- Owner's 2026-10-12 assessment and backlog request in this chat: combine the
  learning and chat work, reuse seeds, keep the capability experiment independent,
  group the remaining work as an epic, and commit/sync planning on main only.
- [Shared OpenKB discussion](https://chatgpt.com/share/6acc428e-cd14-83ec-8412-3b5e7d6826c6).
  Its review loop is a proposal for Donut, not a verified OpenKB feature.
- [Focus-context design](../../docs/focus-context/focus_context_retrieval_design.md)
  and [domain vocabulary](../../docs/adrs/0001-ubiquitous-language.md).
- [Existing question-context hypothesis](SEED-007-per-concept-prompt-splitting-single-note-qg.md)
  and [remote MCP hosting seed](SEED-002-host-mcp-over-https.md).
- SEED-055 concerns file-serving API cleanup and SEED-001 concerns a different
  fuzzy spelling-match behavior; neither is duplicated or reprioritized here.
