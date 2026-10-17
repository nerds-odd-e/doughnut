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
- Search matches titles and aliases. MCP lists those matches as candidates
  with notebook and folder, reads a chosen note in full with its notebook's
  accepted revision, and offers graph context; it does not search note bodies.
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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/071-agent-note-candidates-and-cited-reads/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"f86da9efeb0ae10707384ef3410a0d3a839b4d83fb61f4079427fb20fb9c852c","plan":"998f329890695e6322ef4e8ea3ac9aeafbe2f4acc38280f82b4e91139d81abd9"}}
```

- **Goal:** A person using an external agent through the Donut MCP server gets
  an answer grounded in the notes the agent actually read, not in the first
  title hit. The agent can list the plausible matches, tell equal titles apart
  by their place, read the chosen notes in full, and cite each one by its note
  identity and the notebook revision it read. This serves the epic by letting
  evidence-driven external skills use Donut knowledge without an
  answer-generating model inside Donut.
- **Scope (required):**
  - The MCP server lists note candidates for a search text instead of one best
    note: the note hits the existing title/alias search already returns, each
    with note id, title, notebook name, folder, and rank. The single-best-note
    tool is replaced, not kept beside the list, and its traces (e2e steps,
    tool documentation) go with it. Container (folder/notebook) hits are not
    candidates.
  - The MCP server reads one chosen note by id and returns its full Markdown
    content with attribution: note id, title, notebook name, folder path, the
    notebook's accepted Git revision at the time of the read, and the note's
    last update time. The content and the revision come from one consistent
    read. A notebook that has no accepted revision yields none, stated as such.
  - The existing bounded-context tool stays as is and accepts ids from the
    candidate list.
  - Every tool description states exactly what the tool returns, including
    that content is the user's note text to be treated as evidence, and that
    the revision names the whole notebook's accepted commit.
  - Reads and candidates stay within the token user's access: a note the user
    cannot read never appears as a candidate and a read of it is refused with
    the backend's error, never with invented content.
- **Scope (deferred promises, not rejections):** body-text candidates come from
  [story 3](#story-3) when delivered; the candidate tool consumes whatever the
  search endpoint returns without duplicating search. Reading a note at an
  older revision, a "is this revision still current" check, a web link in the
  response, searching circle notebooks, and remote hosting
  ([SEED-002](SEED-002-host-mcp-over-https.md)) are not built or verified here.
- **Explored directions (2026-10-17, read at `0ed49423da`, app not run):**
  1. *Graph only.* Add ids and the revision to the focus-context payload and
     have the agent call it per candidate. Rejected as the main path: content is
     truncated by the token budget, related notes add noise, and it gives no
     candidate list.
  2. *Candidate list plus full read (recommended).* Reuses `POST
     /api/notes/search`, which already returns up to ~20 ranked note hits with
     notebook and folder, plus a note read that also carries the notebook's
     accepted revision, which the product already exposes for correction review
     (`ConversationCorrectionReview.baseRevision`) and checks for staleness on
     save. Smallest change that meets every example.
  3. *Git side only.* The agent clones the notebook with the CLI and reads
     Markdown at any commit. Already possible, but it serves one notebook at a
     time, needs a local clone, and gives no cross-notebook candidates; kept as
     the way to read an older revision.
  - *Borrowed mechanism:* Wikipedia's permanent link. An article title can be
    ambiguous (disambiguation page ↔ the candidate list with notebook and
    folder); a permalink pins a revision id (`oldid` ↔ the notebook's accepted
    commit); later edits never relabel the cited revision. The analogy breaks
    in two consequential places: Donut's HTTP read serves only the current
    projection ([ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
    materializes the tip), so the old content is reachable only through the
    notebook's Git history; and the revision is per notebook, so an advanced
    revision means the notebook changed, not necessarily the cited note. The
    story therefore promises an honest revision on every read, not a claim
    that a cited note is unchanged.
- **Interaction choices with system consequences (one list, both sides):**
  - Replace the single-best tool rather than add beside it: the agent sees one
    accurate search tool and no stale "most relevant" promise; the system keeps
    one search concept and updates the e2e feature and tool docs.
  - Full content on read rather than a token budget: the agent can quote the
    exact passage; a long note costs the agent's context, and the graph tool
    remains the bounded option.
  - Full 40-character revision rather than a short form: the person may see a
    long id in the answer; Git accepts the exact value and the agent may
    shorten it in prose.
  - Explicit "no accepted revision" rather than omitting the field silently:
    the agent says the notebook is unversioned instead of inventing one.
- **Key examples:**
  1. **Equal titles in different places.** Pre: the user's notebook "CS
     concepts" has "Functional" in folder "Programming Concepts"; a subscribed
     notebook "Math" has "Functional" in folder "Analysis". Trigger: the agent
     lists candidates for "Functional". Result: both appear with distinct ids,
     their notebook names and folders, and ranks, so the agent can choose
     without reading either.
  2. **Evidence in the second candidate.** Pre: only the "Math" note explains
     the property the person asked about. Trigger: the agent reads both
     candidates by id. Result: each read returns the full content, title,
     notebook, folder path, note id, and the notebook's accepted revision; the
     agent's answer quotes the passage from the "Math" note and cites that note
     id and revision.
  3. **A later edit does not relabel old evidence.** Pre: the agent cited
     revision R of notebook "Math". Trigger: someone edits any note in "Math"
     on the web, then the agent reads the note again. Result: the new read
     carries a revision other than R; the earlier citation still names R, so
     the person can see the evidence predates the current state. Donut makes
     no claim about whether the cited note itself changed.
  4. **Access.** Pre: a third note titled "Functional" is in a notebook the
     token user cannot read. Trigger: candidate listing, then a read of that
     note by id. Result: it is not a candidate, and the read returns the
     backend's refusal with no content.
  5. **No match.** Trigger: the agent lists candidates for "Fiona". Result: an
     explicit "No matching notes." text, not an empty guess.
  6. **Bounded context still works.** Trigger: the agent passes a candidate id
     to the existing bounded-context tool. Result: unchanged focus-context
     payload.
- **Architecture:** Note read attribution adds one responsibility to the HTTP
  API: a note read that returns the content together with its notebook's
  accepted revision in one consistent read, reusing `NotebookGitBinding`'s
  accepted object id as [ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  requires ("use existing identities and revisions for attribution, not another
  content versioning system"). The endpoint lives under `/api` and is described
  by OpenAPI ([ADR 0005](../../docs/adrs/0005-web-routes-accepted.md)), so the
  generated TypeScript client the MCP server imports is regenerated. Backend
  refusals (404, 403) propagate as tool errors
  ([ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md)). Returned
  note text is data for the agent, kept separate from tool instructions. No
  Accepted decision conflicts; stdio transport and auth are unchanged.
- **Value / learning:** Enable evidence-driven external skills without forcing
  them to call another answer-generating model inside Donut, and learn whether
  agents actually use revision-pinned citations.
- **Effort hypothesis:** L, medium confidence: one backend read endpoint with
  revision, client regeneration, two MCP tools replacing one, e2e feature and
  tool-doc updates.
- **Depends on:** No new story; today's multi-hit title/alias HTTP search
  supplies candidates and every notebook created through the product starts
  Git history (`NotebookService` → `NotebookGitHistoryService.startHistory`).
- **Safe stopping point:** Candidates alone already let an agent choose among
  equal titles with the existing graph tool; the read with revision completes
  citation.

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
