---
id: SEED-046
status: dormant
planted: 2026-09-26
planted_during: owner review of the 2026-09-26 manual test of notebook files and Git integration
trigger_when: now; the owner asked for every finding to be queued
scope: large
---

# SEED-046: Notebook files and Git work stay fast and understandable

## Why This Matters

Owners who keep notebooks with files and work on them from local checkouts,
often with AI IDEs, alongside Web Donut are the beneficiaries. A two-hour
manual test on 2026-09-26 found that the delivered file and Git behavior is
correct and safe in most places, but publishing slows down with history once a
notebook holds files, pull forces a fresh clone after unrelated file changes,
CLI and upload messages are hard to act on, and web edits rewrite formatting.

The owner decided on 2026-09-26 that every finding deserves attention, that
the publish performance gap must be fixed, and that the clone success message
must become much shorter and better formatted.

Measurements come from the local Development stack (`pnpm dev`), whose file
bytes live in backend memory rather than GCS, so GCS transfer time is
excluded. Test files were incompressible random bytes (`head -c … /dev/urandom`);
"100 MB of files" means twelve 8,912,896-byte files.

## Alternatives and Decision

Doing nothing leaves publish unusable for notebooks with files and history,
which directly blocks the near-future direction of parallel work from local
checkouts. Raising the limit or telling owners to keep files elsewhere would
contradict the accepted "one store for file bytes" direction. Fixing each
symptom where it appeared would add code without removing the causes. The
recommended direction groups findings by the owner-visible journey that fixes
them together, puts the measured performance gap first, and keeps each story
independently deliverable.

Performance work follows the owner's standing expectation: remove the obvious
slowness so the work publish does scales with the size of the change, through
a simpler, more cohesive design with less code — not caches, background
precomputation, or special modes. If only a more complex fast path is found,
surface it as a decision.

Owner decisions on 2026-09-26 that removed candidates:

- Replacing a note's picture on the web keeps the previous file, because it is
  part of the notebook and its Git history
  ([attachments](../../docs/notebook-git-attachments.md)).
- AI guidance files keep following the ordinary note rules: `AGENTS.md`,
  `SKILL.md` and similar files must carry frontmatter with `type` to publish.
  Publishing them without it is dropped, not parked.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours. These are hypotheses
including delivery, not commitments.

<a id="story-15"></a>

### 7b. Note realm JSON lists each sidebar field once

**Identity:** SEED-046#story-15
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/013-note-realm-lists-sidebar-once/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"6f7de99d34dd03f2b4cca5a40641ae885f6fea3cef4fe57fe77fb1386bd2bd42","plan":"145b87e7bbecea54b1e28ec834fa6a56e7be66d2d0156f5c599996551e9d686d"}}
```

- **Goal:** Correction of story 7 (SEED-046#story-9; provenance
  `5d384c2665:.planning/slice-plans/010-responses-carry-no-orm-internals/PLAN.md`,
  commits 9365a11c7d and 5d384c2665). API consumers of a note realm (the web
  app, CLI, MCP) receive each of `notebookRealm`, `ancestorFolders` and
  `scopedReadmeContent` once, so the response describes the domain once
  instead of repeating the whole notebook, its readme included, in every note
  show, create, update and move response.
- **Scope:**
  - **Required:** the `GET /api/notes/{note}` body lists each of those three
    keys at most once, with today's values; `scopedReadmeContent` is absent
    when no scoped readme applies. The Jackson 2 note-realm serialization
    test, which exercises a mapper web responses never use, is retired.
  - **Preserved:** every note-realm consumer (frontend, CLI, MCP) and the
    generated API client, unchanged.
  - **Excluded:** where the folder trail's response conversion lives (low,
    cohesion-only); the notebook object's shape; folder and file page realms,
    which already list each field once.
- **Key examples:**
  1. A note in `outer/inner`; `GET /api/notes/{note}` → the raw body contains
     `"notebookRealm"` and `"ancestorFolders"` exactly once each (today twice).
  2. Boundary: a note at notebook root with no scoped title pattern → the body
     has no `"scopedReadmeContent"` key.
- **Plan:** [013-note-realm-lists-sidebar-once](../slice-plans/013-note-realm-lists-sidebar-once/PLAN.md)
- **Effort hypothesis:** S — high confidence.
- **Depends on:** none (story 7 is delivered).

## Ordering and Scope Reduction

Correction 7b finishes story 7's response shape by removing the note realm's
duplicated keys; it is small and stands alone.

Story numbers are local order; identities keep their original anchors.

## Open Decisions

None that change selection or order.

## When to Surface

Now; correction 7b is first in the backlog list.

## Breadcrumbs

- Manual test session 2026-09-26 (SEED-045#story-1, history recoverable at
  7499f4454a).
- [Git synchronization](../../docs/notebook-git-synchronization.md),
  [Git LFS attachment storage](../../docs/notebook-git-lfs.md),
  [attachments](../../docs/notebook-git-attachments.md),
  [ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
