---
id: SEED-021
status: dormant
planted: 2026-09-17
planted_during: Investigating why note n102466 showed no incoming reference despite note n102472's outgoing wiki-link resolving to it
trigger_when: when correcting inbound wiki-link/alias reference matching for titles containing characters affected by NFKC compatibility normalization
scope: S
---

# SEED-021: Fix inbound wiki-link matching for NFKC-foldable title characters

## Why This Matters

A note's "References" panel is supposed to show, symmetrically, every other note
that links to it. Live reproduction against the dev database showed this is
false for at least one class of title: note n102472 ("どのように") authors
`parent: "[[how 手段／方法]]"`, and its own outgoing wiki link correctly resolves
to note n102466 ("how 手段／方法"). But n102466's References panel is empty — the
same reference is invisible from the target's side.

Root cause: outgoing resolution (`WikiLinkNoteCandidates`) matches the authored
token against note titles with a raw, non-normalized string compare. Inbound
resolution (`AuthoredNoteReferenceInboundFacade`, via
`FrontmatterAliases.normalizedLookupKey`) NFKC-normalizes its lookup key before
comparing it against the raw, non-normalized `wiki_note_portion` column. NFKC
compatibility normalization rewrites the fullwidth solidus `／` (U+FF0F) to the
ASCII `/` (U+002F), so the two sides compare different strings and the SQL
match silently fails. This will reproduce for any title/alias containing other
NFKC-foldable characters (fullwidth digits, fullwidth Latin letters, ligatures,
etc.), which are common in Japanese-vocabulary notebooks like this one.

## Alternatives and Decision

It was initially suspected that slash-in-title matching itself is legacy,
superseded behavior that should be removed outright rather than fixed. Reading
`DisplayNamePathSeparators.java` and accepted ADR 0004
(OKF-compatible notebook markdown) shows the opposite: Donut deliberately maps
an authored ASCII `/` in a note title to the fullwidth `／` because titles
project to Git-native file/folder names (ADR 0002), and path-shaped wiki links
(`[[Folder/Title]]`, ADR 0004's **Portable path**) are current, accepted,
load-bearing architecture for the git-backed notebook workflow — not a legacy
mechanism. This needs explicit confirmation during refinement (see Open
Decisions) before the fix direction is finalized, but the working assumption
going into refinement is a normalization-consistency fix (making inbound
matching compare the same normal form as outgoing matching), not a removal of
path-shaped/portable-path matching.

## Story Decomposition

<a id="story-1"></a>

### 1. Make inbound wiki-link/alias matching consistent with outgoing resolution

- **Goal:** A notebook owner who authors a wiki link/alias reference to
  another note can trust the target note's References panel shows that
  incoming reference back, whenever outgoing resolution from the source note
  already resolves it — regardless of which Unicode form the shared
  title/alias text takes.
- **Scope:**
  - Required: any authored wiki-link or alias reference that outgoing
    resolution (`WikiLinkResolver`/`WikiLinkNoteCandidates`) successfully
    resolves to a target note must also be discoverable as an incoming
    reference on that target note's page. Concretely, the inbound candidate
    lookup in `AuthoredNoteReferenceInboundFacade` must not be *narrower* than
    what the real resolver (already reused for the final verdict via
    `resolvesToTarget`) would accept — it may still be broader, since the
    real resolver re-confirms every candidate before it's shown.
  - Rejection: no change to `PathShapedTarget`/Portable-path suffix matching
    semantics (ADR 0004) — those remain exactly as they resolve today. No
    change to whether explicit `aliases:` frontmatter entries themselves may
    contain a slash (`FrontmatterAliases` already forbids that; unrelated to
    this bug).
  - Deferred: whether alias matching's existing NFKC-folding behavior is the
    right end-user matching policy for aliases is not reopened here; this
    story only removes the asymmetry between what outgoing resolution accepts
    and what the inbound candidate query can find, not any broader matching
    policy question.
  - Exact fix location and shape (e.g. whether the title-lookup key drops NFKC
    normalization to mirror the raw-match outgoing path, versus normalizing
    the stored column too) is an implementation decision for slice planning,
    not fixed here.
- **Key examples:**
  1. Note A authors `parent: "[[how 手段／方法]]"` (fullwidth solidus) matching
     note B's exact title "how 手段／方法". A's outgoing reference already
     resolves to B (unchanged). B's References panel must now list A.
  2. General case: any title/alias pair that differs only by a character NFKC
     compatibility normalization would fold (fullwidth digits/letters,
     ligatures, other fullwidth punctuation) behaves the same way — if
     outgoing resolution finds the target, the target's References panel
     shows the referrer.
  3. Unaffected boundary: a path-shaped wiki link (`[[Folder/Title]]`)
     continues to resolve via Portable-path suffix matching exactly as before
     — this story changes normalization consistency only, not path-shaped
     resolution semantics.
- **Effort hypothesis:** S (30–90 minutes), moderate confidence — the fix is
  local to the inbound candidate lookup/comparison; scope is confirmed bounded
  (see Open Decisions), so no removal-sized work is expected.
- **Depends on:** No unfinished product prerequisite.
- **Safe stopping point:** Inbound candidate matching accepts everything
  outgoing resolution accepts for titles/aliases containing NFKC-foldable
  characters; existing path-shaped/Portable-path behavior (ADR 0004) is
  otherwise unchanged; backend suite proves the previously-missing inbound
  match (key example 1) without regressing path-shaped resolution (key
  example 3).

## Ordering and Scope Reduction

First in the product backlog by owner direction — this is a correctness bug in
a symmetric, user-visible feature (References), not a new capability.

## Open Decisions

- Resolved during investigation: the slash-related matching machinery
  (title→filename fullwidth substitution, `PathShapedTarget`/Portable-path
  suffix matching) is **not** legacy. Accepted ADR 0004 explicitly specifies
  unqualified `[[Title-or-Alias]]` as shorthand Portable-path resolution
  (lines 101–107), and recent commit history actively extends this system
  (e.g. `f72b5d64ac` "introduce new plan for portable path ambiguity
  follow-up", `21861d6a0c` "skip unreadable namesakes when authoring a
  Portable path", `9aac14d029` "author /Title for notebook-root collisions",
  `c8338072f0` "write the full folder path when repairing ambiguity") — none
  deprecating it, and no commit or planning artifact anywhere proposes its
  removal. `FrontmatterAliases`'s explicit `aliases:` list has separately
  always forbidden slash characters outright (`INVALID_ALIAS_CHARACTERS`); it
  never had a slash-based alias behavior to remove either. Scope stays
  bounded to the NFKC normalization-consistency fix; no removal substory is
  added.

## When to Surface

Immediately, from the product backlog.

## Breadcrumbs

- [ADR 0002 — Git-native portable notebook synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
- [ADR 0004 — OKF-compatible notebook markdown](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
- `backend/src/main/java/com/odde/donut/algorithms/FrontmatterAliases.java`
- `backend/src/main/java/com/odde/donut/entities/repositories/AuthoredNoteReferenceInboundFacade.java`
- `backend/src/main/java/com/odde/donut/validators/DisplayNamePathSeparators.java`
