# 0001 — Ubiquitous language for Donut domain concepts

**Status:** Accepted  
**Date:** 2026-08-15  
**Decision makers:** Terry
**Consulted:** (people / teams asked for advice)

## Context

Donut’s product vocabulary should be **consistent**: each idea has one name,
and each name means one thing. Humans, UI copy, APIs, and coding agents then
share those terms.

This ADR is the **canonical ubiquitous language**. The glossary below is
the source of truth; prefer these meanings in UI copy, APIs, tests, and
code identifiers. This glossary is amended in place. Add or change domain
terms here; do not supersede this ADR with a new one. Spaced-repetition domain
concepts, including morning recall residual measurement, live in [ADR
0003](./0003-spaced-repetition-scheduling-policy-accepted.md).

## Notebook / note structure

- **Note** — Atomic knowledge document (title, content, markdown / rich
  content, frontmatter; may be nested under another note)
- **Notebook** — Top-level collection of notes a user owns or
  subscribes to
- **Notebook short description** — One-line short plain-text description
  of a notebook. Shown in the **notebook catalog**. Distinct from the
  **Readme**.
- **Folder** — Hierarchical path segment inside a notebook
- **Portable notebook tree** — Canonical OKF-compatible Markdown
  representation of one **notebook** as files and folders. It does not use
  Donut server note IDs or SPA locations as notebook addresses and round-trips
  under the profile in
  [ADR 0004](./0004-okf-compatible-notebook-markdown-accepted.md). Portable
  means decoupled from Donut's private and web identities; it does not mean
  that paths survive rename or that every Markdown tool understands every
  Donut profile extension.
- **Portable path** — Address of a **note** in a **portable notebook tree**,
  optionally qualified by **notebook** and optionally extended with a
  **property** selector. It may have a bundle-root or shorthand form;
  source-relative forms fit the same model where supported. Its resolution
  scope is one portable notebook tree: the source notebook unless explicitly
  notebook-qualified. A shorthand Portable path resolves only when it
  identifies one destination under the documented resolution scope. Otherwise
  it is unresolved/ambiguous, and Donut asks for a longer path. A Portable path
  is not a Donut note ID, SPA location, or stable identity across rename.
- **Readme** — Markdown body on a notebook or folder (landing content).
  Also bears YAML frontmatter for that notebook or folder. In the
  portable tree, maps to that directory’s `README.md` with
  `type: Readme` (not OKF `index.md`). Distinct from **notebook short
  description**. Qualified: **notebook readme**, **folder readme**.
- **Notebook catalog** — List of a user’s notebooks (and notebook
  groups). The catalog heading is **Notebooks**. Catalog-wide search is
  **All notebooks**.
- **Subscribed notebook** — A shared notebook the user follows via a
  **subscription**; appears in the **notebook catalog**
- **Notebook group** — Named catalog grouping of notebooks. Distinct
  from a **Circle**.
- **Circle** — Multi-user shared space with members and notebooks
- **Bazaar** — Marketplace where notebooks are shared for others to
  browse and subscribe
- **Wiki link** — In-content reference to a **note** or a **property**
  on a note. Donut-authored form is wiki `[[target]]` /
  `[[target|display]]` (optionally `Notebook:Title`; a property appends
  `#prop:<encoded-key>`). Path Markdown is the same link. Spelling and
  property-key encoding:
  [ADR 0004](./0004-okf-compatible-notebook-markdown-accepted.md). Web
  destination: `noteProperty`, once Proposed
  [ADR 0005](./0005-web-routes.md) is Accepted — human-owned exception
  trailed in `.planning/quick/013-note-property-canonical-path/PLAN.md`.
- **Property** — YAML frontmatter key–value on a note (scalar or
  one-level list). Distinct from a **relationship note**. A value may
  contain **wiki links**. Relation-like keys (`example of`, `a part of`)
  are still properties. Portable identity is (concept path, exact YAML
  key) via `#prop:<encoded-key>` (ADR 0004). Web canonical location is
  `noteProperty`, once Proposed ADR 0005 is Accepted (see above).
- **Relationship** — Typed association between notes (e.g. “similar to”,
  “a part of”)
- **Relationship note** — A note that represents a relationship
  (`type: Relationship` in frontmatter), with source, target, and
  relation. Source and target are **wiki links** (same dual-spelling as
  the body;
  [ADR 0004](./0004-okf-compatible-notebook-markdown-accepted.md)).
- **Wikidata association** — Binding a note to a Wikidata entity. The
  action is **Associate Wikidata**. Distinct from **Wikidata ID** (the
  Q-id).
- **Skip Memory Tracking** — Notebook setting that opts the notebook out
  of the assimilation sequence and blocks Bazaar subscribe. Distinct
  from **Remove from recall**.

## Book

- **Book** — Attached reading artifact (EPUB, PDF, …), distinct from a
  notebook
- **Book layout** — Structure of an attached book
- **Reading record** — Progress through a book

## Refinement, health, and search

- **Refinement layout** — Layout for decomposing and improving a note
  while refining
- **Notebook health** — Current findings state of a notebook (empty
  folders, readme-only folders, dead wiki links). Shown on the notebook
  **Health** tab. Distinct from **lint** (the check action).
- **Lint** — Action that checks **notebook health** and returns findings.
  Does not mutate the notebook. Short UI: **Run lint**.
- **Fix** — Applies selected repairs from current **notebook health**
  findings, then typically lints again
- **Semantic search** — Meaning-based find of notes via embeddings

## Assimilation

- **Subscription** — Following a shared notebook (from the Bazaar or a
  Circle) with a daily assimilation quota. The action is **subscribe**.
- **Assimilation** — First-pass intake of a note into the learner’s
  memory schedule. Creates a **New** memory tracker
  ([ADR 0003](./0003-spaced-repetition-scheduling-policy-accepted.md)).
- **Assimilation sequence** — Ordered units offered as next-to-assimilate
  (menu walkthrough, `/next`). Distinct from assimilating on a note. A
  unit is in the sequence iff it has not been skipped from the sequence
  and has no live understanding memory tracker.
- **Skip from the assimilation sequence** — Mark a unit so it is not
  offered as next. Distinct from assimilating that unit from the note
  (any tracker type). Short UI: **Skip**
- **Return to sequence** — Delete the sequence-skip row; the unit is
  pending in the walkthrough again. Short UI: **Return to sequence**
- **Understanding memory tracker** — Note-level tracker created by
  ordinary Assimilate. A spelling or commissioned tracker does not
  satisfy ordinary sequence due.
- **Spelling memory tracker** — Note-level tracker for recalling the note
  title by spelling. The learner creates it.
- **Remember spelling** — Learner action at assimilation: verify the note
  title (or alias), then create a spelling memory tracker
- **Property memory tracker** — Understanding memory tracker keyed by a
  **property** name (the frontmatter key).
- **Daily assimilation target** — Max new understanding memory trackers
  to create per day (profile or subscription). Spelling and commissioned
  trackers do not consume this count.

## Recall

- **Memory tracker** — One learner's scheduled memory for a note (or
  **property**). Types (how it is created): **understanding**,
  **spelling**, **property**, **commissioned**.
- **Memory tracking** — Creating and maintaining memory trackers.
  Tracker-level opt-out is **Remove from recall**.
- **Recall** — Spaced retrieval of assimilated material. Donut name
  for FSRS **review** (recall is better than review). Methods: **recall
  prompt** or **just review**.
- **Recall prompt** — One ask during recall for a memory tracker. Kinds:
  **spelling** (no MCQ) or **MCQ** (the prompt HAS_A an MCQ). An MCQ is
  not a type of recall prompt.
- **MCQ** — Multiple-choice content on a note (stem, choices, solution).
  A recall prompt may have an MCQ; an MCQ is not a type of recall prompt.
  Origin (AI-generated vs manually added) is how the content was produced,
  not a prompt kind.
- **Contested** — Marks an MCQ as not feasible. Distinct from a kind of
  recall prompt.
- **Contest** — Challenge an MCQ shown in a recall prompt; the MCQ may be
  marked contested and replaced.
- **Just review** — Recall by reviewing the note and self-evaluating
  with **Grade** Good or Again. Short UI: **Just review**.
- **Grade** — The single scheduling evaluation concept: Again (1),
  Hard (2), Good (3), Easy (4). The numeric value **is** FSRS `G`.
  Used by recall prompts, **just review**, and Tutor **Feedback**.
  **Confusion** and **Overlap** are not grades. Scheduling detail:
  [ADR 0003](./0003-spaced-repetition-scheduling-policy-accepted.md).
- **Answer** — The learner's response to a **recall prompt**. Prompt
  grades and **confusion** link a RecallLog row to this answer.
- **Accidental match** — Spelling answer that fails the note under recall
  but names another accessible note by title or plain alias. Transitions:
  [ADR 0003](./0003-spaced-repetition-scheduling-policy-accepted.md).
- **Remove from recall** — Stop an existing memory tracker from appearing
  in recall; the unit does not re-enter the assimilation sequence. Short
  UI: **Remove** / **Remove from recall**. Not a grade.
- **Revive** — Re-enable recall for a tracker that was removed from
  recall. Short UI: **Revive**. Not a grade.
- **Pace**, **Accuracy**, **Consistency**, and **Retrieval lapse** —
  Separate residual readouts of a morning's recall against expectation
  and the learner's own baseline. Not diagnostic of cause. There is no
  composite index of them. Measurement:
  [ADR 0003](./0003-spaced-repetition-scheduling-policy-accepted.md).
- **Daily probe** — Optional opt-in standalone task before recall,
  independent of recall item content. Not a scheduling input and not a
  check on Pace, Accuracy, or Consistency. Measurement:
  [ADR 0003](./0003-spaced-repetition-scheduling-policy-accepted.md).

**New**, **Stability**, **Difficulty**, **Retrievability**, **RecallLog**,
**Thinking time**, **Confusion**, and **Overlap**: **Spaced repetition
glossary** in [ADR 0003](./0003-spaced-repetition-scheduling-policy-accepted.md).

## Focus context

- **Focus context** — Bounded neighborhood around a **focus note** (depth
  0) plus related notes reached by wiki links, inbound references, and
  sampled folder peers, within a token budget. Used for conversation,
  recall-prompt generation, note automation, Learning Session Request,
  and export.
- **Focus note** — The center note of a **focus context** (depth 0).

## Conversation

- **Conversation** — Thread of messages about a **note** or **recall
  prompt**. Participants may be humans and/or the **AI Assistant**.
- **Message** — One utterance in a conversation
- **Message center** — Inbox UI for conversations and unread state.
  Short UI: **Messages**
- **AI Assistant** — Conversation participant that sends messages with no
  human sender. Short UI: **AI Assistant**

## Commissioned Learning Session

Vocabulary for Learning Sessions that a Tutor conducts outside Donut, on
commission from Donut:

- **Learning Orchestrator** — The Donut component that directs and
  coordinates Learning Sessions
- **Commissioned memory tracker** — Memory tracker maintained through
  commissioned Learning Sessions rather than ordinary **recall**
- **Tutor** — Party that conducts a Learning Session from the request and
  produces a report; may be a person or an AI assistant, and is outside
  Donut
- **Learning Session** — One commissioned unit of tutoring, covering the
  due commissioned memory trackers of a single notebook
- **Potential learning session** — Due commissioned memory trackers that
  could be commissioned but have no Learning Session yet
- **Learning Session Request** — The document Donut gives the Tutor to
  conduct the session
- **Learning Session Report** — The document the Tutor returns, carrying
  Feedback per Session Item
- **Session Item** — One memory tracker within a Learning Session, and
  the Feedback recorded for it
- **Feedback** — A Tutor's evaluation of a Session Item: a **Grade** and
  descriptive text. Recommendations of what to study next are not Feedback.

Request/Report documents:
[commissioned learning session protocol](../commissioned-learning-session-protocol.md).
Scheduling of recorded Grades:
[ADR 0003](./0003-spaced-repetition-scheduling-policy-accepted.md).

## Alignment policy

- Product and internal names use **Donut** / `donut`. For now, `doughnut` is
  allowed only as the literal identifier of a live external resource this
  product does not migrate (GitHub repo `nerds-odd-e/doughnut`, site
  `https://doughnut.odd-e.com`, GCS objects and buckets, MySQL user and
  databases, Gitpod image, names under `infra/gcp/**`) — not in product
  prose, UI, or new identifiers.
- Features, tests, OpenAPI names, and packages follow this glossary.
  Spaced-repetition domain concepts follow the **Spaced repetition glossary** in
  [ADR 0003](./0003-spaced-repetition-scheduling-policy-accepted.md).
- Same nouns in UI, API, and schema, with **minimum DTO**. Do not
introduce a translation type that wraps one as the other.
- Agents treat this ADR as binding for naming choices. Spaced-repetition domain
  concepts follow ADR 0003. Humans and agents share an explicit dictionary
  instead of inferring synonyms.

## Prerequisites / Assumptions

- Spaced-repetition domain concepts are constrained by the **Spaced repetition
  glossary** in
  [ADR 0003](./0003-spaced-repetition-scheduling-policy-accepted.md).

## Related

- Links: playbook [README.md](./README.md); ADR-0000
[use-adrs-accepted.md](./0000-use-adrs-accepted.md); ADR 0003
[spaced-repetition scheduling policy](./0003-spaced-repetition-scheduling-policy-accepted.md)
(**Spaced repetition glossary**, morning recall residuals); ADR 0004
[OKF-compatible notebook Markdown](./0004-okf-compatible-notebook-markdown-accepted.md)
(portable Markdown profile); ADR 0005
[web routes](./0005-web-routes.md) (`noteShow` / `noteProperty`); [commissioned learning session
protocol](../commissioned-learning-session-protocol.md) (Request/Report
documents)
