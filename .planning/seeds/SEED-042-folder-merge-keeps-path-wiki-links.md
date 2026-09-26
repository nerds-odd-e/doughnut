---
id: SEED-042
status: dormant
planted: 2026-09-26
planted_during: SEED-035 story 11 refinement (dissolve and merge folders that contain files)
trigger_when: selecting web folder organization fixes
scope: S
---

# SEED-042: Path wiki links survive a folder merge

## Why This Matters

A notebook owner can write a wiki link by path, such as `[[Holder/Dup/B]]`,
which finds a note only through its exact folder chain. Web folder renames,
moves, dissolves and cross-notebook moves rewrite such links when notes change
folders. A same-notebook folder move that merges into a same-named folder does
not: its merge branch moves the notes and returns without rewriting inbound
links, so those links become dead. The notebook health report lists them
under "Dead wiki links", but the owner must repair each by hand. Found by code
reading during SEED-035 story 11 refinement; no test covers links on this
path.

Why now: SEED-035 story 11 lets folders holding files (including every
web-uploaded picture) merge, which today is refused, so merges and this bug
become common right after it. The fix is tiny and touches the same code.

## Alternatives and Decision

- **Leave it:** owners find dead links in the health report and fix them by
  hand.
- **Chosen:** make the merge move stop being a special case: capture inbound
  links before choosing between a plain move and a merge, and rewrite them
  after either, exactly as dissolve does. No new link rules.

## Story Decomposition

<a id="story-1"></a>

### Keep path wiki links working when a folder merges into a same-named folder
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless","assessment":"ready","reasons":[],"basis":{"document":"5a65faa0b1c77a1259b5f3fbf347a6f4dd2199728117c8db9fdcc89d3ada0ba0"}}
```

- **Identity:** SEED-042#story-1
- **Goal:** An owner who moves a folder onto a same-named folder in the same
  notebook and accepts the merge keeps every path wiki link to the moved notes
  working, as a plain move and a dissolve already do.
- **Scope:** Inbound path wiki links to notes moved by a same-notebook merge
  move are rewritten to the notes' new folder chains, using the folder name
  that survives the merge, through the existing accepted-change boundary, like
  the plain move. The move shares one capture-and-rewrite path for its plain
  and merge outcomes instead of adding a second copy.
- **Excluded:**
  - Title links such as `[[B]]`: a same-notebook merge changes no note titles
    or notebook, so they are unaffected.
  - Cross-notebook merges and dissolve with merge, which already rewrite.
  - Links changed by folder moves in a local checkout; local-publish
    acceptance does not rewrite links, which is a separate concern.
  - Repairing links that earlier merges already broke; the health report shows
    them for manual repair.
  - `image:` file references (excluded by SEED-035 story 11).
- **Key examples:**
  - Root has `Dup/`; `Holder/Dup/` holds note `B`; note `A` contains
    `[[Holder/Dup/B]]`. Moving `Holder/Dup` to the root with merge → `A`
    contains `[[Dup/B]]`, and the link opens `B`.
  - `[[Holder/Dup/Inner/C]]` to a note in a nested same-named subfolder becomes
    `[[Dup/Inner/C]]`.
  - `[[B]]` is unchanged.
- **Proof home:** `NotebookFolderMoveWikiLinkRewriteControllerTest`.
- **Effort hypothesis:** XS, high confidence; planless execution (owner
  decision 2026-09-26).
- **Depends on:** none. After SEED-035 story 11, a case-variant merge
  (`Holder/Diagrams` onto `diagrams`) rewrites to the surviving name
  (`[[diagrams/B]]`); the rewrite already reads the note's actual folder.
- **Safe stopping point:** each delivery stands alone.

## Ordering and Scope Reduction

Queued right after SEED-035 story 11 (owner decision 2026-09-26); the owner
then chose to execute it immediately, planless (2026-09-26).

## When to Surface

Now; it is queued.

## Breadcrumbs

- [SEED-035 story 11](SEED-035-ai-workspace-supporting-files.md#story-11)
  refinement, 2026-09-26: the owner asked to keep this bug out of that story
  and queue it right after.
- Owner decisions, 2026-09-26 (refinement): fix by removing the merge special
  case rather than duplicating the rewrite; skip slice planning; execute now.
