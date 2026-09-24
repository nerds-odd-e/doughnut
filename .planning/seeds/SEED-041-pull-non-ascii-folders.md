---
id: SEED-041
status: dormant
planted: 2026-09-24
planted_during: execution retrospective of SEED-035 story 20 (tests on LFS notebooks)
trigger_when: now; queued first by the owner as a bug
scope: small
---

# SEED-041: Pull treats non-ASCII folders like any other folder

## Why This Matters

Owners who name folders in Chinese or other non-ASCII scripts get a different
pull from the one ASCII folder names get. The finding came from the review of
SEED-035 story 20, which fixed the same fault on the publish side
(commit 86bf69d2d7: `notebookPublishLfsSelection.ts` now reads
`git ls-tree -r -z`). The owner queued it first on 2026-09-24.

- **Expected:** With unpublished local commits in a checkout, `donut notebook
  pull` receives a note added on the web under an existing folder named `例文`
  exactly as it receives one under an ASCII-named folder.
- **Actual (from code reading):** `isRepresentedFolder` in
  `cli/src/commands/notebook/notebookAcceptedAdditionInterval.ts` runs
  `git ls-tree -d --name-only <commit> -- <folderPath>` without `-z` and
  compares the trimmed output with `folderPath`. Git quotes non-ASCII paths
  (`core.quotePath`), so `例文` is listed as `"\344\276\213\346\226\207"` and
  the check fails. `isEligibleAcceptedAdditionInterval` is then false, and
  `notebookAcceptedInterval.ts` (around line 68) takes the structural or
  non-linear path instead of the addition path.
- **Evidence:** a throwaway Git experiment showed the quoted listing without
  `-z` and `例文\0` with `-z`. Not yet reproduced end to end through a pull.
  The other CLI Git calls that parse paths already use `-z` (`diff-tree`,
  `ls-tree -r`, `ls-files -u`); `status --porcelain` only counts lines.
- **Unknown:** what the owner sees on the fallback path (a refusal, a
  different rebase, or the same result by a slower route).

## Alternatives and Decision

- **Recommended:** read NUL-separated output (`-z`) and compare without the
  trailing NUL, as the publish-side fix does.
- **Rejected:** setting `core.quotePath=false` for the call; `-z` is the rule
  the other CLI path readers already follow.

## Story Decomposition

<a id="story-1"></a>

### Pull receives web notes under non-ASCII folders like any other folder
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless","assessment":"ready","reasons":[],"basis":{"document":"1b83f45923156d27e032878c7b735f4aa253672e38c29295a5b3d54a8c9e50fa"}}
```

- **Identity:** SEED-041#story-1
- **Kind:** Bug, queued first priority by the owner (2026-09-24).
- **For / why:** Owners whose folder names use non-ASCII characters, whose
  pulls with unpublished local work may be handled differently or refused.
- **Evaluation:** First reproduce through the CLI pull entry point (the
  `cli/tests/notebookPull.addition.suite.ts` style) and record what the owner
  sees; then the same scenario with an ASCII folder and with `例文` gives the
  same pull result.
- **Key examples:**
  1. A checkout has one unpublished local note edit; on the web a note
     `例文/B.md` is added under the existing folder `例文`. `donut notebook
     pull` receives `例文/B.md` and keeps the local edit, as it does for
     `examples/B.md`.
  2. The same with a nested non-ASCII folder `物理/例文`.
- **Effort hypothesis:** S, medium confidence; route through dough-bug-fixing.
- **Depends on:** none.
- **Safe stopping point:** Non-ASCII folders take the same addition path;
  ASCII behaviour is unchanged.
- **Open decision for refinement:** If reproduction shows the fallback path
  already gives the owner the same result, decide whether to fix it anyway
  for consistency or close the report as no visible defect.

## When to Surface

Now; first in the product backlog.

## Breadcrumbs

- The publish-side fix of the same fault is commit 86bf69d2d7.
