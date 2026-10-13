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

<a id="story-2"></a>

### Save a useful chat clarification as a reviewed note correction

**Identity:** SEED-073#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A note author discussing a note or recall question can retain
  the useful clarification in the note without copying and reconstructing it.
- **Scope:** Reuse current chat suggestions and refinement previews to show
  an editable correction to the existing subject note, its reason, and its
  originating exchange. Acceptance saves through ordinary note publication;
  rejection leaves authored content unchanged. Retain the proposal decision
  and the accepted change's reference with the conversation, including after
  reopening it. Resolve the subject note consistently for note and MCQ chats.
  This story corrects one existing note; creating discovery notes and changing
  several notes together remain later candidates.
- **Evaluation:** A recall conversation reveals that a note omits a useful
  distinction. Its author previews and edits an added example, accepts it,
  and can reopen both the improved note and the conversation showing what was
  accepted. Declining an alternative remains visible as a decision. A reader
  without edit rights can discuss the distinction without publishing a change.
- **Value / learning:** Close the concrete conversation-to-knowledge gap shared
  by the original learning-loop and stronger-chat proposals.
- **Effort hypothesis:** L, medium confidence; current acceptance and Git
  publication exist, but durable proposal attribution needs refinement.
- **Depends on:** No new story; it can start from an existing conversation.
  Reuse the existing [reviewed refinement handoff](../../docs/recall-help.md#reviewed-refinement).
- **Safe stopping point:** A reviewed, attributable note correction remains
  useful without adaptive retrieval or any general review system.

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
  Grade; grade/schedule repair is story 7. Learner reasoning remains private
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

<a id="story-7"></a>

### Repair an invalid recall attempt's learning effects while retaining its history

**Identity:** SEED-073#story-7
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner should not carry misleading recall readouts or a
  schedule caused by a question accepted as invalid.
- **Scope:** The owner accepted the direction of excluding an invalid attempt
  from evaluation while retaining its evidence, rather than automatically
  awarding a successful Grade. Make the correction attributable and apply it
  coherently to affected readouts and current scheduling state under ADR 0003.
  Do not reset/recreate the memory tracker or append a compensating GOOD.
- **Evaluation:** An incorrect answer has already produced AGAIN. After the
  question is accepted as invalid and correction is authorized, the learner
  can inspect the original attempt and its correction, and the next recall and
  readouts agree with the chosen correction policy. Repeating the same correction
  does not become another learning event. A later Grade is a decisive case for
  policy refinement, not grounds for a guessed implementation restriction.
- **Value / learning:** Preserve trust in learning history and scheduling when
  question quality, rather than demonstrated recall, caused the result.
- **Effort hypothesis:** Larger than L until authority and historical reach are
  settled; low confidence. This is an unselected follow-up requiring further
  decomposition before any executable plan, not a commitment to one large build.
- **Depends on:** An attributable accepted invalidity decision. Story 6 is the
  preferred supplier, but refinement must establish any genuine blocking
  implementation dependency rather than deriving it from order alone.
- **Safe stopping point:** Corrected scheduling and readouts remain explainable
  from retained history; deferred adaptive tutoring or retrieval adds no dependency.
- **Open decisions:** Who authorizes correction; latest and historical attempts;
  subsequent Grades, Confusion, and cached readouts; personal versus other
  attempts on the same MCQ; and the durable ADR 0003 policy interpretation.
  The accepted direction does not silently answer these. This candidate is not queued.

## Ordering and Scope Reduction

The backlog owns global priority. Within this epic: resolve an actual learning
difficulty, retain a useful correction, improve question evidence through
SEED-007, then improve body search, external-agent evidence access, and cited
comparison. This order favors immediate learning value before broader query
machinery; it does not impose serial technical dependencies.

Drop or defer broader comparison first if capacity is tight, then expanded
external-agent access. Keep the existing workflows usable at every stopping
point. The separately queued capability experiment can use a current notebook
checkout and is neither a supplier nor a consumer blocking this epic.

## Open Decisions

- Select real confusing-question cases with the owner during refinement;
  no failure diagnosis or retrieval-quality gain has yet been measured.
- Define how long proposal decisions and their private conversation evidence
  should remain available, using existing deletion/ownership rules.
- Agree practical latency and cost expectations for retrieval/query evaluation.
- Agent-reported gaps, automatic repeat-failure triggers, and claim-level
  provenance remain later decisions. Detailed contest and invalid-question
  correction policies are owned by stories 6 and 7.

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

<a id="manual-language-learner-evaluation"></a>

### Evaluate private recall help as a language learner

**Identity:** SEED-073#manual-language-learner-evaluation
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** Observe whether private recall help resolves a language learner's
specific confusion, especially after an incorrect follow-up answer and an
ambiguous question.

Scope: A 45-minute manual session authorized on 2026-10-13, using an isolated
E2E app and real OpenAI replies with `gpt-6.1-sol`, capped at ten replies.
Observe explanation, incorrect practice feedback, continued misunderstanding,
ambiguity, saved continuation, and return to recall. Evaluate pedagogical
usefulness from a simulated learner perspective; this establishes no actual
human learning or retention claim. Report findings without product repair.

#### Manual observation: 2026-10-13

Observed through the real browser at `http://127.0.0.1:52087/recall`, product base `871aea594bff4aedf46e26a86eb1f6e8166d5f66`, mission revision `ed72ab022ade78a1a689882eb848e5ba4403ae78`. The selected checkout was `.worktrees/manual-language-recall-help`, created for this mission. Real consuming exports confirmed `gpt-6.1-sol`; ten assistant replies were consumed, five in each conversation. Fixed original questions were seeded without paid question generation.

Setup initially failed with concurrent class-directory writes, then a missing main class. A fresh sequential `classes --rerun-tasks --no-build-cache` and temporary invocation of the existing `runE2eHold` with `backendReload: false` reached readiness. This is an observation workaround, not a product repair.

Findings: the incorrect follow-up received misconception-specific feedback; continued confusion led to a simpler meaning-based explanation and another unanswered check; the learner supplied the correct new answer and an original example. The ambiguous question was recognized from the generic wrong-answer shortcut; the assistant distinguished both valid meanings, gave disambiguating wording, generated a fair follow-up, and explicitly said the original grade and schedule were unchanged. A subsequent past-familiarity challenge corrected the shortcut that familiarity is always present.

Both saved conversations reopened. Escape restored the original attempt and focus; Resume reached the next fixed question. Exact persisted Answer, RecallLog and MemoryTracker snapshots matched before and after each conversation. The default 1280x720 help view placed chat and question side by side in a narrow modal; long replies required substantial scrolling and the header/composer were separated. Toggle maximize widened the conversation by hiding question context.

One surprise remains unconfirmed: after the fourth ambiguity reply, a later browser snapshot showed the completed-recall screen with help dismissed, without a recorded deliberate dismissal. Last-answer navigation and reopening recovered all messages. This did not repeat after the fifth ambiguity reply. Cause and reproducibility were not established.

Assessment: the observed teaching provides useful conceptual repair in these simulated learner cases; an ambiguous original attempt remains visibly red and scheduled as incorrect. This is agent-simulated dialogue, not measured human comprehension, representative model reliability, or retention. Browser and HTTP/storage evidence jointly cover this session. Production runtime model was not inspected.


#### Fixed original cases

```json
{
  "models": {
    "questionGenerationModel": "gpt-4.1-mini",
    "evaluationModel": "gpt-6.1-sol",
    "othersModel": "gpt-4.1-mini"
  },
  "notes": {
    "Used to and be used to": 1,
    "Two grammatical meanings of used to": 2
  },
  "cases": [
    {
      "title": "Used to and be used to",
      "noteId": 1,
      "mcq": {
        "id": 1,
        "correctAnswerIndex": 0,
        "contextSeed": null,
        "testedFocus": "Distinguish a past habit from being accustomed to something.",
        "validationRationale": "Finds it normal indicates being accustomed: be used to + gerund.",
        "questionStem": "After living in London for a year, Emi finds driving on the left normal. Choose the expression that conveys this meaning: Emi ___ on the left.",
        "responseChoices": [
          "is used to driving",
          "used to drive"
        ]
      },
      "trackers": [
        {
          "id": 1,
          "note": {
            "noteTopology": {
              "createdAt": "2026-10-13T01:25:08.000Z",
              "id": 1,
              "title": "Used to and be used to",
              "updatedAt": "2026-10-13T01:25:08.000Z"
            },
            "content": "Used to + base verb describes past habits or past states, typically contrasting with the present: I used to drive to work, but now I take the train. Be used to + noun or gerund means be accustomed to something: I am used to driving on the left, so it feels normal now. In be used to, to is a preposition and can be followed by a noun or -ing form. Get used to + noun or gerund describes becoming accustomed. The two patterns have different meanings; both can form grammatical sentences.",
            "id": 1
          },
          "lastRecalledAt": null,
          "nextRecallAt": "2026-10-13T01:25:08.332Z",
          "assimilatedAt": "2026-10-13T01:25:08.332Z",
          "stability": 0.0,
          "difficulty": null,
          "removedFromTracking": false,
          "type": "UNDERSTANDING",
          "propertyKey": "",
          "recallCount": 0,
          "recalledNote": {
            "ancestorFolders": [],
            "noteTopology": {
              "createdAt": "2026-10-13T01:25:08.000Z",
              "id": 1,
              "title": "Used to and be used to",
              "updatedAt": "2026-10-13T01:25:08.000Z"
            },
            "notebookId": 1,
            "propertyKey": null,
            "propertyValue": null
          },
          "latestTutorFeedbackGrade": null,
          "spelling": false,
          "propertyValue": ""
        }
      ]
    },
    {
      "title": "Two grammatical meanings of used to",
      "noteId": 2,
      "mcq": {
        "id": 2,
        "correctAnswerIndex": 0,
        "contextSeed": null,
        "testedFocus": "Distinguish past habit from being accustomed to something.",
        "validationRationale": "Used to + base verb can describe a past state.",
        "questionStem": "Choose the grammatically correct completion: I ___ in Tokyo.",
        "responseChoices": [
          "used to live",
          "am used to living"
        ]
      },
      "trackers": [
        {
          "id": 2,
          "note": {
            "noteTopology": {
              "createdAt": "2026-10-13T01:25:08.000Z",
              "id": 2,
              "title": "Two grammatical meanings of used to",
              "updatedAt": "2026-10-13T01:25:08.000Z"
            },
            "content": "I used to live in Tokyo means I lived there in the past, with an implied contrast to now. I am used to living in Tokyo means living in Tokyo is familiar or normal to me. Both sentences are grammatically correct. A question choosing between them needs a meaning or time context. The correct choice depends on the intended meaning, not on one expression being universally ungrammatical.",
            "id": 2
          },
          "lastRecalledAt": null,
          "nextRecallAt": "2026-10-13T01:25:08.515Z",
          "assimilatedAt": "2026-10-13T01:25:08.515Z",
          "stability": 0.0,
          "difficulty": null,
          "removedFromTracking": false,
          "type": "UNDERSTANDING",
          "propertyKey": "",
          "recallCount": 0,
          "recalledNote": {
            "ancestorFolders": [],
            "noteTopology": {
              "createdAt": "2026-10-13T01:25:08.000Z",
              "id": 2,
              "title": "Two grammatical meanings of used to",
              "updatedAt": "2026-10-13T01:25:08.000Z"
            },
            "notebookId": 1,
            "propertyKey": null,
            "propertyValue": null
          },
          "latestTutorFeedbackGrade": null,
          "spelling": false,
          "propertyValue": ""
        }
      ]
    }
  ]
}
```


#### Incorrect answer and continued confusion


**Learner**

"I'm a Japanese learner of English. I chose 'used to drive' because I learned that after 'to' we use the base verb. 'To driving' looks wrong to me. Please explain in simple English why 'is used to driving' fits here."


**Assistant**

Your rule is correct for **“to” before a base verb**, as in “I want **to drive**.” But **“to” has another job**: it can be a preposition, like “in” or “at.”

Compare these two patterns:

- **used to + base verb** = a past habit or state  
  “Emi **used to drive** on the left.”  
  She did this in the past; it suggests things are different now.

- **be used to + noun or -ing form** = something is familiar or normal  
  “Emi **is used to driving** on the left.”  
  Driving on the left feels normal to her now.

In **“be used to,”** *to* is a preposition. That is why we use **driving**, not **drive**. You can also put a noun after it: “Emi is used to **London traffic**.”

The question says Emi **finds driving on the left normal**, so **“is used to driving”** fits. Your choice, **“used to drive,”** is grammatical too—but it describes her past, not what feels normal now.

A useful memory tip:  
**used to do = did before**  
**be used to doing = feels normal**


**Learner**

"Give me one different, related practice question. Wait for my answer before explaining it."


**Assistant**

At first, working at night was difficult for Ken. Now it feels normal to him.

Which expression fits this meaning?

**Ken ___ at night.**

A. is used to working  
B. used to work


**Learner**

"B: used to work. He worked at night many times in the past, so now it is normal. I think 'used to work' can mean both the past habit and that he feels normal now. Also I thought 'to working' was wrong."


**Assistant**

You correctly noticed that repeated experience can make something feel normal. But **“used to work” does not itself mean “feels normal now.”** It describes a past habit, usually suggesting that the habit has changed.

The answer here is **A: “Ken is used to working at night.”** The key clue is **“Now it feels normal to him.”**

Compare:

- **Ken used to work at night.** = He worked at night in the past; he probably does not now.
- **Ken is used to working at night.** = Working at night feels normal to him now.

About **“to working”**: your base-verb rule applies to phrases like **“wants to work.”** But in **“is used to,”** *to* is a preposition, so it takes a noun or an **-ing** form:
- He is used to **night work**.
- He is used to **working at night**.

In this answer, you connected past experience with present familiarity—but the English patterns keep those two meanings separate.


**Learner**

"I still feel confused when both situations include doing something many times. Can you give me a simple way to choose without grammar words, then one short new sentence to try? Please wait for my answer."


**Assistant**

Ask yourself: **What is the sentence telling me?**

- **“I did it before.”** → **used to do**
- **“It feels normal to me.”** → **am/is/are used to doing**

Both may involve doing something many times. Choose by the **message**, not the number of times.

Try this:

**Maya ___ coffee, but now she only drinks tea.**

A. used to drink  
B. is used to drinking


**Learner**

"A: used to drink. The message is what she did before, and now she drinks tea. For something normal now, can I say 'I am used to studying English every morning'? So I should choose the meaning first, not just see the word 'to'."


**Assistant**

Yes—**A is correct.** “But now she only drinks tea” shows a change from what Maya did before.

And **“I am used to studying English every morning”** is correct. It means that studying English every morning **feels normal or familiar to you**.

Exactly: **choose the meaning first**, rather than deciding from the word *to* alone.

- **I used to study…** → I did it before.
- **I am used to studying…** → It feels normal to me.

Your answer here shows that you used the meaning and the “now” clue to choose the right expression.


#### Ambiguity, fair practice and past familiarity


**Learner**

"Why is my answer wrong?"


**Assistant**

Your answer, **“am used to living,” is grammatically correct**. It should not have been marked wrong based on this question’s wording.

Both completions work, but they mean different things:

- **“I used to live in Tokyo”** means I lived there in the past, implying that I don’t now.
- **“I am used to living in Tokyo”** means living there feels familiar or normal to me.

The answer key chose the past-state meaning, but the question only asked for a **grammatically correct** completion. Without context specifying the intended meaning, either answer is defensible. This was a problem with the question, not necessarily with your understanding.


**Learner**

"That helps, but the app still shows my answer in red. Should I stop using 'I am used to living in Tokyo'? What words would make only one answer correct? And has this conversation fixed my original wrong result or next recall time?"


**Assistant**

No—you should **keep using “I am used to living in Tokyo”** when you mean that living there feels familiar or normal. The red marking doesn’t make that sentence incorrect.

To make only one choice fit, the question needs a clear meaning cue:

- **Past state:** “To say that Tokyo was your home in the past but is not now, complete: I ___ in Tokyo.”  
  → **used to live**
- **Being accustomed:** “To say that living in Tokyo feels normal to you now, complete: I ___ in Tokyo.”  
  → **am used to living**

These cues distinguish the intended meaning; both expressions remain grammatically valid.

**This conversation has not changed your original marked result or next recall time.** We’ve identified a flaw in the question, but that discussion does not update the app’s grade, memory tracker, or recall schedule.


**Learner**

"Give me one different, related practice question. Wait for my answer before explaining it."


**Assistant**

You started working night shifts a year ago. Now the schedule feels normal to you.

Which sentence expresses that meaning?

**A.** I used to work at night.  
**B.** I am used to working at night.


**Learner**

"B. 'I am used to working at night' says the schedule feels normal now. A would talk about my past habit. This question is fairer because it tells me the meaning to express."


**Assistant**

Correct—**B** expresses that working at night feels normal to you now.

Your distinction is right:
- **Be used to + -ing** means being accustomed to something.
- **Used to + base verb** describes a past habit or state, usually contrasting with the present.

And yes, this question is fairer because it specifies the intended meaning. Your answer shows you applied the distinction correctly here; it does not change the original app result or recall schedule.


**Learner**

"One more thing: what about 'I was used to living in Tokyo'? That is about the past too. If I remember 'used to = past' and 'be used to = normal now', will I make another mistake? Please explain simply."


**Assistant**

Yes—that shortcut could lead to another mistake. **“Be used to” means “be accustomed to,” not necessarily “normal now.”** The form of **be** tells you the time.

- **I am used to living in Tokyo.** → It feels normal to me **now**.
- **I was used to living in Tokyo.** → It felt normal to me **at that time in the past**. This alone doesn’t tell us how I feel now.
- **I used to live in Tokyo.** → I lived there in the past, with an implied contrast to now.

A safer memory rule is:

**used to + base verb = past habit or state**  
**be used to + noun / -ing = accustomed or familiar**

So first ask **“past habit/state, or familiarity?”** For familiarity, then look at **am/is/are, was/were**, etc., to find the time.
