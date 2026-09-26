# Publishing costs what the change costs

## Source

- Story: [SEED-046#story-1](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-1)
- **Identity:** SEED-046#story-1
- Direction: [North Star — one attachment content model](../../NORTH-STAR.md#one-store-for-file-bytes):
  file bytes are verified once, when stored; accepted history selects versions.

## Goal and scope

Publishing from a local checkout costs what the published commits change, not
the length of accepted history or the files they leave alone. The CLI and the
server each look only at the attachments that the first-parent unpublished
commits change, instead of walking every accepted commit and every current
file.

Kept: size limit for new files, Book source protection, Markdown validation,
forward-only history, the raw-attachment refusal, the missing-content refusal
for any changed attachment (intermediate commits included), and verification
of changed content against its pointer. An over-limit file the accepted
notebook already holds (a Book source, a legacy file) stays publishable when
unchanged, moved or renamed.

Owner decisions (2026-09-26): the earlier-accepted over-limit rule is not the
center of the problem; real GCS stays out, content-store reads are counted
instead. Consequently, and deliberately:

- "Previously accepted" means present in the accepted head tree, looked up
  only when a changed attachment is over the limit. Restoring an over-limit
  file deleted in older history is refused.
- An over-limit file added and removed again inside unpublished commits is
  refused as oversized (the owner rewrites local history, as the existing
  amend-recovery scenario shows). The CLI no longer skips uploading it.

Excluded: GCS, network time, clone, pull, CLI wording, the whole-history
bundle in each direction, per-file overhead of many new files, the server's
whole-tree projection checks. These return only if the final measurement shows
one of them dominant — then stop and report, do not extend.

## Outside-in proof

Entry points: backend `NotebookGitController.publishNotebookGitProposal`
controller tests with the in-memory content store's `getCalls()` (as in
`NotebookGitWebContentSaveCostControllerTest`); CLI `run(['notebook',
'publish', …])` suites with the `spawnSync` intercept of
`notebookPublish.lfs.testHelpers.ts`, counting `git` processes and recording
`lfs push` object ids; timing on the local Development stack.

| Key example | Proof |
| --- | --- |
| 1 one-line edit, ~100 MB files, ~40 commits: fast, no content read | slice 1 server cost test (`getCalls()` = 0 with files and a long history); slice 2 CLI cost suite (git process count equal for 1 and many accepted commits × files; no `lfs push`); measurement after slice 2 |
| 2 one-line edit, ~1,036 files / 300 MB / 47 commits | measurement after slice 2 (counts covered by example 1's size independence) |
| 3 +12 new files | slice 1 (`getCalls()` = number of new digests); slice 2 (`lfs push` ids = exactly the new digests); measurement |
| 4 one changed file | slice 1 (`getCalls()` = 1); slice 2 (`lfs push` ids = the new version only) |
| 5 over-limit new file refused; note edit beside an accepted over-limit file publishes | existing `NotebookGitAttachmentSizeAdmissionControllerTest` (limit inclusive, own-notebook only) green; slice 1 cost test fixture holds a `LIMIT + 1` file |
| kept: raw attachment refused, missing intermediate content refused, failed upload keeps local state | existing `notebookPublish.lfsFailure.test.ts`, `refusesMissingWithinLimitIntermediateHistoryEvenWhenTipIsValid`, `unchangedTipHistoryStillRequiresWithinLimitIntermediatePayload`; e2e `cli_notebook_attachment_size_admission.feature` |

**Baseline (before slice 1's change; owned by slice 1):** there is no saved
reproduction from the manual test, so record one at the current revision on
`pnpm dev` with a script kept beside this plan (`measure-publish.sh`, deleted
at wrap-up). It builds, through the CLI, notebook A (twelve 8,912,896-byte
`/dev/urandom` files, then 40 note-only commits published one by one) and
notebook B (1,000 × 200 KB files plus 36 × 8.5 MB, then ~45 note commits),
then times and counts: one-line edit on A and B, +12 files (100 MB) on A, one
changed 8.5 MB file on A. For each publish record wall time, CLI user+sys
(`/usr/bin/time -p`), and the number of CLI git processes
(`GIT_TRACE=<abs path>` then `grep -c 'trace: built-in: git\|trace: exec: git'`).
Server content reads are proven by the controller tests, not on the stack.
Rerun the same script after slices 1 and 2 and add each row to Learnings.

**Profile gate:** the baseline must confirm the hypothesis from code reading —
the CLI git process count for a one-line edit on A and B grows with accepted
commits × files (thousands on B) and dominates CLI user+sys. If it does not,
record what dominates and stop for replanning before slice 1's change.

Commands:
- `CURSOR_DEV=true nix develop -c pnpm cli:test`
- `CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*NotebookGitAttachmentSizeAdmission*' --tests '*NotebookGitPublishCost*' --tests '*NotebookGitBookSourceFileProtection*' --tests '*NotebookLfsTransfer*'`
- `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_lfs.feature,e2e_test/features/cli/cli_notebook_attachment_size_admission.feature`

## Slices

### 1. The server checks only the attachments the published commits change

Type: Behavior
Status: done
Proof: new `NotebookGitPublishCostControllerTest` (examples 1, 3, 4 and the
over-limit neighbour of 5) and the admission suites green; e2e LFS and size
admission features green after the scenario removals below; baseline recorded
before the change and rerun after. Removing the server's history walk reads
only pointers, not stored content, so `getCalls()` cannot show it; the
measurement rerun is its evidence.

Sizing: above the 5-minute target because of the baseline's external wait
(building notebook B on the stack) and the e2e run; the code change itself is
one method plus deletions and cannot be split without keeping the tip digest
walk the intermediate rule needs.

Behavior: a Git-backed notebook with several attachments (one `LIMIT + 1`),
a long accepted history → publish a note-only commit / a commit adding files /
a commit changing one file → accepted as today; stored content is read
0 times / once per new digest / once; a changed over-limit attachment not in
the accepted head tree is refused with the existing oversized message before
any content read.

Change:
- `NotebookGitAttachmentSizeAdmission.admit`: for each first-parent commit in
  the range, walk only paths that differ from its parent (JGit `TreeWalk` with
  `TreeFilter.ANY_DIFF`); for each changed attachment: empty → skip, not a
  pointer → raw refusal, size mismatch for a digest → corrupt refusal,
  over the limit → refuse unless the accepted head tree holds the digest
  (looked up lazily, once), then `get` + `matchesClaim` → missing / corrupt
  refusals. Delete `attachmentPayloadDigestsInHistory`, the tip digest set and
  `isNewOversizedIntermediateOnly`.
- Delete `allowsOmittedOversizedIntermediateWhenTipCorrectionIsValid` and
  `trustedHistoryGrandfathersPreviouslyAcceptedOversizedPayload`; delete e2e
  scenarios "Corrective oversized intermediate LFS commit is omitted while the
  tip publishes" and "Explicit Git LFS fetch of an omitted oversized
  intermediate reports unavailable" with steps only they use. No negative test
  replaces them.
- Docs: `docs/notebook-git-lfs.md` (admission bullet ~line 35, omitted
  intermediate paragraph ~line 119, list item ~line 135) and
  `docs/notebook-git-attachments.md` (~line 141) state the new rule: an
  over-limit file is refused wherever the published commits introduce it,
  unless the accepted notebook already holds it.

### 2. The CLI uploads only the file contents the unpublished commits change

Type: Behavior
Status: done
Proof: new cost case in `cli/tests/notebookPublish.lfs.test.ts` (example 1:
equal git process count for an accepted history of 1 commit vs 20 commits × 10
attachments, and no `lfs push`; examples 3 and 4: pushed ids are exactly the
changed digests); existing LFS and LFS-failure suites green;
e2e LFS feature green; measurement rerun and compared with the baseline.

Behavior: a bound checkout whose accepted history has many commits holding
attachments → `donut notebook publish` of a note edit / new files / a changed
file → publish succeeds with a git process count independent of accepted
history and unchanged files; `lfs push` is skipped / carries only the new
digests.

Change:
- `selectRequiredLfsObjectIds` becomes: one `git log --first-parent --raw -z
  --no-renames <acceptedHead>..<proposedHead>` for changed attachment blobs,
  one `git cat-file --batch` for their pointer text; empty → skip, raw →
  existing refusal, size mismatch → corrupt refusal; return the distinct
  digests. Delete `attachmentPayloadDigestsInHistory`, `readBlobBytes`, the
  raw-blob digest derivation, `LIMIT_BYTES` and the intermediate-only skip.
- Delete the test "does not upload a new oversized intermediate-only object";
  keep "accepted raw history converted to LFS does not block publishing a
  pointer" green unchanged.

## Considered and excluded

- Caching accepted digests or precomputing them server-side: rejected by the
  standing performance expectation.
- Sending only `acceptedHead..main` in the bundle and not downloading the full
  accepted bundle to read the head: deferred; notebooks without files stay at
  0.5–0.8 s, so neither is dominant now.
- A store existence check instead of `get` for changed digests: would save
  reads of new content only, adds an interface method; not needed for the
  examples.

## Learnings

**Baseline (2026-09-26, pre-change revision on the dev stack, CLI bundle from
this checkout; `measure-publish.sh`, run 20260926-204034):**

| Publish | Wall s | CLI user+sys s | CLI git processes |
| --- | --- | --- | --- |
| A one-line edit (12 × 8.5 MB, 42 accepted commits) | 23.04 | 15.28 (3.95 + 11.33) | 580 |
| B one-line edit (1,036 files, 47 accepted commits) | 556.52 | 469.29 (244.92 + 224.37) | 49,797 |
| A +12 files (100 MB) | 8.08 | 6.02 (3.25 + 2.77) | 617 |
| A one changed 8.5 MB file | 8.05 | 6.02 (3.27 + 2.75) | 642 |

Profile gate passed: the git process count grows with accepted commits ×
files (tens of thousands on B) and CLI user+sys is most of the wall time.
Setup costs on the stack were also large: B's 45-commit note publish took
~23 minutes (server re-reads every file of every new commit).

**Slice 1 accepted proof:** `NotebookGitPublishCostControllerTest`
(`noteEditReadsNoStoredContent` 0 reads beside an accepted `LIMIT + 1` file,
`newFilesAreEachReadOnce` 3, `oneChangedFileIsReadOnce` 1,
`newOversizedFileIsRefusedBeforeAnyContentRead` 0; red before the change at
4/7/4/5) plus the size-admission, Book source, LFS transfer and web continuity
suites (19 tests); full backend 2,660 green; e2e `cli_notebook_lfs.feature`
10/10 and `cli_notebook_attachment_size_admission.feature` 1/1.

**Measurement rerun location:** the Development stack runs only the primary
checkout's backend, so the rerun from this story branch measures the CLI
(slice 2) against the unchanged server; the server-side timing rerun happens
after integration to main, when the dev backend reloads the change.

**CLI after slice 2, server pre-change (2026-09-26, run 20260926-213341;
`REUSE_RUN=20260926-204034 measure-publish.sh` on the baseline notebooks, now
3–5 accepted commits longer and A holding 24 more files):**

| Publish | Wall s | CLI user+sys s | CLI git processes |
| --- | --- | --- | --- |
| A one-line edit | 1.53 | 0.34 (0.21 + 0.13) | 17 (was 580) |
| B one-line edit | 7.68 | 0.49 (0.25 + 0.24) | 17 (was 49,797) |
| A +12 files (100 MB) | 2.10 | 0.50 (0.27 + 0.23) | 20 (was 617) |
| A one changed 8.5 MB file | 2.08 | 0.49 (0.29 + 0.20) | 20 (was 642) |

The CLI git process count no longer depends on accepted history or unchanged
files, and CLI CPU time is under 0.5 s everywhere. The remaining wall time is
the pre-change server plus transfers; B's 7.7 s awaits the server rerun after
integration.

**Slice 2 accepted proof:** `cli/tests/notebookPublish.lfs.test.ts` "costs
what the unpublished commits change" (equal git process count for 1 vs 20
accepted commits × 10 attachments and no push; pushed ids exactly
`[oid(101), oid(102)]` / `[oid(201)]`; all red before the change), and the
converted-history test now asserts exactly `[OID_B]` is pushed; `pnpm
cli:test` 468 green; e2e LFS 10/10 and size admission 1/1. The raw `--raw -z`
parser is shared with accepted-commit changes (`parseRawChangesZ`).
