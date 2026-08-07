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
| **Add to learning** | Synonym used for starting a subscription |
| **Re-assimilate** | Return a note to assimilation after failed recall |
| **Skip recall** | Opt a note or notebook out of recall (panel action or notebook setting) |
| **Revive recall** | Re-enable recall after it was skipped |
| **Remembering spelling** | Assimilation option that requires verifying the note title by spelling |
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
| **Learning** | Overall learner metaphor; alias for subscription (“add to learning”); “target of learning N notes per day” as assimilation quota |
| **Layout** | Refinement layout for a note vs book layout for an attached book |
| **Quiz / question** | Spelling, AI-generated, predefined, and contested prompts without a shared parent name |
| **Property** | Reduced relationship field; wiki property from accidental match; property memory tracker key |
| **Chat / conversation / message** | Human note threads and AI chat share the same nouns without a clear split |
| **My notes / my notebooks** | Notebook catalog; note search scope; subscribed notebook appearing among the user’s notebooks |
| **Description / Readme** | Catalog blurb (`notebook description`) vs notebook/folder markdown body (`readme`) — both “about the notebook” but different fields |

#### Redundant (several names, one concept)

| Concept | Overlapping names |
|---------|-------------------|
| Turning off recall for a note or notebook | skip recall, Skip Memory Tracking, no “add to learning” when memory tracking is off |
| Following a shared notebook | subscribe, subscription, add to learning |
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
   code identifiers:

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
| **Subscription** | Following a shared notebook (from the Bazaar or a Circle) with a daily assimilation quota |
| **Assimilation** | First-pass intake of a note into the learner’s memory schedule |
| **Recall** | Spaced review of assimilated material |
| **Recall question** | A single recall prompt (kinds: spelling, AI-generated, predefined, …) |
| **Memory tracking** | Whether a note or notebook participates in assimilation and recall |
| **Wiki link** | In-content `[[…]]` reference to a note (optionally `Notebook:Title`) |
| **Relationship** | Typed association between notes (e.g. “similar to”, “a part of”) |
| **Relationship note** | A note that represents a relationship (`type: relationship` in frontmatter), with source, target, and relation |
| **Wikidata association** | Binding a note to a Wikidata entity (also called Wiki association) |
| **Property memory tracker** | Recall tracking keyed by a property or relationship label |
| **Refinement layout** | Layout for decomposing and improving a note while refining |
| **Book layout** | Structure of an attached book |
| **Conversation** | Thread of messages about a note (human or AI participant) |
| **Message** | One utterance in a conversation |
| **Message center** | UI for conversations and unread state |
| **OKF** | [Open Knowledge Format](https://cloud.google.com/blog/products/data-analytics/how-the-open-knowledge-format-can-improve-data-sharing): portable directory of markdown concept files with YAML frontmatter |
| **Spaced-repetition schedule** | User interval list for recall |
| **Daily assimilation target** | Max new notes to assimilate per day (profile or subscription) |
| **Notebook health** | In-app lint, findings, and fixes for a notebook |
| **Focus context** | Bounded note neighborhood for AI use |
| **Book** | Attached reading artifact (EPUB, PDF, …), distinct from a notebook |

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
   - Prefer **memory tracking** for the setting; **skip recall** for the action.
   - Prefer **recall question** over bare **quiz** when naming the prompt type.
   - Prefer **spaced-repetition schedule** over **space setting** in new copy.
   - Prefer **semantic search** over **semantical search**.
   - Use **notebook description** for the one-line plain-text catalog blurb;
     use **readme** for the markdown notebook/folder body — never treat them as the
     same concept.
   - Use **Tutor** for whoever conducts a Learning Session — not *teacher*,
     *coach*, or *instructor*.
   - Use **Learning Session** for the commissioned unit; reserve **recall** for
     the ordinary spaced review Doughnut conducts itself.
   - Say a Learning Session is **commissioned** (by the Learning Orchestrator)
     and later **recorded** (from a Learning Session Report).

5. **Alignment policy** — On Accept:

   - **New** features, tests, OpenAPI names, and packages follow this glossary.
   - **Existing** names may remain until a deliberate rename slice; do not
     mass-rename in drive-by PRs.
   - Agents treat this ADR as binding for naming choices; conflicts with older
     strings are expected until cleaned up.

6. **Out of scope** — Exact UI microcopy, i18n, and full mechanical rename
   plans. Those become normal product work citing this ADR.

## Consequences

- Product language converges on fewer overloaded words (**link**,
  **learning**, **layout**).
- Humans and agents share an explicit dictionary instead of inferring synonyms.
- Some existing strings (`add to learning`, `space setting`) become known debt
  until renamed.
- Circles, notebook groups, and folders stay clearly separated in speech.
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
- Some metaphors (“add to learning”, “space setting”) may feel colder when
  replaced with sharper terms.

## Prerequisites / Assumptions

- No Accepted ADR yet constrains domain naming (only ADR-0000 on using ADRs).

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links: playbook [README.md](./README.md); ADR-0000
  [use-adrs-accepted.md](./0000-use-adrs-accepted.md); [Open Knowledge Format](https://cloud.google.com/blog/products/data-analytics/how-the-open-knowledge-format-can-improve-data-sharing)
