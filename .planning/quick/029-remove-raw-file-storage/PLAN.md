# Remove the legacy raw file storage

## Source

- Identity: SEED-035#story-19.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-19)
  (refined 2026-09-24, owner accepted; release staging corrected 2026-09-24,
  see Current decisions).
- Governing direction: North Star
  [one attachment content model](../../NORTH-STAR.md#one-attachment-content-model),
  "every notebook on LFS" and "moving before retiring".
- **Start condition:** met. Story 14's startup conversion shipped in `v1.3.23`
  (Application Release: admission, GCP deploy and health probe all succeeded,
  2026-09-24). Story 20 was delivered (`8f83e1ebde`): tests no longer need raw
  notebooks except the raw-subject tests this plan deletes.

## Goal and scope

Maintainers keep one file representation in code: no code asks how a notebook
stores its files, and the `attachment_representation` column is gone.

Included: the size check on publish, the file reader and the publisher take
the single LFS path; `RAW`, the enum, the entity field, the repository query
and the column go; story 14's startup conversion, its testability endpoint,
the raw demotion endpoint, their generated client, E2E step, page object and
scenario, the backend raw fixture and its raw-subject tests go; the
`@Order` on `FlyWayFreeVersionRealMigration` goes.

Excluded: legacy picture, `attachment_blob` and Book storage (story 18);
shrinking bundles; rewriting history; any change to how raw blobs already in
accepted history are read, cloned or pulled.

Assumptions (checked on `origin/main` `a061808c28`, 2026-09-24):
- Main-code readers of the representation are exactly:
  `NotebookGitAttachmentSizeAdmission.admit` (branches to `admitRawRange`),
  `NotebookAttachmentFile.lfsPointer` (filters on LFS),
  `NotebookGitProposalPublisher` (passes it to the size check),
  `NotebookGitLfsConversionService`, `NotebookGitLfsConversionOnStartup`,
  `NotebookGitBindingRepository.findNotebookIdsByAttachmentRepresentation`, and
  `NotebookGitTestabilityController` (`force_raw_notebook_git_binding_for_testability`,
  `convert_raw_notebook_to_lfs_for_testability`).
- The LFS size check already inspects only the range after the accepted head
  and ignores non-pointer blobs when collecting grandfathered digests, so raw
  blobs in accepted history stay accepted without a raw branch. The CLI side
  (`notebookPublishLfsSelection.ts`) already accepts raw history and keeps its
  test (`cli/tests/notebookPublish.lfs.test.ts`, "accepted raw history
  converted to LFS does not block publishing a pointer").
- The only E2E users of the two raw testability endpoints are the
  "An existing checkout of a raw notebook pulls its conversion to LFS and
  continues on LFS" scenario in `e2e_test/features/cli/cli_notebook_lfs.feature`
  and its steps in `e2e_test/step_definitions/cli_notebook_clone.ts`.
- Story 4 (plan 027, executing in parallel) plans a "non-LFS notebook fails
  loudly" upload check tested on `createLegacyRawNotebook`. Whatever of it
  has landed on main when slice 3 starts is removed there too.
- Migrations run on `ApplicationReadyEvent`, after the new instance serves,
  while the other instance still runs the old code (db-migration skill,
  "Release safety"). The `bundle_bytes` precedent unmapped the column in
  `v1.3.15` (`b8480476f4`) and dropped it in `v1.3.16` (`ea65f212ce`).
- Story 14's startup conversion catches per-notebook failures and leaves those
  bindings `RAW`, so a successful deployment of `v1.3.23` alone does not prove
  that production has no raw bindings. The first raw-free release needs a
  read-only production check before deployment as well as its migration check.
- Next Flyway versions: `300000342`, `300000343` (newest is `300000341`;
  `300000339` is retired).

## Outside-in proof (key examples)

| Promise (seed example) | Slice | Observable proof |
| --- | --- | --- |
| Converted notebook with raw history: fresh clone has the old raw bytes, a new picture publishes (3) | 1 | New controller test on a product LFS notebook whose accepted history is seeded as raw `physics/diagram.png` then its LFS pointer: publishing a new picture is accepted, the bundle's first commit still reads the raw bytes, web download of the current file gives the current bytes. CLI unit test above stays green. |
| Publishing to any notebook uses the LFS size check; web download reads through the pointer (1) | 2, 3 | Existing LFS tests stay green: `NotebookGitAttachmentSizeAdmissionLfsHistoryControllerTest`, `NotebookGitAttachmentLfsPublicationControllerTest`, `NotebookAttachmentControllerTest` LFS download; no test builds a raw binding |
| One raw binding remains → defer deployment at the pre-deploy check; if present at migration time, migration fails naming the notebook ids without changing the column (2) | 4 | Read-only production query before release; migration test (JDBC, `backend/src/test/java/db/migration`, like `NotebookGitAcceptedHistoryCompletenessTest`): a binding set to `RAW` makes the check throw a message naming its notebook id; the column default is unchanged |
| No raw binding → new bindings store LFS without code writing it (1) | 4 | Same migration test: with no `RAW` row the migration succeeds and the column default is `LFS`; `NotebookGitBindingAssertions` no longer reads a representation |
| The column is gone (1) | 5 | Migration test or schema check: after migration `notebook_git_binding` has no `attachment_representation` column; full backend suite green |

## Slices

### 1. Raw history stays usable without the startup conversion
Type: Structure
Status: done
Accepted proof: `NotebookGitAttachmentLfsRawHistoryControllerTest`
(`rawHistoryStaysReadableWhileANewPictureIsPublished`) green before and after
the deletions, with the LFS publication, web continuity, LFS size-history and
`NotebookAttachmentControllerTest` classes; `cli/tests/notebookPublish.lfs.test.ts`
green; `cli_notebook_lfs.feature` 11/11; API client regenerated, frontend
`vue-tsc` clean. `findNotebookIdsByAttachmentRepresentation` was deleted here
(no other user).
Proof: new raw-history controller test (example 3) green before and after the
deletions; `cli/tests/notebookPublish.lfs.test.ts` green;
`pnpm cypress run --spec e2e_test/features/cli/cli_notebook_lfs.feature` green
without the deleted scenario; generated API client up to date.

Internal change: first add the example-3 controller test, seeding the accepted
history directly with `NotebookGitAcceptedHistoryFixture` (raw commit, then the
LFS `.gitattributes` and pointer commit, with the pointer row and stored bytes).
Then delete `NotebookGitLfsConversionService`, `NotebookGitLfsConversionOnStartup`,
`NotebookGitLfsConversionControllerTest`, both raw testability endpoints, the
`@Order` on `FlyWayFreeVersionRealMigration`, the E2E scenario, its two steps,
the `forceRawGitBinding`/`convertToLfs` page-object methods and
`testabilityNotebookGit.ts` wrappers; regenerate the API client.
Unchanged external behaviour: publishing, cloning and downloading LFS
notebooks, including ones with raw history. Enables slice 2 by leaving the
backend raw fixture as the only way to build a raw binding.

### 2. Publishing checks every file size on the LFS path
Type: Structure
Status: planned
Proof: `NotebookGitAttachmentSizeAdmissionLfsHistoryControllerTest`,
`NotebookGitAttachmentLfsPublicationControllerTest` and the slice 1
raw-history test green; `NotebookGitAttachmentSizeAdmission*Test` no longer
uses `createLegacyRawNotebook`.

Internal change: `NotebookGitAttachmentSizeAdmission.admit` always checks the
LFS range: delete `admitRawRange`, `attachmentObjectIdsInHistory`,
`objectLength` if unused, and the representation parameter; the publisher
stops passing it. Sort the raw-subject tests in
`NotebookGitAttachmentSizeAdmissionControllerTest` and
`NotebookGitAttachmentSizeAdmissionHistoryControllerTest`: keep a promise on
an LFS notebook only when no LFS test covers it (LFS tests for exact limit,
mixed refusal and grandfathering exist), otherwise delete the test; delete the
history class if nothing is left. Unchanged external behaviour: every LFS
publish. Enables slice 3.

### 3. Reading a file no longer asks how its notebook stores files
Type: Structure
Status: planned
Proof: `NotebookAttachmentControllerTest`, `NoteAttachmentImageControllerTest`,
`NotebookGitAttachmentLfsWebContinuityControllerTest` and the slice 1 test
green; `git grep -n 'createLegacyRawNotebook\|Representation.RAW' backend/src`
finds nothing.

Internal change: `NotebookAttachmentFile` parses every non-empty row as a
pointer without reading the binding. Sort the remaining raw-subject tests
(`NotebookAttachmentControllerTest`, `NotebookGitAttachmentCreationControllerTest`,
`NotebookGitAttachmentMetadataControllerTest`,
`NotebookGitAttachmentPublicationControllerTest`) by the same keep-or-delete
rule, then delete `createLegacyRawNotebook`. Remove any story 4 non-LFS upload
refusal and its test. Unchanged external behaviour: every LFS download and
save. Enables slice 4: no code reads the representation.

### 4. Bindings no longer record a representation (first release)
Type: Behavior
Status: planned
Proof: migration test for examples 2 and 1 above; full backend suite green.

Release gate: before deploying the raw-free code, run a read-only production
query and require no rows:

```sql
SELECT notebook_id FROM notebook_git_binding WHERE attachment_representation = 'RAW';
```

If any remain, leave the raw-capable release running and resolve those
notebooks before deployment. A successful `v1.3.23` deploy alone is not this
check: the startup converter logs individual failures and leaves them raw.

Behavior: production deploys this release → migration `V300000342` refuses,
naming every notebook id with a `RAW` binding and leaving the column unchanged;
otherwise it sets the column default to `'LFS'` → the entity
no longer maps `attachment_representation`, the enum,
`findNotebookIdsByAttachmentRepresentation` and the builder's
`representation(...)` are gone, and a newly created binding stores `LFS` from
the column default. The old instance still writes the column during the
rolling deploy, which the kept column allows.

After this slice, stop. The owner releases it; slice 5 waits for that release.

### 5. The representation column is dropped (next release)
Type: Behavior
Status: planned
Proof: after migration the column is absent (migration test or schema
assertion); full backend suite green.

Start condition: a release containing slice 4 is deployed (Application
Release admission and GCP deploy succeeded for a tag that contains slice 4's
commit). Do not commit this slice to main before that, because the next
release would otherwise carry both steps.

Behavior: the next release deploys → `V300000343` drops
`notebook_git_binding.attachment_representation`. No code maps it, so both
the old and the new instance keep working during the rolling deploy.
Update the db-migration skill's newest-file and next-version notes. Remove the
migration-only test of `V300000342` if it can no longer run once the column is
gone, keeping the drop assertion.

## Current decisions

- **Two releases, not one.** The seed's single refusing drop migration would
  drop a column the still-running old instance maps. Following the
  db-migration skill's release-safety rule and the `bundle_bytes` precedent,
  the refusal and default change ship with the code removal (slice 4), and the
  drop ships in the next release (slice 5). Check for remaining raw bindings
  before deploying slice 4. The migration rechecks after the new instance is
  ready; it cannot by itself prevent a brief interval of raw-free serving if
  an unconverted binding appears between the pre-deploy check and migration.
- A binding created in the seconds between the new instance becoming ready
  and `V300000342` running would get the old `'RAW'` default and make the
  migration refuse. The deploy then fails loudly (ADR 0006); the fix is to set
  that row to `LFS` (its content is LFS). Accepted as a negligible, loud risk
  rather than adding a third release.
- Example 3 is proved on a product LFS notebook with seeded history, not by
  keeping any raw code or fixture.

## Learnings

- The existing `commitOnTopOf`/`seedAcceptedHistory` helpers express a
  raw-then-LFS accepted history; slices 2 and 3 can rely on the slice 1 test
  when they remove the raw branches.
