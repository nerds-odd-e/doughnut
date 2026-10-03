# Remove the title-service trace from the voice-input notes

**Identity:** SEED-066#voice-docs-title-trace

## Source

Correction from the execution retrospective of
[Keep note titles under the author's control](../../seeds/SEED-066-voice-input.md#author-controlled-titles)
(plan `002-keep-titles-under-author-control`, commits `aa093fffeb` and
`3ba7ca22c4`). Story: [Remove the title-service trace from the voice-input notes](../../seeds/SEED-066-voice-input.md#voice-docs-title-trace).

## Findings

At `3ba7ca22c4`, `docs/voice-input.md` still describes the removed automatic
title service:

- Observation boundary: "persistence and title services were exercised".
- Navigation observation: "Both content PATCHes and title requests targeted
  the source" and "despite successful source title-suggestion responses".
- The new "Dictation does not change titles" section is a negation note, and
  its last sentence says earlier observations "predate this".
- Responsiveness table: "title settled separately" and "body/title settled
  before Stop".
- Intro: "the title stays as the author set it" restates the removed
  behavior's absence.

The owner's standing rule is that removal leaves no historical note or
negation in docs; Git history is the record.

## Goal and scope

Edit `docs/voice-input.md` so it reads as if automatic titles never existed:
drop the title-service wording above, the "Dictation does not change titles"
section, and the intro's title clause, keeping every body and timing
observation. Keep "Dictation writes only to the note body."

- **Excluded:** product code, tests, and other docs. The conversation tool's
  "Suggested title" reply is unrelated.

## Outside-in proof

| Promise | Owning slice | Proof |
| --- | --- | --- |
| The notes no longer mention automatic titles | 1 | `grep -n -i -E "title (service|request)|title-suggestion|title settled|body/title|change titles|predate|stays as the author" docs/voice-input.md` prints nothing, and every body and timing observation is still present |

Documentation only; no test or typecheck gate applies.

## Slices

### 1. Voice-input notes describe only body dictation
Type: Structure
Status: planned
Proof: the `grep` above, read against the findings list.

Make the edits listed in Goal and scope.

## Current decisions

- One documentation slice; no code changes.
