# 0001 — Ubiquitous language for Doughnut domain concepts

**Status:** Accepted  
**Date:** 2026-08-15  
**Decision makers:** Terry
**Consulted:** (people / teams asked for advice)

## Context

Doughnut’s product vocabulary should be **consistent**: each idea has one name,
and each name means one thing. Humans, UI copy, APIs, and coding agents then
share those terms.

This ADR is the **canonical ubiquitous language**. The glossary below is
the source of truth; prefer these meanings in UI copy, APIs, tests, and
code identifiers. This glossary is amended in place. Add or change domain
terms here; do not supersede this ADR with a new one.

## Notebook / note structure

- **Note** — Atomic knowledge document (title, content, markdown / rich
  content, frontmatter; may be nested under another note)
- **Notebook** — Top-level collection of notes a user owns or
  subscribes to
- **Notebook short description** — One-line short plain-text description
  of a notebook. Shown in the **notebook catalog**. Distinct from the
  **Readme**.
- **Folder** — Hierarchical path segment inside a notebook
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
- **Wiki link** — In-content reference to a note. Doughnut-authored form
  is wiki `[[target]]` / `[[target|display]]` (optionally
  `Notebook:Title`). Path Markdown `[display](/folder/File.md)` is the
  same link, not a second kind. Spelling rules:
  [ADR 0004](./0004-okf-compatible-notebook-markdown-accepted.md).
- **Property** — YAML frontmatter key–value on a note (scalar or
  one-level list). Distinct from a **relationship note**. A value may
  contain **wiki links**. Relation-like keys (`example of`, `a part of`)
  are still properties.
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
  from opting out of **recall**.

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

## Assimilation and recall

- **Subscription** — Following a shared notebook (from the Bazaar or a
  Circle) with a daily assimilation quota. The action is **subscribe**.
- **Assimilation** — First-pass intake of a note into the learner’s
  memory schedule
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
- **Recall** — Spaced retrieval of assimilated material. Doughnut names
  the activity **recall**, not FSRS/Anki **review**. Methods: **recall
  prompt** or **just review**.
- **Recall prompt** — One ask during recall for a memory tracker. Kinds:
  **spelling** (no MCQ) or **MCQ** (the prompt HAS_A an MCQ). An MCQ is
  not a type of recall prompt.
- **MCQ** — Multiple-choice content on a note (stem, choices, solution).
  A recall prompt may have an MCQ; an MCQ is not a type of recall prompt.
  **Contested** marks an MCQ as not feasible. Origin (AI-generated vs
  manually added) is how the content was produced, not a prompt kind.
- **Contested** — Marks an MCQ as not feasible. Distinct from a kind of
  recall prompt.
- **Contest** — Challenge an MCQ shown in a recall prompt; the MCQ may be
  marked contested and replaced.
- **Just review** — Recall by reviewing the note and self-evaluating. A
  method of recall. Short UI: **Just review**
- **Accidental match** — Recall result that matches an unintended note
- **Memory tracking** — Creating and maintaining memory trackers for
  notes. Tracker-level opt-out is **Remove from recall**.
- **Remove from recall** — Stop an existing memory tracker from appearing
  in recall; the unit does not re-enter the sequence. Short UI:
  **Remove** / **Remove from recall**
- **Revive** — Re-enable recall for a tracker that was removed from
  recall. Short UI: **Revive**
- **Property memory tracker** — Understanding memory tracker keyed by a
  **property** name (the frontmatter key).
- **Stability** — Persisted current interval of a memory tracker, in
  whole hours. After a grade, next recall time is last recalled time plus
  `I(r, S)` with **requested retention** `r` locked at 0.9 (so due hours
  equal Stability hours). A **New** tracker has Stability 0 (due now).
  Short UI: **Stability**
- **New** — Memory tracker that is ungraded (`S = 0`, Difficulty unset /
  **N/A**; Difficulty:
  [ADR 0003](./0003-spaced-repetition-scheduling-policy.md)).
  Assimilation is not a grade; confusion is not a grade. Not “never
  succeeded.” After any mapped grade the tracker is no longer New.
- **Requested retention** — Target retrievability at the next due. Locked
  globally at **0.9**. At this `r`, open FSRS `I(0.9, S) = S` in whole
  hours. May be shown read-only in recall statistics (e.g. the heatmap
  color anchor).
- **Retrievability** — Computed from elapsed whole hours and Stability;
  not stored.
- **RecallLog** — One persisted memory-state transition for a memory
  tracker. Doughnut’s name for the FSRS-shaped review history (review
  (FSRS) = recall). Prompt grades and confusion link an **answer**; just
  review and Tutor Feedback do not. Shape:
  [ADR 0003](./0003-spaced-repetition-scheduling-policy.md).
- **Daily assimilation target** — Max new understanding memory trackers
  to create per day (profile or subscription). Spelling and commissioned
  trackers do not consume this count.

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

Vocabulary for Learning Sessions that a Tutor conducts outside Doughnut, on
commission from Doughnut:

- **Learning Orchestrator** — The Doughnut component that directs and
  coordinates Learning Sessions
- **Commissioned memory tracker** — Memory tracker maintained through
  commissioned Learning Sessions rather than ordinary recall
- **Tutor** — Party that conducts a Learning Session from the request and
  produces a report; may be a person or an AI assistant, and is outside
  Doughnut
- **Learning Session** — One commissioned unit of tutoring, covering the
  due commissioned memory trackers of a single notebook
- **Potential learning session** — Due commissioned memory trackers that
  could be commissioned but have no Learning Session yet
- **Learning Session Request** — The document Doughnut gives the Tutor to
  conduct the session
- **Learning Session Report** — The document the Tutor returns, carrying
  Feedback per Session Item
- **Session Item** — One memory tracker within a Learning Session, and
  the Feedback recorded for it
- **Feedback** — A Tutor's evaluation of a Session Item (score, and later
  descriptive feedback and recommendations)

## Alignment policy

- Features, tests, OpenAPI names, and packages follow this glossary.
- Same nouns in UI, API, and schema, with **minimum DTO**. Do not
introduce a translation type that wraps one as the other.
- Agents treat this ADR as binding for naming choices. Humans and agents share an explicit dictionary instead of inferring synonyms.

## Prerequisites / Assumptions

- No Accepted ADR yet constrains domain naming (only ADR-0000 on using ADRs).

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links: playbook [README.md](./README.md); ADR-0000
[use-adrs-accepted.md](./0000-use-adrs-accepted.md); ADR 0003
[spaced-repetition scheduling policy](./0003-spaced-repetition-scheduling-policy.md)
(FSRS **review** = Doughnut **recall**; **Stability** / **New** /
**Retrievability** / **RecallLog**); ADR 0004
[OKF-compatible notebook Markdown](./0004-okf-compatible-notebook-markdown-accepted.md)
(portable Markdown profile)
