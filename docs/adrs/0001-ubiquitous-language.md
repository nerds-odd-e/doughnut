# 0001 — Ubiquitous language for Doughnut domain concepts

**Status:** Proposed  
**Date:** 2026-07-31  
**Decision makers:** Terry
**Consulted:** (people / teams asked for advice)

## Context

Doughnut’s product vocabulary should not be **inconsistent**: the same idea appears under
several names, and some names mean more than one thing. Humans, UI copy, APIs,
and coding agents then invent synonyms or collide terms.

This ADR proposes a **canonical ubiquitous language**, plus renames where
today’s wording is missing, ambiguous, or redundant. Accepting it does **not**
require renaming the whole codebase at once; it constrains *new* naming and
guides gradual alignment (tests, UI, OpenAPI, packages).

### Current vocabulary

| Term | Meaning |
|------|---------|
| **Quiz / question** | A recall prompt (spelling, AI-generated, predefined, contested, …) |
| **Question contest** | Challenging or replacing an AI-generated recall question |
| **Accidental match** | Recall result that matches an unintended note |
| **Property** | Attribute on a note; may result from reducing a relationship note to the source |
| **Reading record** | Progress through a book |
| **Space setting** | User’s spaced-repetition interval list (e.g. `1, 2, 4, 8`) |
| **Access token** | Credential for API / CLI access to Doughnut |

### Problems

#### Ambiguous (one word, several meanings)

| Term | Colliding senses |
|------|------------------|
| **Learning** | Overall learner metaphor; commissioned **Learning Session** family — not a synonym for subscription |
| **Layout** | Refinement layout for a note vs book layout for an attached book |
| **Quiz / question** | Spelling, AI-generated, predefined, and contested prompts without a shared parent name |
| **Property** | Reduced relationship field; wiki property from accidental match; property memory tracker key |
| **Chat / conversation / message** | Human note threads and AI chat share the same nouns without a clear split |
| **My notes / my notebooks** | Notebook catalog; note search scope; subscribed notebook appearing among the user’s notebooks |
| **Description / Readme** | Catalog blurb (`notebook description`) vs notebook/folder markdown body (`readme`) — both “about the notebook” but different fields |

#### Redundant (several names, one concept)

| Concept | Overlapping names |
|---------|-------------------|
| Following a shared notebook | subscribe, subscription |
| Binding to Wikidata | Wikidata association, Wiki association, associate Wikidata ID |
| User recall interval list | space setting, spaced repetition |
| Meaning-based find | semantical search, semantic search |

#### Missing or weakly named

| Gap | Why it matters |
|-----|----------------|
| No umbrella for **recall question** kinds | Spelling / AI / predefined / contested lack a shared type name |
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
| **OKF** | [Open Knowledge Format](https://cloud.google.com/blog/products/data-analytics/how-the-open-knowledge-format-can-improve-data-sharing): portable directory of markdown concept files with YAML frontmatter |
| **Notebook health** | In-app lint, findings, and fixes for a notebook |
| **Skip Memory Tracking** | Notebook setting that opts the notebook out of the assimilation sequence and blocks Bazaar subscribe. It does not opt the notebook out of recall. |

#### Learning and recall

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
| **Recall** | Spaced retrieval of assimilated material: the learner must produce the knowledge. Doughnut names this **recall**, not **review**. | |
| **Recall question** | A single recall prompt (kinds: spelling, AI-generated, predefined, …) | |
| **Memory tracking** | Creating and maintaining memory trackers for notes. Tracker-level opt-out is **Remove from recall**. | |
| **Remove from recall** | Stop an existing memory tracker from appearing in recall; the unit does not re-enter the sequence | **Remove** / **Remove from recall** |
| **Revive** | Re-enable recall for a tracker that was removed from recall | **Revive** |
| **Property memory tracker** | Recall tracking keyed by a property or relationship label | |
| **Spaced-repetition schedule** | User interval list for recall | |
| **Daily assimilation target** | Max new understanding memory trackers to create per day (profile or subscription). Spelling and commissioned trackers do not consume this count. | |

#### Conversation and focus

| Term | Meaning |
|------|---------|
| **Conversation** | Thread of messages about a note (human or AI participant) |
| **Message** | One utterance in a conversation |
| **Message center** | UI for conversations and unread state |
| **Focus context** | Bounded note neighborhood for AI use |

3. **Commissioned learning terms** — Vocabulary for learning that a Tutor
   conducts outside Doughnut, on commission from Doughnut:

| Term | Meaning |
|------|---------|
| **Learning Orchestrator** | The Doughnut component that directs and coordinates learning sessions |
| **Commissioned memory tracker** | Memory tracker maintained through commissioned learning sessions rather than ordinary recall |
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
   - Use **subscription** / **subscribe** for following a shared notebook;
     reserve **learning** for the overall learner metaphor, not as a synonym of
     subscription.
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
   - Prefer **recall question** over bare **quiz** when naming the prompt type.
   - Prefer **spaced-repetition schedule** over **space setting** in new copy.
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
   - This glossary slice (**assimilation sequence**, sequence skip, **Return
     to sequence**, **Remove from recall**, **Revive**, **Skip Memory
     Tracking**) is the source for new naming in that work. **Existing** names
     may remain until a deliberate rename slice; do not mass-rename in
     drive-by PRs.
   - Agents treat this ADR as binding for naming choices; conflicts with older
     strings are expected until cleaned up.

6. **Out of scope** — Exact UI microcopy, i18n, and full mechanical rename
   plans. Those become normal product work citing this ADR.

## Consequences

- Product language converges on fewer overloaded words (**link**,
  **learning**, **layout**).
- Product language names spaced retrieval **recall**, not **review**, so the
  glossary matches the philosophy (recall is better than review) and stays
  distinct from FSRS/Anki vocabulary.
- Humans and agents share an explicit dictionary instead of inferring synonyms.
- Some existing strings (`space setting`) become known debt until renamed.
- Circles, notebook groups, and folders stay clearly separated in speech.
- Spelling is spoken as a learner-created **spelling memory tracker**, not as a
  note option or a mode of assimilation.
- Sequence skip, **Remove from recall**, **Revive**, and notebook **Skip
  Memory Tracking** stay distinct in speech; **Return to sequence** is not
  **Revive**.
- Commissioned learning names are fixed before the capability is built, so its
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
- Some metaphors (“space setting”) may feel colder when replaced with sharper
  terms.

## Prerequisites / Assumptions

- No Accepted ADR yet constrains domain naming (only ADR-0000 on using ADRs).

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links: playbook [README.md](./README.md); ADR-0000
  [use-adrs-accepted.md](./0000-use-adrs-accepted.md); ADR 0003
  [spaced-repetition scheduling policy](./0003-spaced-repetition-scheduling-policy.md)
  (FSRS **review** = Doughnut **recall**); [Open Knowledge Format](https://cloud.google.com/blog/products/data-analytics/how-the-open-knowledge-format-can-improve-data-sharing)
