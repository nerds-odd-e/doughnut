# Align durable LFS contract with authorized oversized-intermediate exception

## Correction input

- Kind: bounded retrospective correction (completed execution).
- Source execution: completed fresh-checkout LFS delivery at
  `0b532162accef12392c4070423eb255215264fb0:.planning/quick/019-notebook-lfs-continuity/PLAN.md`.
- Provenance: story-branch commits
  `7db86ca0a5`…`df8c86b0db` on `story/notebook-lfs-continuity`
  (parent of first increment `e84967c03a`; branch base `e0fcf33229`).
  Omission behavior delivered in `fce754e518`; recovery commands documented in
  `7693f1df23`.
- Beneficiary: maintainers and agents reading the durable LFS attachment
  contract before changing admission, retention, or recovery.
- Bounded outcome: the Decision section of `docs/notebook-git-lfs.md` states the
  same narrow authorized exception the product already implements and that the
  Recovery section already describes.
- Current findings (scope):
  - `docs/notebook-git-lfs.md` Acceptance still requires object availability
    across newly admitted history without naming the owner-authorized exception
    for new oversized intermediate-only LFS payloads when the tip is valid.
  - Recovery already documents omitted oversized intermediates; North Star and
    the completed plan records Terry's ADR 0002 exception. Decision/Recovery
    disagree.
- Preserved promises and constraints:
  - Keep implemented admission (`NotebookGitAttachmentSizeAdmission` LFS path).
  - Do not widen the exception (tip-only retention, raw-Git leniency, or
    deletion of previously accepted objects remain out of scope).
  - Do not change ADR text without a separate human ADR decision; ADR 0002
    already defers retention detail to this durable LFS document.
  - Do not implement story 15 receive/rebase, browsing, migration, or schema
    release.
- Observable proof ownership: documentation change is verified by a focused
  consistency check against the existing admission proof boundary (no new
  product behavior).

## Outcome and stopping point

Readers of the durable LFS Decision learn that within-limit and tip objects
remain required, while a new oversized payload that appears only in unpublished
intermediate LFS commits may be absent when the tip is valid—matching Recovery,
North Star, and the delivered story. Safe stop: Decision and Recovery agree;
product code unchanged.

## Refined slices

### 1. State the authorized exception in the durable Decision
Type: Structure
Status: planned

Edit only `docs/notebook-git-lfs.md` Acceptance (and any immediately adjacent
Decision bullets that would still contradict the exception) so the narrow
oversized-intermediate omission rule is explicit where admission is defined.
Leave Recovery command examples in place unless a one-line cross-reference
removes contradiction. Do not edit ADR files, North Star, or product code.

Accepted proof:
- Promise: Decision text matches the implemented LFS admission exception and no
  longer contradicts Recovery.
- Boundary: durable LFS contract document versus
  `NotebookGitAttachmentSizeAdmission` LFS path /
  `NotebookGitAttachmentSizeAdmissionLfsHistoryControllerTest`.
- Setup: none (read current revision).
- Observations: Decision names tip validity, new oversized intermediate-only
  payloads, within-limit retention, and unavailable recovery; Recovery remains
  consistent.
- Command: `CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests '*AttachmentSizeAdmission*LfsHistory*'`
  (regression only; expect unchanged behavior).
- Result: (not run — planned)

## Delivery notes

- Planning-only from dough-execution-retrospective; not Taken; not authorized
  for execution by this review.
- No seed required for this correction.
- Next plan number after `020-notebook-lfs-receive`.
