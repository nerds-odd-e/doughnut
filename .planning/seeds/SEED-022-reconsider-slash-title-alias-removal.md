---
id: SEED-022
status: dormant
planted: 2026-09-17
planted_during: Refining SEED-021 (inbound wiki-link NFKC-normalization bug)
trigger_when: when the owner is ready to decide whether to supersede ADR 0004's Portable-path shorthand matching
scope: L
---

# SEED-022: Reconsider slash-based title/alias (Portable path) matching

## Why This Matters

While diagnosing SEED-021 (a note's References panel omitting an incoming
reference whose shared title contains a fullwidth slash), the owner stated:

> "The aliases using slash in title is a legacy behavior and is supposed to be
> completely removed, as per my understanding. Looks like it's not the case in
> the code."

Investigation (reading `DisplayNamePathSeparators.java`, `FrontmatterAliases.java`,
Accepted ADR 0004, and `git log` on the relevant files) found the opposite of
that understanding:

- Accepted [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  lines 101–107, explicitly specifies unqualified `[[Title-or-Alias]]` wiki
  links as shorthand **Portable path** resolution — a note title containing a
  path separator is current, deliberate, documented design, not a leftover.
- A raw ASCII `/` cannot be authored directly into a note title at all:
  `DisplayNamePathSeparators.replaceOsInvalidChars` substitutes the fullwidth
  `／` for it on write, specifically so titles stay filesystem-safe for the
  Git-native notebook binding (ADR 0002). That substitution is why the SEED-021
  title literally contains `／` — it is filesystem-safety behavior, not a
  removable "alias" feature.
- `FrontmatterAliases`'s explicit `aliases:` frontmatter list has always
  outright forbidden slash characters (`INVALID_ALIAS_CHARACTERS`); there is
  no slash-based alias behavior in that mechanism to remove either.
- Recent commit history (e.g. `9aac14d029` "author /Title for notebook-root
  collisions", `c8338072f0` "write the full folder path when repairing
  ambiguity", `f72b5d64ac` "introduce new plan for portable path ambiguity
  follow-up") shows this system under active, ongoing extension, not wind-down.
  No commit message or planning artifact anywhere proposes removing it.

So SEED-021 was kept narrowly scoped to the NFKC-normalization bug only, with
no removal work. This seed exists to hold the owner's original removal intent
separately, in case the owner still wants to pursue it as a deliberate,
informed decision to supersede ADR 0004 — not to re-litigate the finding above
without new grounds.

## Alternatives and Decision

Unresolved. Two directions, neither selected yet:

1. **Keep current design.** Portable-path shorthand resolution (slash-shaped
   `[[Folder/Title]]` wiki links, and the filesystem-safe fullwidth
   substitution it depends on) stays as Accepted ADR 0004 describes it. No
   further action beyond SEED-021's normalization fix.
2. **Supersede ADR 0004's Portable-path shorthand.** Remove slash-shaped
   wiki-link/path matching and the title-to-filename slash substitution
   outright, product-code-and-tests-and-docs, as if the behavior never
   existed — no negated tests, no "used to support X" documentation. This
   would need: an explicit ADR amendment/supersession (not a silent code
   change against an Accepted ADR), a decision on what a note whose title the
   user wants to type with a `/` should do instead, and an assessment of
   impact on the Git-native notebook sync feature (ADR 0002) that Portable
   paths serve. Materially larger than SEED-021; not attempted without the
   owner first confirming this direction with fresh justification.

## Story Decomposition

No story is decomposed yet. A beneficiary and evaluable outcome for direction
2 above are not yet established — the owner's original justification ("legacy
behavior") did not hold up against ADR 0004 and active development history.
Decomposing a removal story requires either a new justification for
superseding ADR 0004, or an explicit owner decision to proceed anyway knowing
the current design is intentional. Route to
[dough-story-decomposition](../../.claude/skills/dough-story-decomposition/SKILL.md)
once that decision is made, since the parent problem (why remove it, and what
replaces the shorthand) is still unresolved.

## Ordering and Scope Reduction

Placed last in the product backlog: it is speculative, disputed against an
Accepted ADR, and has no confirmed beneficiary today. It should not block or
compete with any currently justified story.

## Open Decisions

- Does the owner still want to pursue removing slash-based title/Portable-path
  matching, now knowing it is Accepted-ADR-0004 design under active
  development — and if so, on what new grounds, and what should replace the
  `[[Title-or-Alias]]` shorthand for notebook owners who rely on it today?
  Blocks any decomposition or planning of this seed.

## When to Surface

When the owner explicitly wants to revisit this decision — not automatically
triggered by any other story.

## Breadcrumbs

- SEED-021 — inbound wiki-link NFKC-normalization bug (the investigation that
  produced this finding); completed and spent, see
  `.planning/seeds/SEED-021-inbound-wiki-reference-nfkc-mismatch.md` at commit
  `52c5b93e55`
- [ADR 0002 — Git-native portable notebook synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization.md)
- [ADR 0004 — OKF-compatible notebook markdown](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
- `backend/src/main/java/com/odde/donut/validators/DisplayNamePathSeparators.java`
- `backend/src/main/java/com/odde/donut/algorithms/FrontmatterAliases.java`
