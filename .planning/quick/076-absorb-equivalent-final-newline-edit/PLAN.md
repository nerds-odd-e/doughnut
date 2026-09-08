# Absorb an equivalent final-newline note edit

Source: Manual UAT finding on 2026-09-08 against delivered
[SEED-009 story 9](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-9).
Status: planned; refinement audit completed, no implementation performed.

## Goal and scope

A notebook owner can pull an accepted web edit when their one unpublished local
note edit has exactly the same Markdown bytes except for the final LF. Pull
finishes cleanly at accepted main, reports that no unpublished change remains,
and preserves the original local tip through Git's existing recovery reference.

Keep the delivered Story 9 boundary: one unpublished single-parent commit,
one existing ordinary Markdown note, linear eligible accepted content history,
and no publication during pull. Substantive same-note differences must still
pause with the existing native Git continue/abort guidance.

Exclude web or database content normalization, changes to the Portable Markdown
format, CRLF normalization, trailing-space or general whitespace equivalence,
semantic Markdown merging, structural history, multiple local commits/notes,
new conflict UI, and automatic publication.

## Finding analysis and current decisions

- Reproduction: the shared file ends after the frontmatter; local and web both
  add the same visible body line, but the local blob ends in LF and the accepted
  web blob does not. Ordinary `git rebase` pauses with identical visible conflict
  arms because the blob's final-line-terminator state differs.
- The mismatch is real at the byte level but does not represent two competing
  authored values. It should follow Story 9's existing absorbed-patch outcome,
  not its substantive-conflict outcome.
- [ADR 0004 — OKF-compatible notebook Markdown profile](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
  requires the Portable codec to round-trip author-owned Markdown losslessly and
  separates format from integration behavior. Therefore the fix belongs in the
  pull integration path; it must not append or strip final newlines during web
  save, export, import, or persistence. ADR 0001 terminology is retained. ADR
  0002 remains Proposed and is non-binding.
- Extend `notebookPullRebase` only after an ordinary rebase has paused. Inspect
  the sole unmerged Markdown path's stage-2 and stage-3 blobs without trimming.
  Qualify only the exact relation `accepted == local + LF` or
  `local == accepted + LF`; when qualified, discard the now-redundant replay and
  finish at accepted head. Every other conflict remains paused and receives the
  current recovery guidance.
- Drive the behavior through the public CLI `run()` boundary with a real
  temporary Git checkout and accepted bundle. The backend remains the mocked
  external dependency; no internal helper is exported for testing.

## Outside-in proof

| Promise | Owner | Observable proof |
|---|---|---|
| A final-LF-only replay is absorbed | 1 | Pull succeeds; `HEAD` and `main` equal accepted head; checkout is clean; accepted bytes are present |
| Local recovery remains available | 1 | `ORIG_HEAD` still resolves to the original local tip |
| The user receives the existing absorbed outcome | 1 | CLI reports “No unpublished change remains” and a second pull reports unchanged |
| Real content conflicts are not auto-resolved | 1 | Existing body and nested-YAML conflict scenarios still pause with continue/abort guidance |
| Authored Markdown is not globally normalized | 1 | Accepted checkout retains the accepted blob's exact no-final-LF bytes |

## Ordered slices

### 1. Absorb a final-line-terminator-only note replay
Type: Behavior
Status: planned
Proof: Add the exact byte-level reproduction beside the existing absorbed
same-note scenario in `notebookPull.absorbed.suite.ts`. Exercise `run()` against
a real Git repository and bundle. Assert accepted `HEAD`/`main`, a clean checkout,
exact accepted bytes, preserved `ORIG_HEAD`, absorbed output, and idempotent next
pull. Keep the existing substantive body/YAML conflict scenarios green.

Behavior: A clean bound checkout has one unpublished ordinary-note edit, and
new eligible accepted history contains the same complete note content with only
the optional final LF differing → run `donut notebook pull <directory>` → the
redundant local replay is absorbed at accepted main without a paused rebase.

Start with the failing public-boundary regression. Add the smallest private
stage-blob equivalence check and rebase completion path in
`notebookPullRebase.ts`; reuse the current conflict-path and Git execution
infrastructure. Do not introduce a merge driver, a general whitespace option,
or an exported comparison utility.

Focused verification while iterating:
`CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookPull.test.ts`.
Before completing the slice, run
`CURSOR_DEV=true nix develop -c pnpm cli:test`.

Sizing: about five minutes, medium-high confidence. This is one predicate, one
existing rebase branch, and one public-boundary proof loop. If safe rebase
completion requires separable state-recovery behavior or approaches the
ten-minute hard limit, preserve the failing reproduction and refine this same
PLAN before continuing.

## Considered but excluded

- Canonicalizing every Portable note to a final LF: conflicts with the accepted
  lossless author-content contract and creates unrelated repository diffs.
- Normalizing content in the rich or Markdown editor: leaves other valid write
  paths inconsistent and turns an integration defect into an authoring policy.
- `--ignore-space-at-eol` or broader whitespace merging: can erase meaningful
  authored whitespace beyond the observed finding.
- A new browser-to-CLI E2E scenario: the manual UAT already established the web
  source, while the CLI boundary test reproduces the exact blobs with real Git
  more directly and deterministically.
- Changing conflict messaging: substantive conflict recovery already passed UAT
  and remains the required counterexample.

## Refinement result

Slice 1 is **Ready** and was not replaced. Stage-blob qualification and
completion of the same paused rebase are one cohesive conditional path, not
separable product outcomes or proof loops. The public CLI/Git scenario observes
all final-state promises, while the existing substantive-conflict scenarios
cover the rejection boundary. Extracting a Structure leaf would leave unused
private preparation if the Behavior were cancelled.

There was no execution attempt or overrun, no completed slice to preserve, and
no sizing exception. All scoped promises remain mapped to slice 1. Execution can
begin directly; the five-minute estimate is a planning hypothesis rather than a
guarantee.
