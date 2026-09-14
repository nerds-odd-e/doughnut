# Git history as evidence for notebook identity

Research date: 2026-09-14. Input to Proposed [ADR 0002](adrs/0002-git-native-portable-notebook-synchronization.md).
This is research and architectural advice, not an implementation or an approved
identity policy.

For the application survey, deletion/recreation experiments, and a concrete
policy recommendation, see
[Git range comparisons and notebook identity](notebook-git-range-comparison-research.md).

## Finding

Inspecting adjacent commits can recover continuity that an accepted-to-tip diff
loses. It can also expose interruptions that an endpoint diff conceals. Neither
method can recover intentions that were never recorded. Donut can inspect
history without applying intermediate notebook states to MySQL.

Git commits reference trees and parents; trees record paths and objects, not
stable Donut identities or authored rename operations. This is the underlying
information limit. [Git objects](https://git-scm.com/book/en/v2/Git-Internals-Git-Objects)

Git derives rename/copy candidates from compared trees. `-M` enables rename
detection and accepts a similarity threshold; the default is 50%. A reported
rename is an inferred pairing, not an operation stored in the commit.
[Git diffcore](https://git-scm.com/docs/gitdiffcore)

`git log --follow` demonstrates following a file through renames, but supports
only one file at a time. It is not a notebook-wide identity resolver.
[Git log](https://git-scm.com/docs/git-log)

## Controlled experiment

Ran four disposable repositories using Git 2.50.1 (Apple Git-155), with global
and system Git configuration disabled, on 2026-09-14. No Donut data was used.
Each comparison used `git diff --name-status -M50% BASE TIP`, then the same
command for every adjacent parent/child pair. `R100` means identical-content
pairing; other R numbers are Git's similarity scores. They are not probabilities.

| History authored in the experiment | Overall diff | Adjacent diffs | Interpretation |
| --- | --- | --- | --- |
| Rename `Old.md` unchanged, then completely rewrite `New.md` | Add New, delete Old | `R100 Old → New`, then `M New` | History retains a strong continuity candidate lost by the overall diff. |
| Rename A → B → C → D, changing another 30 of 100 lines each time | Delete A, add D | `R062 A → B`, `R065 B → C`, `R069 C → D` | Incremental similarity can reveal a candidate chain despite weak endpoint similarity. |
| Delete Old in B; recreate the same bytes as New in C | `R100 Old → New` | Delete Old, then add New | The overall diff hides a gap; whether it means intentional replacement or temporary removal is a product question. |
| Two byte-identical notes; actually move A → Y and B → X together | `R100 A → X`, `R100 B → Y` | Same pairings | Git produces a definite-looking result even though it cannot recover which actual move belonged to which identity. |

The first case used 100 lines of
`original unique line NNN with notebook content`, renamed the file without
editing, then replaced those lines with
`replacement completely different unique line NNN with notebook content`.
The gradual case started with those original lines and, at step k = 0, 1, 2,
replaced lines 30k through 30k+29 with
`changed stage k entirely different replacement sentence number NNN`.
The gap and duplicate cases used identical generated 100-line documents.
Each step was staged and committed; moves used ordinary filesystem renames.
These recipes and commands reproduce the comparisons without relying on the
temporary repository or its commit IDs.

These observations establish examples, not an accuracy rate on owner notebooks.
No Donut/JGit implementation, latency benchmark, or threshold selection was
tested. In particular, a Git CLI pairing is not evidence that the current
Donut implementation accepts the same change.

## Implications for Donut

1. **Separate identity analysis from database mutation.** Start from the current
   accepted identity mapping, inspect the proposed range as needed, derive the
   final correspondence, then apply one final projection. An exact move in B
   followed by an edit in C can be composed without creating a live B state.
2. **Do not treat a Git pairing as an identity oracle.** Multiple candidates,
   copied templates, tiny notes, complete rewrites, path reuse, and deletion gaps
   can remain ambiguous. Chaining weak pairings can compound mistakes; no score
   tested here establishes user intent. Even exact bytes are a domain policy
   for inferring continuity, not a mathematical proof.
3. **History can help without requiring valid intermediate Markdown.** Path and
   blob evidence remains inspectable when an intermediate file has invalid YAML.
   Notebook validation belongs to the proposed tip; Git object integrity and
   ancestry checks belong to the transferred history.
4. **Whole-range combinations need history analysis now.** Combining today's
   unchanged-content rename with a later content edit already defeats exact
   endpoint matching. The accumulated-publication story cannot defer all
   history analysis to the broader rename-and-edit story. The latter owns new
   inference/ambiguity capabilities beyond composing already supported changes.
5. **An unchanged final tree need not imply unchanged identity.** Deleting and
   recreating a path can end with identical bytes. Conversely, a temporary local
   removal undone before publication may have no intended deletion effect.
   Resolve that meaning before implementing irreversible entity deletion.
6. **Folder identity needs its own evidence.** Git documents directory rename
   detection for merge/cherry-pick, not general directory-rename output from
   `git diff`. File candidates do not automatically identify a Donut Folder,
   especially when its contents are empty or split between destinations.
   [Directory rename detection](https://git-scm.com/docs/directory-rename-detection)
7. **Bound inference cost deliberately.** Git's exhaustive rename/copy fallback
   is quadratic in candidate counts and can be curtailed by a rename limit.
   Inspecting more commits adds work. An exhausted search must not silently
   become a destructive delete/add decision. Fix algorithm settings and surface
   unresolved correspondence consistently; measure realistic notebook histories
   before selecting budgets. [Git diff](https://git-scm.com/docs/git-diff)

## Critical review of final-revision-only projection

The proposal avoids making invalid drafts live, repeated indexing, and
temporary destructive operations. It preserves useful Git history independently
of which revisions Donut can display. It does not remove the need to inspect
history or settle private identity.

- **Define what “all commits” means:** all objects reachable from accepted
  main, with original IDs; not unpublished local branches, reflogs, or unreachable
  objects. A full clone contains that history even if an old revision cannot be
  opened as a valid Donut notebook.
- **Use two validation boundaries:** received Git graph/object integrity and
  permitted ancestry for the range; Portable-format, authorization, business
  invariants, and identity consistency for the final projection. Retaining an
  invalid Markdown draft must not imply rendering it through live application
  parsers or skipping safe object handling.
- **Make acceptance indivisible:** prepare the final state before publishing the
  ref; commit content, identity outcomes, derived state needed on return, and
  accepted head atomically. If Git storage is external to the database, make
  immutable objects durable first and advertise only the database-accepted head.
  A crash must not leave an advertised head without its corresponding projection
  or accessible objects. Retries must not repeat deletion or allocate new IDs.
- **Keep final-tree-equal publications:** new commits may end at the same tree
  as the base. Preserve the commits, and resolve any identity significance;
  “nothing changed” is not sufficient grounds to reject the history.
- **Publication boundaries matter for destructive effects:** if a deletion has
  already been accepted separately, the old learning data has been removed under
  current behavior. A later file cannot recover it from Git content alone. For a
  deletion/recreation wholly inside one unpublished range, identity meaning is
  still open. Do not promise identical private-data outcomes regardless of when
  the owner published.
- **Do not promise universal inference:** arbitrary ID-free snapshots cannot
  always distinguish rename/edit from delete/create. Safe refusal is a workable
  default proposal; an owner-confirmed correspondence mechanism would require
  a separate explicit policy and transport design. Rewriting history is not an
  acceptable required resolution under the chosen direction.
- **Resolve divergence separately:** two independent descendants cannot both
  form one original-ID linear history without relaxing no-rewrite/no-merge.
  Clear rejection is coherent for now; automatic reconciliation is not supplied
  by final-only application.

## Recommended ADR wording

Retain the original linear Git history; require only the newly published tip
to have a valid Donut projection. Determine identity using the current accepted
mapping and relevant evidence from the entire proposed range. Apply the resolved
base-to-tip result once, atomically, without replaying intermediate Donut states.
Unresolved identity conflicts preserve both sides and block acceptance, rather
than silently guessing or requiring users to rewrite their history.

The detailed ambiguity policy, deletion-gap semantics, and any owner-assisted
resolution remain open design decisions in the ADR draft.
