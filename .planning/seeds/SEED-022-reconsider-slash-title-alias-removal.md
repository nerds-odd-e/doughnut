---
id: SEED-022
status: dormant
planted: 2026-09-17
planted_during: Refining SEED-021 (inbound wiki-link NFKC-normalization bug)
trigger_when: when the owner is ready to remove the remaining recall semantics embedded in note titles
scope: M
---

# SEED-022: Make note titles literal in recall

## Why This Matters

The earlier version of this seed incorrectly treated slash-shaped wiki-link
paths from Accepted ADR 0004 as if they required recall aliases embedded in a
note title. They are separate concepts. ADR 0004 makes the stored title the
display name and uses `/` inside authored wiki links as Portable-path syntax;
it does not require a title containing the fullwidth character `／` to be split
into recall alternatives.

The remaining recall implementation still gives two positions inside a title
special meaning: a fullwidth-slash segment beginning with a tilde becomes an
additional answer/masking fragment, and a trailing bracket pair becomes a
separately masked qualifier. This makes visually ordinary title characters
change learning behavior. The owner has confirmed that these semantics should
be removed: a title is one value, with only a tilde at the actual beginning of
the whole title retaining its existing suffix behavior. Alternative spellings
belong in YAML `aliases:` frontmatter.

## Alternatives and Decision

Selected: interpret the whole note title as one recall value. A leading
`~`, `〜`, or `～` on that whole value keeps the established suffix-matching
behavior. Fullwidth slashes, later tildes, and brackets anywhere in the title
are literal characters. Keep explicit frontmatter aliases as the separate
source of alternative answers and masks.

The compatibility alternative—keep parsing slash-delimited suffix fragments
and trailing qualifiers—was rejected because those implicit alternatives are
the behavior the owner intends to retire. Existing stored titles are not
rewritten, and no migration invents frontmatter aliases for them.

## Story Decomposition

### Story 1: Treat the whole note title as one recall value

- **Goal:** A learner can predict spelling verification and recall masking from
  the displayed title: the entire title is literal, except for an actual
  leading tilde marker on the whole title.
- **Scope:** Apply the rule to spelling-answer matching and recall-question
  cloze masking. Remove fullwidth-slash suffix-fragment parsing and trailing
  bracket-qualifier parsing. Preserve whole-title leading-tilde behavior and
  explicit YAML `aliases:` behavior. Leave stored titles unchanged.
- **Key examples:** `word／~logical` accepts and masks only the whole literal
  title, not `word` or `logical` separately; `cat(animal)` accepts and masks
  only the whole literal title, not `cat` or `animal` separately; `~logical`
  still accepts `logical` and masks suffix occurrences; a frontmatter alias
  still works as an alternative answer and mask.
- **Rejection constraints:** Do not change ASCII `/` Portable-path parsing in
  wiki links, fullwidth-slash filename safety, title storage, or frontmatter
  alias rules. Do not fold the separately fixed inbound-reference bug or other
  whole-token NFKC/deduplication concerns into this story.
- **Value / learning:** Removes hidden title grammar while preserving the one
  explicit title marker and the explicit alias mechanism the learner already
  relies on.
- **Effort hypothesis:** M — high confidence in the owning backend boundary;
  moderate test-update breadth because the removed grammar is represented in
  both answer-matching and cloze-masking cases.
- **Depends on:** none.
- **Safe stopping point:** Spelling and masking agree on the one-value title
  rule, initial-tilde and frontmatter-alias behavior remain covered, and no
  stored title or Portable-path behavior changes.

## Ordering and Scope Reduction

This seed has one story because answer matching and cloze masking are two
observable uses of the same title rule; delivering only one would leave recall
internally inconsistent. The NFKC/deduplication concern is independent and is
not needed to achieve this outcome.

## Open Decisions

None.

## When to Surface

Now: the owner confirmed the literal-title rule and requested an executable
slice plan.

## Breadcrumbs

- [ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md)
  (`Remember spelling` verifies the note title or alias)
- [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  (the title column is the display-name source of truth; Portable paths are a
  wiki-link concern)
- SEED-021 — inbound wiki-reference NFKC mismatch, completed separately
