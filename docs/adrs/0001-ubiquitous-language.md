# 0001 — Ubiquitous language for Doughnut domain concepts

**Status:** Proposed  
**Date:** 2026-08-15  
**Decision makers:** Terry
**Consulted:** (people / teams asked for advice)

## Context

Doughnut’s product vocabulary should be **consistent**: each idea has one name,
and each name means one thing. Humans, UI copy, APIs, and coding agents then
share those terms.

This ADR is the **canonical ubiquitous language**. Accepting it constrains
*new* naming and guides gradual alignment (tests, UI, OpenAPI, schema). It
does not require renaming the whole codebase at once.

### Remaining collisions (still to tighten in this glossary)

#### Ambiguous (one word, several meanings)

| Term | Colliding senses |
|------|------------------|
| **Property** | Reduced relationship field; wiki property from accidental match; property memory tracker key |
| **Chat / conversation / message** | Human note threads and AI chat share the same nouns without a clear split |
| **My notes / my notebooks** | Notebook catalog; note search scope; subscribed notebook appearing among the user’s notebooks |
| **Description / Readme** | Catalog blurb (`notebook description`) vs notebook/folder markdown body (`readme`) — both “about the notebook” but different fields |

#### Redundant (several names, one concept)

| Concept | Overlapping names |
|---------|-------------------|
| Following a shared notebook | subscribe, subscription |
| Binding to Wikidata | Wikidata association, Wiki association, associate Wikidata ID |
| Meaning-based find | semantical search, semantic search |

#### Missing or weakly named

| Gap | Why it matters |
|-----|----------------|
| **Focus context** | Used in AI flows but not established as a first-class domain noun |
| **OKF** (Open Knowledge Format) | Named in CLI lint but not introduced as a first-class glossary noun |
| **Owned vs subscribed notebook** | Distinction exists in behavior but not as glossary entries |
| **Notebook health** vs export/lint tooling | Same findings idea at two layers without a shared term family |

## Decision

1. **Maintain a glossary** — Keep the dictionary in this ADR (Accepted form) as
   the source of truth for product/domain naming. Phase plans and renames may
   cite it; they must not invent competing canonical names silently.

2. **Canonical terms** — Prefer these meanings in new UI copy, APIs, tests, and
   code identifiers.

#### Notebook / note structure and OKF

| Term | Meaning |
|------|---------|
| **Note** | Atomic knowledge document (title, content, markdown / rich content, frontmatter; may be nested under another note) |
| **Notebook** | Top-level collection of notes a user owns or subscribes to |
| **Notebook description** | One-line short plain-text blurb of a notebook (catalog/settings; not markdown, not the Readme) |
| **Folder** | Hierarchical path segment inside a notebook |
| **Readme** | Markdown body on a notebook or folder (landing content; separate from notebook description) |
| **Notebook catalog** | List of a user’s notebooks (and notebook groups) |
| **Notebook group** | Named catalog grouping of notebooks (not a Circle) |
| **Circle** | Multi-user shared space with members and notebooks |
| **Bazaar** | Marketplace where notebooks are shared for others to browse and subscribe |
| **Wiki link** | In-content `[[…]]` reference to a note (optionally `Notebook:Title`) |
| **Relationship** | Typed association between notes (e.g. “similar to”, “a part of”) |
| **Relationship note** | A note that represents a relationship (`type: relationship` in frontmatter), with source, target, and relation |
| **Wikidata association** | Binding a note to a Wikidata entity (also called Wiki association) |
| **Refinement layout** | Layout for decomposing and improving a note while refining |
| **Book** | Attached reading artifact (EPUB, PDF, …), distinct from a notebook |
| **Book layout** | Structure of an attached book |
| **Reading record** | Progress through a book |
| **OKF** | [Open Knowledge Format](https://cloud.google.com/blog/products/data-analytics/how-the-open-knowledge-format-can-improve-data-sharing): portable directory of markdown concept files with YAML frontmatter |
| **Notebook health** | In-app lint, findings, and fixes for a notebook |
| **Skip Memory Tracking** | Notebook setting that opts the notebook out of the assimilation sequence and blocks Bazaar subscribe. It does not opt the notebook out of recall. |

#### Assimilation and recall

| Term | Meaning | Short UI |
|------|---------|----------|
| **Subscription** | Following a shared notebook (from the Bazaar or a Circle) with a daily assimilation quota | |
| **Assimilation** | First-pass intake of a note into the learner’s memory schedule | |
| **Assimilation sequence** | Ordered units offered as next-to-assimilate (menu walkthrough, `/next`). Distinct from assimilating on a note. A unit is in the sequence iff it has not been skipped from the sequence and has no live understanding memory tracker. | |
| **Skip from the assimilation sequence** | Mark a unit so it is not offered as next. Does not block assimilating that unit from the note (any tracker type). | **Skip** |
| **Return to sequence** | Delete the sequence-skip row; the unit is pending in the walkthrough again | **Return to sequence** |
| **Understanding memory tracker** | Note-level tracker created by ordinary Assimilate. A spelling or commissioned tracker does not satisfy ordinary sequence due. | |
| **Spelling memory tracker** | Note-level tracker for recalling the note title by spelling. The learner creates it. | |
| **Remember spelling** | Learner action at assimilation: verify the note title (or alias), then create a spelling memory tracker | |
| **Recall** | Spaced retrieval of assimilated material. Doughnut names the activity **recall**, not FSRS/Anki **review**. Methods: **recall prompt** or **just review**. | |
| **Recall prompt** | One ask during recall for a memory tracker. Kinds: **spelling** (no MCQ) or **MCQ** (the prompt HAS_A an MCQ). An MCQ is not a type of recall prompt. | |
| **MCQ** | Multiple-choice content on a note (stem, choices, solution). A recall prompt may have an MCQ; an MCQ is not a type of recall prompt. **Contested** is a property of an MCQ. Origin (AI-generated vs manually added) is how the content was produced, not a prompt kind. | |
| **Contested** | Property of an MCQ: marked not feasible. Not a kind of recall prompt. | |
| **Contest** | Challenge an MCQ shown in a recall prompt; the MCQ may be marked contested and replaced. | |
| **Just review** | Recall by reviewing the note and self-evaluating. A method of recall, not the absence of a prompt. | **Just review** |
| **Accidental match** | Recall result that matches an unintended note | |
| **Memory tracking** | Creating and maintaining memory trackers for notes. Tracker-level opt-out is **Remove from recall**. | |
| **Remove from recall** | Stop an existing memory tracker from appearing in recall; the unit does not re-enter the sequence | **Remove** / **Remove from recall** |
| **Revive** | Re-enable recall for a tracker that was removed from recall | **Revive** |
| **Property memory tracker** | Recall tracking keyed by a property or relationship label | |
| **Stability** | Persisted current interval of a memory tracker, in whole hours. After a grade, next recall time is last recalled time plus `I(r, S)` with **requested retention** `r` locked at 0.9 (so due hours equal Stability hours). A newly assimilated tracker may have Stability 0 (due now). | **Stability** |
| **Requested retention** | Target retrievability at the next due. Locked globally at **0.9** — not a Settings knob, not in the UI, not persisted. At this `r`, open FSRS `I(0.9, S) = S` in whole hours. | |
| **Retrievability** | Computed from elapsed whole hours and Stability; not stored. | |
| **Daily assimilation target** | Max new understanding memory trackers to create per day (profile or subscription). Spelling and commissioned trackers do not consume this count. | |

#### Conversation and focus

| Term | Meaning |
|------|---------|
| **Conversation** | Thread of messages about a note (human or AI participant) |
| **Message** | One utterance in a conversation |
| **Message center** | UI for conversations and unread state |
| **Focus context** | Bounded note neighborhood for AI use |

3. **Commissioned Learning Session terms** — Vocabulary for Learning Sessions
   that a Tutor conducts outside Doughnut, on commission from Doughnut:

| Term | Meaning |
|------|---------|
| **Learning Orchestrator** | The Doughnut component that directs and coordinates Learning Sessions |
| **Commissioned memory tracker** | Memory tracker maintained through commissioned Learning Sessions rather than ordinary recall |
| **Tutor** | Party that conducts a Learning Session from the request and produces a report; may be a person or an AI assistant, and is outside Doughnut |
| **Learning Session** | One commissioned unit of tutoring, covering the due commissioned memory trackers of a single notebook |
| **Potential learning session** | Due commissioned memory trackers that could be commissioned but have no Learning Session yet |
| **Learning Session Request** | The document Doughnut gives the Tutor to conduct the session |
| **Learning Session Report** | The document the Tutor returns, carrying Feedback per Session Item |
| **Session Item** | One memory tracker within a Learning Session, and the Feedback recorded for it |
| **Feedback** | A Tutor's evaluation of a Session Item (score, and later descriptive feedback and recommendations) |

4. **Disambiguation rules**

   - Speak of the **notebook** or **folder**, its **readme**, **settings**, and
     **health** as needed — do not call the in-app notebook page a “workspace”.
   - Always qualify **layout** as **refinement** or **book**.
   - Use **wiki link**, **relationship**, or **Wikidata association** — never
     bare **link** or **wiki** when the kind matters.
   - Use **subscription** / **subscribe** for following a shared notebook.
     Do not use bare **learning**. Use **Learning Session** (and its family)
     for commissioned tutoring; use **assimilation**, **recall**, or
     **subscription** for Doughnut’s own activities.
   - **Skip from the assimilation sequence** is not the assimilate action; it
     does not block assimilating that unit from the note. Do not use **skip
     assimilation**, **Unskip**, or **skip recall** for this mark.
   - **Return to sequence** puts a skipped unit back in the walkthrough;
     **Revive** re-enables recall for a tracker that was **removed from
     recall**. They are different actions.
   - **Skip Memory Tracking** is the notebook-only setting: it opts the
     notebook out of the **assimilation sequence** and blocks Bazaar
     subscribe. It does not opt out of **recall**. Do not skip memory
     tracking on a note. Tracker-level opt-out is **Remove from recall**.
   - Wire/AI shapes of an MCQ are not glossary nouns.
   - Use **Stability** for a memory tracker’s interval, not a user interval
     list or **space setting**. Do not persist **Retrievability**. **Requested
     retention** is a global constant 0.9, not a learner setting.
   - Prefer **semantic search** over **semantical search**.
   - Use **notebook description** for the one-line plain-text catalog blurb;
     use **readme** for the markdown notebook/folder body — never treat them as the
     same concept.
   - Use **Tutor** for whoever conducts a Learning Session — not *teacher*,
     *coach*, or *instructor*.
   - Use **Learning Session** for the commissioned unit; reserve **recall** for
     the ordinary spaced activity Doughnut conducts itself.
   - Say a Learning Session is **commissioned** (by the Learning Orchestrator)
     and later **recorded** (from a Learning Session Report).

5. **Alignment policy** — On Accept:

   - **New** features, tests, OpenAPI names, and packages follow this glossary.
   - **Existing** names may remain until a deliberate rename slice; do not
     mass-rename in drive-by PRs.
   - End state for this slice: the same nouns in UI, API, and schema, with
     **minimum DTO** (a recall prompt HAS_A an MCQ when it is MCQ; do not
     introduce a translation type that wraps one as the other). Reach that
     in phases.
   - Agents treat this ADR as binding for naming choices; conflicts with older
     strings are expected until cleaned up.

6. **Out of scope** — Exact UI microcopy, i18n, and the mechanical rename
   slices. Those become normal product work citing this ADR.

## Consequences

- Product language converges on fewer overloaded words (**link**). Bare
  **learning** is not a glossary noun; commissioned tutoring uses the
  **Learning Session** family. **Refinement layout** and **book layout** stay
  distinct; never use bare **layout** for either.
- Product language names spaced retrieval **recall**, not **review**, so the
  glossary matches the philosophy (recall is better than review) and stays
  distinct from FSRS/Anki vocabulary. **Just review** names a recall method,
  not a different activity.
- Humans and agents share an explicit dictionary instead of inferring synonyms.
- The memory tracker’s interval is **Stability**, not a learner-edited day
  list; **Retrievability** stays a computed value; **requested retention**
  is a global constant 0.9, not a Settings knob.
- Circles, notebook groups, and folders stay clearly separated in speech.
- Spelling is spoken as a learner-created **spelling memory tracker**, not as a
  note option or a mode of assimilation.
- Sequence skip, **Remove from recall**, **Revive**, and notebook **Skip
  Memory Tracking** stay distinct in speech; **Return to sequence** is not
  **Revive**.
- Commissioned Learning Session names are fixed before the capability is built, so its
  entities, API, UI copy, and tests start on the glossary instead of renaming
  later.
- Retired product concepts (**workspace** as CLI sync tree, **push / pull**
  sync, **sync baseline** / `.doughnut-sync`) must not reappear in product
  code or glossary; portable content follows ADR 0002.

## Pros

- Low process cost: one ADR, incremental alignment.
- Reduces accidental synonymy in plans and API design.

## Cons

- Glossary will need edits as the product grows.
- Temporary dual vocabulary until renames land.
- **Stability** (hours) is more technical than informal **space setting**.

## Prerequisites / Assumptions

- No Accepted ADR yet constrains domain naming (only ADR-0000 on using ADRs).

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links: playbook [README.md](./README.md); ADR-0000
  [use-adrs-accepted.md](./0000-use-adrs-accepted.md); ADR 0003
  [spaced-repetition scheduling policy](./0003-spaced-repetition-scheduling-policy.md)
  (FSRS **review** = Doughnut **recall**; **Stability** / **Retrievability**);
  [Open Knowledge Format](https://cloud.google.com/blog/products/data-analytics/how-the-open-knowledge-format-can-improve-data-sharing)
