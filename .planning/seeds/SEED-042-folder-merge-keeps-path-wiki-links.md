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
which finds a note only through its exact folder chain. Web folder moves,
dissolves and cross-notebook moves rewrite such links when notes change
folders. A same-notebook folder move that merges into a same-named folder does
not: its merge branch moves the notes and returns without rewriting inbound
links, so those links silently become dead. Found by code reading during
SEED-035 story 11 refinement; no test covers links on this path.

## Alternatives and Decision

- **Leave it:** owners find dead links later and fix them by hand.
- **Recommended:** give the merge move the same inbound-link rewrite the other
  folder relocations already use. No new link rules.

## Story Decomposition

<a id="story-1"></a>

### Keep path wiki links working when a folder merges into a same-named folder
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

- **Identity:** SEED-042#story-1
- **Goal:** An owner who moves a folder onto a same-named folder in the same
  notebook and accepts the merge keeps every path wiki link to the moved notes
  working, as a plain move and a dissolve already do.
- **Scope:** Inbound path wiki links to notes moved by a same-notebook merge
  move are rewritten to the notes' new folder chains, through the existing
  accepted-change boundary, like the plain move.
- **Excluded:** title links, which never depend on folders, including any
  ambiguity when a merge brings two same-titled notes together; cross-notebook
  merges and dissolve with merge, which already rewrite.
- **Key examples:**
  - Root has `Dup/`; `Holder/Dup/` holds note `B`; note `A` contains
    `[[Holder/Dup/B]]`. Moving `Holder/Dup` to the root with merge → `A`
    contains `[[Dup/B]]`, and the link opens `B`.
  - `[[Holder/Dup/Inner/C]]` to a note in a nested same-named subfolder becomes
    `[[Dup/Inner/C]]`.
  - `[[B]]` is unchanged.
- **Effort hypothesis:** S, high confidence.
- **Depends on:** none.
- **Safe stopping point:** each delivery stands alone.

## Ordering and Scope Reduction

Queued right after SEED-035 story 11 (owner decision 2026-09-26).

## When to Surface

Now; it is queued.

## Breadcrumbs

- [SEED-035 story 11](SEED-035-ai-workspace-supporting-files.md#story-11)
  refinement, 2026-09-26: the owner asked to keep this bug out of that story
  and queue it right after.
