# 0001 — Ubiquitous language for Donut domain concepts

**Status:** Accepted  
**Decision makers:** Terry
**Consulted:** (people / teams asked for advice)

## Context

This glossary is Donut's **canonical ubiquitous language**: one name per idea,
one meaning per name, shared by humans, UI copy, APIs, tests, and code.
Amend domain terms here in place; do not supersede this ADR.

## Notebook / note structure

- **Note** — Atomic knowledge document with title, content, and frontmatter,
  represented as Markdown. Its server-side identity anchors learning data.
- **Notebook** — Top-level collection of notes, attachments, and folders a user
  owns or subscribes to. Content can reside at its root or within folders.
- **Notebook short description** — Short, one-line plain-text description in
  the **notebook catalog**, distinct from the **Readme**.
- **Folder** — Named hierarchical location containing notes, attachments, and
  child folders within a notebook.
- **Attachment** — A named supporting file owned by a notebook and located at
  its root or in a folder, with its original bytes preserved. Includes IDE
  rules, skills, and images; it requires neither a referring note nor OKF
  frontmatter and is not a learning unit.
- **Image** — An Attachment with image presentation capabilities.
- **Attachment reference** — Authored reference to an Attachment, distinct from
  a note/property Wiki link. References do not own the file's lifetime.
- **Trash** — Recoverable removal of a note or folder by moving it beneath the
  notebook-root `_trash/`. The entity keeps its identity and identity-bound
  data, including learning history where applicable; moving it out recovers it.
- **Permanent deletion** — Removal of an entity and its dependent data. It ends
  that identity; recreating the same content, even at the same path, creates a
  new identity.
- **Portable notebook tree** — A notebook's content as folders, OKF-profile
  note and container-Readme files, attachments, and structural markers. Private
  identities and learning data, including recall history, stay outside it.
  Addresses use no server note IDs or SPA locations. It round-trips under
  [ADR 0004](./0004-okf-compatible-notebook-markdown-accepted.md). Portability
  promises neither paths stable across rename nor universal Markdown support.
- **Portable path** — Address of a **note** in a **portable notebook tree**,
  optionally notebook-qualified or extended with a **property** selector.
  Bundle-root, shorthand, and supported source-relative forms share this model.
  Resolution is scoped to the source notebook unless explicitly qualified.
  Shorthand must identify exactly one destination; otherwise it remains
  unresolved/ambiguous and Donut asks for a longer path. It is not a note ID,
  SPA location, or stable identity across rename.
- **Readme** — Notebook/folder landing Markdown with its YAML frontmatter;
  maps to that directory's `README.md` with `type: Readme`, not OKF `index.md`.
  Say **notebook readme** or **folder readme**; distinct from short description.
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
- **Wiki link** — Semantic in-content reference to a **note** or a **property**
  using `[[portable-path]]` or `[[portable-path|display]]`, with optional
  `Notebook:Title` or `#prop:<encoded-key>` qualification.
  A Markdown link to a canonical Donut note URL also participates by note ID;
  its label is display only. Spelling/encoding follows
  [ADR 0004](./0004-okf-compatible-notebook-markdown-accepted.md); web destinations
  follow [ADR 0005](./0005-web-routes-accepted.md) (`noteShow` / `noteProperty`).
- **Property** — YAML frontmatter key–value on a note (scalar or
  one-level list). Distinct from a **relationship note**. A value may
  contain **wiki links**. Relation-like keys (`example of`, `a part of`)
  are still properties. Portable identity is (Portable path, exact YAML
  key) via `#prop:<encoded-key>` (ADR 0004). Web canonical location is
  `noteProperty` ([ADR 0005](./0005-web-routes-accepted.md)).
- **Relationship** — Typed association between notes (e.g. “similar to”,
  “a part of”)
- **Relationship note** — A note that represents a relationship
  (`type: Relationship` in frontmatter), with source, target, and
  relation. Source and target are **wiki links**
  ([ADR 0004](./0004-okf-compatible-notebook-markdown-accepted.md)).
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
  memory schedule. Creates a **New** memory tracker.
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

More detailed **Spaced repetition glossary** is in
[ADR 0003](./0003-spaced-repetition-scheduling-policy-accepted.md).

- **Memory tracker** — One learner's scheduled memory for a note (or
  **property**). Types (how it is created): **understanding**,
  **spelling**, **property**, **commissioned**.
- **Memory tracking** — Creating and maintaining memory trackers.
  Tracker-level opt-out is **Remove from recall**.
- **Recall** — Spaced retrieval of assimilated material; Donut's name for FSRS
  **review**. Methods: **recall prompt** or **just review**.
- **Recall prompt** — One ask for a memory tracker: **spelling** (no MCQ) or
  an **MCQ** prompt containing an MCQ. The MCQ is content, not the prompt itself.
- **MCQ** — Multiple-choice note content (stem, choices, solution) a recall
  prompt may contain. AI-generated or manually added describes content origin,
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
  **Confusion** and **Overlap** are not grades.
- **Answer** — The learner's response to a **recall prompt**. Prompt
  grades and **confusion** link a RecallLog row to this answer.
- **Accidental match** — Spelling answer that fails the note under recall
  but names another accessible note by title or plain alias.
- **Remove from recall** — Stop an existing memory tracker from appearing
  in recall; the unit does not re-enter the assimilation sequence. Short
  UI: **Remove** / **Remove from recall**. Not a grade.
- **Revive** — Re-enable recall for a tracker that was removed from
  recall. Short UI: **Revive**. Not a grade.
- **Pace**, **Accuracy**, **Consistency**, and **Retrieval lapse** —
  Separate residual readouts of a morning's recall against expectation
  and the learner's own baseline. Not diagnostic of cause. There is no
  composite index of them.
- **Daily probe** — Optional opt-in standalone task before recall,
  independent of recall item content. Not a scheduling input and not a
  check on Pace, Accuracy, or Consistency.

## Focus context

- **Focus context** — Bounded neighborhood around a **focus note** (depth 0):
  notes reached through wiki links and inbound references, plus sampled folder
  peers, within a token budget. Used for conversation, recall-prompt generation,
  note automation, Learning Session Request, and export.
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

External tutoring commissioned by Donut:

- **Learning Orchestrator** — The Donut component that directs and
  coordinates Learning Sessions
- **Commissioned memory tracker** — Memory tracker maintained through
  commissioned Learning Sessions rather than ordinary **recall**
- **Tutor** — Person or AI assistant outside Donut who conducts a Learning
  Session from its request and produces a report.
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

## Operations

- **Failure report** — Consecutive run of similar failures. One admin
  list entry and one GitHub issue. Short UI: **Failure Reports**.
  Similarity, consecutive-run, GitHub count updates, and fail-loud usage:
  [ADR 0006](./0006-failure-handling-accepted.md).

## Alignment policy

- Product and internal names use **Donut** / `donut`. For now, `doughnut` is
  allowed only as the literal identifier of a live external resource this
  product does not migrate (GitHub repo `nerds-odd-e/doughnut`, site
  `https://doughnut.odd-e.com`, GCS objects and buckets, MySQL user and
  databases, Gitpod image, names under `infra/gcp/**`) — not in product
  prose, UI, or new identifiers.
- Features, tests, OpenAPI names, and packages follow this glossary.
- Same nouns in UI, API, and schema, with **minimum DTO**. Do not
  introduce a translation type that wraps one as the other.
- Agents follow this glossary for naming, ADR 0003 for spaced-repetition domain
  concepts, and ADR 0006 for failure handling.
