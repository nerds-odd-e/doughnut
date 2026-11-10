---
id: SEED-121
status: active
planted: 2026-11-10
scope: story
---

# Publish large notebooks without losing history

<a id="publish-large-notebooks-without-losing-history"></a>

### Publish large notebooks without losing history

**Identity:** SEED-121#publish-large-notebooks-without-losing-history
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/088-publish-large-notebooks-without-losing-history/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"be8e962ce85d97b4b6ee238e38d296711369cddd88246355af1eea21dc2d49f1","plan":"5e2952eee4ddbf0c90e0a54304e3af8953728c28c08d0efb96c93882e59482d3"}}
```

**Goal.** A notebook owner can publish small committed Markdown edits to an
existing large notebook with the supported Donut CLI. If the live notebook and
accepted Git tree disagree, the owner gets a useful recovery path that
preserves accepted commits, learning data, and their unpublished work.

**Problem and evidence.** On 2026-11-10, a three-file Markdown publish from a
notebook with about 11,000 tracked files failed in CLI `0.5.9`. Its Git tree
listing was 1,394,368 bytes and reproducibly exceeded Node's 1 MiB subprocess
buffer. A temporary CLI workaround reached the server, which refused the
proposal because the live Portable tree differed from accepted `main`. Pull
confirmed the local commit was already based on the server's accepted head;
retrying returned the same refusal. The folder paths, READMEs, attachment
contents, and sampled notes matched; the differing path and cause remain
unconfirmed. The full-tree CLI scan was already replaced in source
(`7c96542978`); the public download inspected that day contained the newer
scan but reported `0.5.5`, below the installed version, so `donut update`
would skip it.

**Scope.**

- Verify publication of small committed Markdown edits when the full accepted
  Git tree listing exceeds 1 MiB. Deliver the existing scan fix through
  `donut update` from the affected installed version, with an accurately
  versioned build. Keep the normal update target usable across both CLI and
  application publication paths.
- Diagnose the reported live/accepted mismatch using the canonical Portable
  representation. Identify differing paths and whether entries are live-only,
  accepted-only, or have different content, including attachment pointers and
  structural entries when they differ. Establish the cause before selecting a
  repair; sampled note or attachment-payload equality is insufficient.
- Correct the confirmed cause and provide supported diagnosis and recovery
  for the affected notebook so the owner can retry publication. Preserve
  genuine live-only edits through an owner-reviewed forward recovery commit;
  repair encoding/projection defects against accepted Git content. Repair must preserve
  accepted commit IDs and ancestry, soundly identified Note/Folder identities
  and their dependent learning data, and pending local work. Git content cannot
  recover already-deleted private identities or learning history.
- Distinguish a stale local base, which uses the existing Pull/rebase/conflict
  workflow, from server projection drift, which Pull alone cannot fix. Ordinary
  Publish continues to refuse drift until supported recovery settles it.

Required restrictions follow
[ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md):
accepted content cannot be silently adopted or rewritten; publication remains
atomic and serialized per notebook; ambiguous identity changes are refused.
Existing file and upload-size policies still govern admission. These preserve
content, identity and storage correctness independently of this story's size
example.

Deferred promises: a general repair policy for arbitrary corruption, recovery
of already-deleted identities/learning data, and a performance commitment for
large unpublished change ranges. These are delivery limits, not new rejection
rules. The observed mismatch's cause and recovery remain this story's work.

**Key examples.**

1. An owner has the affected installed CLI (`0.5.9` in the incident) → runs
   `donut update` → receives a strictly newer, correctly versioned fixed build.
   A later application publication keeps that supported update path usable.
2. A healthy notebook's complete accepted tree listing exceeds 1 MiB; three
   Markdown files change in a local commit based on accepted head → Publish
   succeeds, retains submitted commit IDs and unchanged accepted ancestry,
   and applies the edits. Notebook size alone does not impose an edit limit.
3. Accepted head advances after local work → Publish explains the stale base;
   Pull rebases unpublished commits and pauses for real conflicts; after
   resolution, Publish can accept the resulting fast-forward proposal.
4. Local work is already based on current accepted head, but the server's live
   canonical tree differs → Publish refuses without applying that proposal;
   diagnosis identifies the differing paths and current accepted revision,
   and guidance points to server recovery rather than a Pull/retry loop.
5. The confirmed discrepancy has an evidence-supported repair with sound
   identity correspondence → supported recovery preserves accepted history,
   private identity/learning associations and local work; the owner Pulls if
   recovery advanced accepted head, resolves any real conflict, then publishes
   the pending changes.
6. Diagnosis shows genuine live-only authored edits → the owner reviews the
   exact differences and explicitly requests recovery against that observed
   accepted/live state → recovery appends a validated forward commit preserving
   those edits and existing identity/learning associations. If either state
   has changed, recovery asks for a fresh review rather than applying a stale
   decision. Ordinary Publish does not perform this adoption implicitly.

**Investigation — facts and remaining hypotheses (2026-11-10).**

- At repository revision `8c0f3a25d2`,
  `cli/src/commands/notebook/notebookPublishLfsSelection.ts` uses unpublished
  `git log --raw` changes instead of a complete accepted-tree scan. A local,
  unpaid synthetic replay used 11,000 accepted Markdown files (1,529,000-byte
  listing) and three pending Markdown edits. The old default-buffer
  `ls-tree` operation reproduced `ENOBUFS`; current CLI `run(['notebook',
  'publish', directory])` submitted one 31,995-byte Git bundle and reported
  success, leaving local `main` clean. Only backend fetch was mocked, so this
  observes CLI selection/submission, not backend acceptance or production
  recovery. The existing `notebookPublish.lfs.test.ts` cost test does not
  supply this oversized-tree fixture. Replay at `2026-11-10T05:35:26.794Z`:
  `CURSOR_DEV=true nix develop -c pnpm -C cli exec tsx --input-type=module - < /tmp/seed-121-cli-large-publish-probe.mjs`.
  That temporary probe has SHA-256
  `ee0c7fa12ddecc0425b23b6843021f60fc492f61d0969774800ce07944c5b1b0`;
  it creates and removes isolated Git/fake-token fixtures. Its `/tmp` retention
  is temporary, not a permanent regression test.
- The public GCS download observed at `2026-11-10 05:31:34 UTC` was 2,031,228
  bytes, SHA-256
  `2e15317910ec390b0a4eb9b2f2092368cb5861cc9413f7e249425fc93202a773`.
  It contained the fixed scan and embedded version `0.5.5`; the installed
  `~/.local/bin/donut` embedded `0.5.9` and the old scan. `update.ts` installs
  only strictly newer versions. The authentic installed predecessor was retained
  at `/tmp/seed-121-authentic-cli-0.5.9.mjs` (2,068,561 bytes, SHA-256
  `5b868908f33576604389ad9b41f26a21eaa6efef982c570e2a60488d3365525c`);
  `cli-v0.5.9` also exists. Temporary retention can expire; preserve or rebuild
  the authentic predecessor before updater proof, never relabel current source.
- `.github/workflows/cli-release.yml` supplies a tag-derived CLI version.
  `.github/package_application_artifacts/action.yml` builds with the package
  default version (`0.5.5`), while
  `infra/gcp/scripts/publish-application.sh` uploads that artifact to the same
  latest-object destination. Both writers can replace the update target;
  which writer produced the observed object is unconfirmed.
- `NotebookGitProjection.requireMatchingAcceptedTree` compares the complete
  canonical live path-to-blob-ID map against accepted Git. Its error supplies
  no differing paths. The publisher separately checks a stale expected head.
  Pull and bundle download acquire accepted history; they do not repair the
  live projection. History reset creates a new root and cannot satisfy this
  story's accepted-history promise.
- No specific differing path or encoder/publication defect is confirmed.
  The cheapest decisive observation is one consistent server-side comparison
  at a named accepted head, retaining differing bytes/pointers and enough
  identity/provenance evidence to distinguish encoding drift from genuine
  uncaptured edits. Existing per-folder/per-note HTTP reads do not constitute
  a single locked snapshot. Production diagnostics need the owner's access;
  no production mutation was performed during refinement.

**Alternatives, challenge and recommendation.**

| Candidate | Fit and consequence |
| --- | --- |
| Existing Pull and retry | Correct for a stale local base; cannot repair server projection drift. Reuse it for its existing purpose. |
| Diagnose, then narrowly repair the confirmed projection/encoding defect | Recommended for a representational defect: accepted content and existing private identities remain authoritative in their respective domains. The actual diff must establish the repair. |
| Explicitly preserve reviewed live-only changes in a forward recovery commit | Selected for genuine uncaptured authored content: retains accepted ancestry and private identities, validates the current tip, then uses normal Pull/rebase for pending local work. |
| Existing history reset | Retains live rows but replaces accepted ancestry, so does not meet the selected goal. |

Borrowed mechanism: materialized-view repair separates authoritative records
(accepted Git) from a derived content view (the live Portable projection),
compares them at a stable revision, and repairs the diagnosed discrepancy.
Its limit is consequential: MySQL is itself authoritative for private identities
and learning data, so a wholesale rebuild is unsafe. The analogy supports
diagnosis before repair; it is not evidence that a rebuild preserves identity.

Strongest challenge: a command could preserve every accepted commit while
discarding genuine live-only work, or preserve every live byte while blessing
an encoding defect. Neither establishes successful recovery. Recommend exact
diagnosis followed by the narrowest repair, preserving genuine live-only edits
through reviewed forward publication when the owner chooses that policy.
The developer selected this recommended policy on 2026-11-10. The exact diff
still determines the diagnosed cause and the narrow repair; it does not reopen
whether genuine live-only edits should be preserved.

**Architecture.** Design follows current Accepted
[ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
and its [synchronization contract](../../docs/notebook-git-synchronization.md):
reuse existing accepted-tree readers, the canonical encoder, notebook lock and
publication/domain owners. Diagnosis and publication must use one definition
of equality. A path/blob comparison can run consistently under that lock without
storing Git objects or reading attachment payloads; the existing publication
projector assumes a matching base and is not a general identity-safe restore.
[ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
owns exact Markdown/README/attachment representation;
[ADR 0006](../../docs/adrs/0006-failure-handling-accepted.md) supports deliberate
actionable failure guidance. Under
[ADR 0007](../../docs/adrs/0007-environments-and-isolation-accepted.md),
reproduction uses disposable isolated data and production investigation uses
ordinary authorized read access. No ADR exception has been selected.

**Current decision.** On 2026-11-10 the developer accepted preservation of
genuine live-only edits through an owner-reviewed forward commit, with
encoding/projection defects repaired against accepted content. There is no
remaining refinement question. Diagnosis of the actual incident is a
credentialed early probe in the executable plan; contrary evidence stops its
dependent repair and triggers in-place plan refinement without dropping the
incident's correction or recovery promise.
