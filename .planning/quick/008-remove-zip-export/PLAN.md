# Remove the ZIP export feature

## Source and outcome

- Source: [SEED-009#story-46](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-46).
  Work item: **SEED-009#story-46**.
- Authority: the owner asked to analyze and refine the story, write its slice
  plan, and refine that plan if needed. Planning only; implementation is not
  authorized.
- Goal: remove notebook ZIP export completely so Git acquisition is the only
  supported way to obtain the Portable notebook tree locally and the product no
  longer carries a second export path.
- Included: catalog and settings affordances; frontend download utilities and
  dependency; notebook ZIP API, service and builder; generated OpenAPI client;
  ZIP-specific unit and E2E coverage and harness code; current product prose;
  export-shaped names around the surviving live Portable-tree model.
- Excluded: replacement navigation or browser download; redirects, compatibility
  responses, deprecation/history prose, or an absence-specific regression test;
  wider Git authorization, binding migration or recovery; changes to the CLI or
  Git bundle contract; generic ZIP handling used by EPUB or unrelated CLI tests.
- State: slices 1–2 delivered; slice 3 planned.

## Existing solution and constraints

PFE finding — deletion needs no replacement solution. The existing Git workflow
already owns local notebook acquisition; the existing live Portable-tree model
already owns assembly for every surviving consumer.

| Responsibility | Existing owner | Decision |
| --- | --- | --- |
| Local acquisition | CLI `notebook clone` over `downloadNotebookGitBundle` | Reuse unchanged; do not expose it in the web UI or widen its authorization |
| Live Portable content | `NotebookLivePortableTree` and `PortableTreeSnapshot` | Keep one shared implementation, but move it out of `notebookExport` and remove export-shaped names |
| Accepted local content | Git binding, object store and accepted `main` | Reuse unchanged; no binding migration or fallback is introduced |
| HTTP client contract | Spring controller annotations → generated OpenAPI and TypeScript client | Delete the controller operation, then regenerate; never hand-edit generated artifacts |

This follows [North Star: One notebook tree](../../NORTH-STAR.md#one-notebook-tree),
[One format boundary](../../NORTH-STAR.md#one-format-boundary), Accepted
[ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
(one Git synchronization and acquisition model),
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
(one Portable-tree classification and codec), and
[ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) (`/api` is the
OpenAPI-described HTTP boundary). No Accepted ADR conflicts, exception, new ADR,
or North Star change is needed.

**Common rule:** ZIP was an output wrapper around the same live Portable tree
used by Git. Remove the wrapper and its public surfaces; keep and capability-name
the single tree. Do not preserve feature history in production names, tests,
routes, or documentation.

## Outside-in proof

Do not add a test saying the removed feature is absent; that would retain the
feature as a product concept. Prove final absence through the public contract and
source audit, and prove the surviving user outcome at its existing Git boundary.

| Story promise | Owning slice and proof |
| --- | --- |
| No web ZIP action or replacement navigation | 1: existing catalog/settings frontend tests remain green; frontend typecheck; focused source audit has no feature affordance or download helper |
| No notebook ZIP HTTP/client contract or compatibility behavior | 3: generated OpenAPI/client contains no operation or route; backend and OpenAPI checks pass; focused source audit finds no ZIP endpoint/service/builder |
| Git acquisition still returns the accepted tree, including exact-byte attachments | 2: existing Git bundle controller proof and CLI publication-to-clean-clone E2E stay green after the shared-tree move; slice 3 does not touch that boundary |
| No export-shaped residue in the surviving Portable-tree design or current product prose | 2 and final focused source audit in 3 |

Commands are run through Nix except Git:

- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
- Frontend: `CURSOR_DEV=true nix develop -c pnpm frontend:test`
- Frontend typecheck: `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
- API generation: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`
- OpenAPI lint: `CURSOR_DEV=true nix develop -c pnpm openapi:lint`
- E2E preservation: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/cli/cli_notebook_publish_to_clean_clone.feature`

## Slices

### 1. The web offers no notebook ZIP export
Type: Behavior
Status: done
Behavior: given an owned or readable notebook in the catalog or notebook
settings, when its available actions are shown, no ZIP export action is offered
and no replacement acquisition navigation is added.
Change: remove both Vue controls and their imports/handlers; delete
`notebookExport`, `contentDispositionFileName`, `isPrintableAscii`, their tests,
and the catalog-export spec; remove the now-unused `file-saver` dependency and
lockfile entry. Delete the notebook-export feature, step definitions, ZIP page
object, card/list export actions, active-spec entry, ZIP reader task and file,
file-wait/download-cleanup support that becomes unused. Preserve the remaining
overflow and notebook-management actions without adding a placeholder.
Proof: full frontend tests and typecheck; `pnpm cy:lint`; focused `rg` over
`frontend/` and `e2e_test/` shows no export label, test id, download helper,
page-object action, ZIP-reader task, or export feature.
Sizing: one user-visible removal with one frontend contract; dependency install
and full-suite runtime are external-wait cost, not another implementation beat.

### 2. The live Portable tree has capability names
Type: Structure
Status: done
Internal change: while behavior is still unchanged, move the surviving shared
tree classes from `services.notebookExport` to a capability package such as
`services.notebookTree`; rename `Export*Row`, `ExportReadmeMarkdown`, and
`findExportRowsByNotebookId` to Portable-tree terms. Update imports, constructor
queries, tests, Javadocs, and `docs/notebook-git-synchronization.md` so they
describe the current Git/canonical-tree consumers without ZIP or export-history
language. Keep `NotebookZipBuilder` and `NotebookExportService` temporarily as
the only export-named code, consuming the newly named tree, so this slice is a
green behavior-preserving boundary.
Enables: slice 3 can delete the last ZIP-specific backend package and API without
leaving a false export boundary around the tree Git still uses.
Unchanged behavior: ZIP remains callable during this interim slice; Git cutover,
history reset, accepted web changes, drift detection, bundle download, clone and
exact-byte attachments retain the same tree.
Proof: full backend unit suite; existing Git bundle controller tests; the CLI
publication-to-clean-clone E2E scenario that round-trips text and exact-byte root
files. Focused `rg` shows export-shaped vocabulary only in the ZIP endpoint,
service, builder and their deletion-bound tests.
Sizing: the broad file count is mechanical import/type fallout from one package
move and one vocabulary rule, with one compiler/test loop and no independent
design beat. Expected implementation stays under the 10-minute hard-refinement
threshold; E2E startup/runtime is the stated external-wait exception.

### 3. The notebook ZIP API and builder no longer exist
Type: Behavior
Status: planned
Behavior: given Donut's generated HTTP client or a direct request to the former
notebook ZIP route, when notebook operations are inspected or requested, there
is no ZIP operation and no redirect, compatibility response, or alternative web
route; the existing Git bundle operation is unchanged.
Change: remove `NotebookController.exportNotebook` and its injected
`NotebookExportService`; delete that service, `NotebookZipBuilder`, and all ZIP
controller/service/builder tests, including the export-only attachment test.
Regenerate `open_api_docs.yaml` and `packages/generated/donut-backend-api/**`
from the Java source. Do not hand-edit generated files. Remove any remaining
current documentation or harness residue discovered by the final audit; leave
unrelated EPUB and CLI ZIP utilities intact.
Proof: full backend suite; API generation; OpenAPI lint; full frontend suite and
typecheck against the regenerated client. Focused final `rg` confirms no
`exportNotebook`, notebook `/export` route, export service/builder, generated
operation/types, export test/feature, `notebookExport` package, export-row/query
names, ZIP UI label, or ZIP-feature prose. Existing Git bundle controller tests
remain green; reuse slice 2's unchanged-boundary CLI evidence.
Sizing: production work is deletion plus generator output. Generation and full
suite runtime are the stated external-wait exception; there is no separable
implementation outcome inside the slice.

## Current decisions

- Removal applies even to read-only/browser-only users and notebooks without a
  usable binding; this story does not preserve their old ZIP path or expand Git
  access.
- No absence-specific test, tombstone route, replacement navigation, or product
  history survives. The OpenAPI/source audit proves deletion without making the
  removed feature a permanent concept.
- Generic ADR 0004 wording about exporting a Portable tree is a format/codec
  rule, not documentation of the ZIP feature; do not rewrite the Accepted ADR.
- Preserve generic ZIP utilities only when a non-notebook-export caller remains,
  notably EPUB handling and CLI test fixtures.

## Plan-refinement assessment

All 3 slices have one gate and one proof loop. Slice 2's wide mechanical rename
is one coherent model change rather than accumulated special cases; splitting it
would create temporary competing package/type vocabularies without an
independently evaluable outcome. No slice has an unexplained path beyond the
10-minute hard limit once stated generator, full-suite, and E2E waits are
excluded. Result: **ready for direct execution** when separately authorized.

## Remaining concerns

No slice-specific concern remains from this assessment. Execution should still
re-run the final focused source audit because generated output and active test
harness references are easy places for dead feature vocabulary to hide.
