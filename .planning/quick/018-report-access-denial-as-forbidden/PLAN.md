# Report denied access as a permission problem

## Source and goal

Status: done; compatibility repair verified after integrating remote dependency upgrade.

The user selected both findings from the 2026-09-06 manual UAT for fixes.
This plan owns finding 1; [the search plan](../019-close-search-on-result-activation/PLAN.md)
owns finding 2. They are independent; execute this plan first.

As a notebook user, I can distinguish denied access from a temporary service
failure, so I can use an authorized account instead of retrying an operation
that cannot succeed. Do not change who can access a resource.

Reproduction at `f0fd4bed63`: a seeded non-owner clones or publishes an owner's
notebook. CLI says `The server returned an error. Try again later.` Both
`GET /api/notebooks/{id}/git-bundle` and ordinary notebook GET return HTTP 500
from `UnexpectedNoAccessRightException`. No access bypass occurred. The original
report is `/tmp/donut-uat-multinote.PhXs01/REPORT.md`; the reproduction here is
self-contained and must be recreated with isolated fixtures during execution.

## Scope and current decisions

- Translate the specific `UnexpectedNoAccessRightException` at the shared HTTP
  exception boundary into HTTP 403. Include ordinary notebook reads and Git
  download/publication; other HTTP callers of that same exception consistently
  inherit the corrected classification.
- Use a specific handler in the existing highest-precedence
  `CustomRestExceptionHandler`; keep authorization checks and the checked
  exception unchanged. A bare status annotation is insufficient planning:
  `ControllerSetup` has a catch-all exception handler, so actual MVC dispatch
  must prove which handler wins.
- Return an empty 403 response. Existing CLI 403 handling already supplies
  permission-specific guidance; no new error DTO, misleading `BINDING_ERROR`,
  generated error enum, or response parser is needed. Do not disclose ownership,
  resource contents, credentials, or a stack trace in the denied response.
- Preserve authentication failures as 401 and unrelated server failures as
  failures. Do not redesign authorization, token/login flows, failure reporting,
  or global client error presentation.
- [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md) permits handling
  an exception for an explicit business outcome. The user's selected fix supplies
  that outcome. This does not require changing the ADR or suppressing unrelated
  exceptions. The existing failure-report exclusion already includes this
  exception; no new reporting policy is needed.
- No schema, persistence, or transaction-ownership change is proposed. No storage
  experiment is needed. No API generation is expected for an empty error response
  with unchanged endpoint signatures; use the generation skill if execution
  actually changes an OpenAPI-visible contract type.

## Execution context

- `backend/src/main/java/com/odde/donut/configs/CustomRestExceptionHandler.java`
  contains ordered, specific HTTP exception mappings.
- `backend/src/main/java/com/odde/donut/configs/ControllerSetup.java` catches
  generic exceptions as 500; leave its unrelated behavior intact.
- `NotebookGitBundleControllerTest` already proves owner/subscriber denial at
  the direct controller boundary, but an `assertThrows` does not prove HTTP 403.
- Follow `SoftDeletedTitleConflictMvcTest` / `DisplayNameNormalizationMvcTest`:
  `ControllerTestBase`, `@AutoConfigureMockMvc`, real controllers/advice and DB,
  and concise `makeMe` fixtures. Do not mock authorization or the exception mapper.
- CLI already covers 403 in `notebookClone.failures.test.ts`,
  `notebookPublish.submission.suite.ts`, and
  `notebookPull.acceptedHistory.suite.ts`. Publish may fail at its initial bundle
  read before submission; preserve that valid early denial.

## Ordered slices

### 1. Explain denied notebook access without suggesting a server retry

Type: Behavior
Status: done
Behavior: An authenticated user lacks access to a notebook operation → attempts
the operation → receives HTTP 403 and permission-specific CLI guidance, with no
resource data or publication mutation.
Proof: Real MVC responses and one real installed-CLI denial regression, supported
by existing client status and state-preservation tests.

1. Reproduce red with an MVC test of a non-owner notebook request. Cover ordinary
   notebook GET, Git GET, and Git POST as variants of the same denial contract;
   use a valid accepted bundle/expected head for POST so malformed input does not
   mask authorization. Assert status 403 and empty response body. Retain existing
   owner/subscriber authorization coverage; add a read-only-subscriber Git case
   only if needed to cover dispatch rather than repeat the same policy assertion.
2. Add a focused denial scenario to
   `e2e_test/features/cli/cli_notebook_clone.feature`: prepare an owned notebook,
   run the installed CLI with another seeded user's isolated configuration,
   attempt clone, and observe nonzero exit plus permission guidance and no
   destination. Reuse the installation/notebook fixture and thin CLI page objects;
   do not add another process harness. Keep this E2E `@wip` until green if its
   setup spans the inner reproduction loop.
3. Add the specific shared exception mapping and run the red checks to green.
   On denied publication, assert unchanged accepted binding and note projection
   through the controller fixture. Reuse existing CLI local-ref/file preservation
   proof rather than duplicating it in every status case.
4. Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (all backend unit
   tests, as required by backend rules). Run focused CLI files with
   `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookClone.failures.test.ts tests/notebookPublish.test.ts tests/notebookPull.test.ts`;
   confirm the suite-entry filenames before execution. Run only the affected
   CLI feature via `pnpm cypress run --spec` through Nix, plus existing relevant
   401/500 client cases. An existing green 500 mapping must still report a server
   error, rather than treating every failure as denial.
5. Manually repeat the original non-owner clone/publish checks on isolated local
   data and record the observed message, exit, and unchanged proposal state.
   Do not depend on notebook ID 2 or the old temporary token configuration.

Sizing: roughly 5–8 minutes active work, medium confidence; existing MVC and CLI
fixtures avoid new infrastructure. Required backend and focused installed-CLI
runtimes may exceed five minutes and are the specific external-wait exception.
One denial outcome and one implementation beat; no preceding Structure leaf.
If fixture work exposes a second implementation beat or >10 minutes active work,
refine this plan in place before continuing; do not widen the story.

## Promise ownership and wrap-up

Push recovery: remote `main` advanced to `8d4355487f`, upgrading Cypress from
15.21.1 to 16.0.0. Rebase succeeded and locked dependencies installed. Re-running
the affected CLI feature produced three failures explicitly reporting removed
`cy.exec()` (`/tmp/denial-cypress-rebased.log`); three other scenarios passed.
This invalidated only E2E helper compatibility proof. The affected Git and
filesystem assertions now use Node tasks. The same focused Cypress command passed
six of six scenarios on Cypress 16 (`/tmp/denial-cypress-compat.log`). A fresh
refactor review made no edits, and coordinator formatting passed. Existing backend,
CLI unit, and manual behavior proof remains applicable. No product-scope change.

Completed proof:
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only`: 2,213 tests passed,
  including real MVC read/download/publication 403 and unchanged accepted data.
- `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/notebookClone.failures.test.ts tests/notebookPublish.test.ts tests/notebookPull.test.ts`:
  50 tests passed, including actual 401/403 cases and 500 server guidance.
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`:
  six scenarios passed; denial scenario enabled after green.
- `CURSOR_DEV=true nix develop -c python3 /tmp/denial-manual.py`: isolated
  clone/publish each exited 1 with permission guidance; destination absent;
  proposal HEAD/refs/status/file and accepted bundle/notebook response unchanged.
  Evidence: `/tmp/denial-manual.log`.
- Fresh post-change-refactor completed with no edits; proof reused. Coordinator
  `./scripts/run.sh pnpm format:changed` passed. No endpoint/DTO signatures changed,
  so API generation was unnecessary.

Execution learnings: red MVC dispatch exposed the original generic rethrow.
The publication baseline must be reloaded to compare MySQL timestamp precision;
the CLI alternate token must not replace the owner browser fixture session.
Those fixture defects were corrected before green. Cypress uses `exitCode`.
At roughly ten active minutes, implementation and automated proof were complete;
continuation was limited to the already-launched final manual check, with no new
implementation beat. Required test runtime remained the planned exception.

Execution note: CI observer startup exited before creating a mailbox or process
because Nix could not write `~/.cache/nix/fetcher-cache-v3.sqlite` inside the
sandbox. No observer remains running (startup cell 4 completed); CI coverage is
unavailable and pending CI will be reported as unobserved. Coordinator: root-018;
checkout: `/Users/terryyin/git/doughnut`; repository: `nerds-odd-e/doughnut`, `main`.

| Promise | Evidence owned by slice 1 |
| --- | --- |
| Permission classification reaches actual HTTP clients | MVC status/body across read/download/publish; installed-CLI denial |
| Actionable CLI result | Existing 403 client tests plus real CLI scenario/manual publish |
| Denied operation changes no data or local work | Existing authorization tests, publication binding/projection assertion, existing CLI preservation tests |
| Owner access, 401, and real server errors retain their behavior | Existing backend owner tests and focused CLI 401/500 regressions |
| No generic failure-handling drift | Specific exception mapping only; existing `ControllerSetupTest` stays green |

Execute with `execute-plan` and its per-slice wrap-up: Jidoka, fresh
post-change-refactor agent, conditional API generation, one coordinator-run
`./scripts/run.sh pnpm format:changed`, plan update, commit, push and asynchronous
CI observation. Preserve this plan while active; remove spent planning history
once its behavior and proof are delivered. The independent search plan remains.
