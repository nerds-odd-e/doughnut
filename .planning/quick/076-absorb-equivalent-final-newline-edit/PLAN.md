# Absorb an equivalent final-newline note edit

Source: Manual UAT finding on 2026-09-08 against delivered
[SEED-009 story 9](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-9).

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

## Ordered slices

### 1. Absorb a final-line-terminator-only note replay
Type: Behavior
Status: done
Proof: `notebookPull.absorbed.suite.ts` public `run()` scenario with real Git
checkout/bundle; `pnpm -C cli exec vitest run tests/notebookPull.test.ts` and
`pnpm cli:test` green.

Behavior: A clean bound checkout has one unpublished ordinary-note edit, and
new eligible accepted history contains the same complete note content with only
the optional final LF differing → run `donut notebook pull <directory>` → the
redundant local replay is absorbed at accepted main without a paused rebase.

## Learnings

- Ordinary rebase pauses on exact final-LF-only blob mismatch; absorb by
  inspecting stage-2/3 without trimming, keeping accepted (`checkout --ours`),
  and `rebase --skip`. Fix stays in pull integration (ADR 0004), not codec
  normalization.
