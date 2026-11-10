# Consumer trace

## Effect terms searched

| Term | Origin |
| --- | --- |
| `scripts/acceptance/cli-publication-update.mjs` | changed path |
| `cli-publication-update.mjs` | basename of `scripts/acceptance/cli-publication-update.mjs` |
| `scripts/acceptance/cliPublicationStore.mjs` | changed path |
| `cliPublicationStore.mjs` | basename of `scripts/acceptance/cliPublicationStore.mjs` |
| `scripts/acceptance/largeNotebookCheckout.mjs` | changed path |
| `largeNotebookCheckout.mjs` | basename of `scripts/acceptance/largeNotebookCheckout.mjs` |
| `installPublicationStore` | supplied |
| `servePublicationStore` | supplied |
| `makeLargeNotebookCheckout` | supplied |
| `cli-publication-update` | supplied |
| `makePublication` | supplied |
| `doughnut-cli-latest/doughnut` | supplied |
| `scripts/acceptance/cli-publication-update` | import path of `scripts/acceptance/cli-publication-update.mjs` |
| `scripts/acceptance/cliPublicationStore` | import path of `scripts/acceptance/cliPublicationStore.mjs` |
| `cliPublicationStore` | import path of `scripts/acceptance/cliPublicationStore.mjs` |
| `scripts/acceptance/largeNotebookCheckout` | import path of `scripts/acceptance/largeNotebookCheckout.mjs` |
| `largeNotebookCheckout` | import path of `scripts/acceptance/largeNotebookCheckout.mjs` |
| `predecessorHash` | declared in `scripts/acceptance/cli-publication-update.mjs` |
| `repositoryRoot` | declared in `scripts/acceptance/cli-publication-update.mjs` |
| `run` | declared in `scripts/acceptance/cli-publication-update.mjs`; in 1075 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `env` | declared in `scripts/acceptance/cli-publication-update.mjs`; in 434 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `cwd` | declared in `scripts/acceptance/cli-publication-update.mjs`; in 117 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `stdio` | declared in `scripts/acceptance/cli-publication-update.mjs`; in 53 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `args` | declared in `scripts/acceptance/cliPublicationStore.mjs`; in 226 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `positional` | declared in `scripts/acceptance/cliPublicationStore.mjs` |
| `result` | declared in `scripts/acceptance/cliPublicationStore.mjs`; in 808 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `publicFile` | declared in `scripts/acceptance/cliPublicationStore.mjs` |
| `uploadEnv` | declared in `scripts/acceptance/cliPublicationStore.mjs` |
| `PATH` | declared in `scripts/acceptance/cliPublicationStore.mjs`; in 1472 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `TRACE` | declared in `scripts/acceptance/cliPublicationStore.mjs`; in 62 files, too broad to list consumers by itself; supply it as a `--term` to list them |
| `GCS_FRONTEND_BUCKET` | declared in `scripts/acceptance/cliPublicationStore.mjs` |

## Scope

Whole checkout at `51f26ed24249`, including uncommitted and untracked files: 4208 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `scripts/acceptance/cli-publication-update.mjs`, `scripts/acceptance/cliPublicationStore.mjs`, `scripts/acceptance/largeNotebookCheckout.mjs`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/dough-execute-plan/scripts/story-obligations-cli.mjs` | `positional` | 60: `const { values, positionals } = parseArgs({` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-dependency-completion.mjs` | `repositoryRoot` | 11: `repositoryRoot,` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-dependency-consumers.mjs` | `repositoryRoot` | 13: `repositoryRoot,` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-git-cherry-pick.mjs` | `repositoryRoot`, `positional` | 65: `repositoryRoot,` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-git-cli.mjs` | `repositoryRoot`, `positional` | 17: `repositoryRoot,` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-git-merge.mjs` | `repositoryRoot` | 41: `repositoryRoot,` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-git-rebase.mjs` | `repositoryRoot` | 52: `repositoryRoot,` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-git-repository.mjs` | `repositoryRoot` | 114: `export function repositoryRoot(cwd) {` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-identity-history.mjs` | `repositoryRoot` | 20: `repositoryRoot,` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.agents/skills/dough-product-backlog/scripts/product-backlog.mjs` | `positional` | 220: `parsed = parseArgs({ args: argv, options, allowPositionals: true });` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.github/workflows/cli-release.yml` | `GCS_FRONTEND_BUCKET` | 10: `GCS_FRONTEND_BUCKET: "dough-frontend-01"` |run: acceptance drives actual workflow version/build/upload commands, application publication shell, shared object bytes, authentic update and executed replacement; setup/assertions mapped below. |
| `.github/workflows/deploy.yml` | `GCS_FRONTEND_BUCKET` | 17: `GCS_FRONTEND_BUCKET: "dough-frontend-01"` |reused: slice11 full108-case application release pass at51f26ed2; original makePublication and workflow contracts remain unchanged. New fixture wraps only its own disposable executable. |
| `backend/src/main/resources/install.ps1` | `doughnut-cli-latest/doughnut` | 4: `$DownloadUrl = "$BaseUrl/doughnut-cli-latest/doughnut"` |excluded: installer/local-LB entry is unchanged; subject is authentic already-installed binary update through its real download URL. No installer or local serving implementation changed. |
| `backend/src/main/resources/install.sh` | `doughnut-cli-latest/doughnut` | 7: `DOWNLOAD_URL="$BASE_URL/doughnut-cli-latest/doughnut"` |excluded: installer/local-LB entry is unchanged; subject is authentic already-installed binary update through its real download URL. No installer or local serving implementation changed. |
| `cli/src/commands/update.ts` | `doughnut-cli-latest/doughnut` | 16: `` const DOWNLOAD_PATH = `${BASE_URL}/doughnut-cli-latest/doughnut` `` |run: acceptance drives actual workflow version/build/upload commands, application publication shell, shared object bytes, authentic update and executed replacement; setup/assertions mapped below. |
| `gradlew` | `positional` | 196: `# changing the positional parameters here affects neither the number of` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `infra/gcp/path-routing/requiredStaticPathsFromFrontend.mjs` | `publicFile` | 19: `function walkPublicFiles(publicDir, base, outPaths) {` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `infra/gcp/scripts/upload-cli-binary-to-gcs.sh` | `doughnut-cli-latest/doughnut`, `GCS_FRONTEND_BUCKET` | 4: `# Uploads the bundled CLI to gs://<bucket>/doughnut-cli-latest/doughnut (prod LB + CDN).` |run: acceptance drives actual workflow version/build/upload commands, application publication shell, shared object bytes, authentic update and executed replacement; setup/assertions mapped below. |
| `infra/gcp/scripts/upload-frontend-static-to-gcs.sh` | `GCS_FRONTEND_BUCKET` | 7: `# Destination bucket: GCS_FRONTEND_BUCKET, or GCS_BUCKET if unset.` |run: acceptance drives actual workflow version/build/upload commands, application publication shell, shared object bytes, authentic update and executed replacement; setup/assertions mapped below. |
| `scripts/ci/application-release-bootstrap.mjs` | `repositoryRoot` | 12: `function applicationTags(repositoryRoot) {` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `scripts/ci/application-release-ci.mjs` | `repositoryRoot` | 86: `export async function queryReleaseCi({ repositoryRoot, ...options }) {` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `scripts/ci/application-release-reconciliation.mjs` | `repositoryRoot` | 69: `async function releaseCiOutcome(repositoryRoot, githubRepository, release) {` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `scripts/ci/application-release-state.mjs` | `repositoryRoot` | 83: `repositoryRoot = process.cwd(),` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `scripts/local-lb.mjs` | `doughnut-cli-latest/doughnut` | 131: `if (pathname !== '/doughnut-cli-latest/doughnut') return false` |excluded: installer/local-LB entry is unchanged; subject is authentic already-installed binary update through its real download URL. No installer or local serving implementation changed. |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `backend/src/test/java/com/odde/donut/controllers/InstallControllerTest.java` | `doughnut-cli-latest/doughnut` | 22: `.andExpect(content().string(containsString("doughnut-cli-latest/doughnut")))` |excluded: installer/local-LB entry is unchanged; subject is authentic already-installed binary update through its real download URL. No installer or local serving implementation changed. |
| `scripts/ci/application-release-entry.test.mjs` | `makePublication` | 8: `makePublication,` |reused: slice11 full108-case application release pass at51f26ed2; original makePublication and workflow contracts remain unchanged. New fixture wraps only its own disposable executable. |
| `scripts/ci/application-release-publication.test.mjs` | `makePublication`, `repositoryRoot` | 9: `makePublication,` |reused: slice11 full108-case application release pass at51f26ed2; original makePublication and workflow contracts remain unchanged. New fixture wraps only its own disposable executable. |
| `scripts/ci/application-release-retry.test.mjs` | `makePublication` | 9: `makePublication,` |reused: slice11 full108-case application release pass at51f26ed2; original makePublication and workflow contracts remain unchanged. New fixture wraps only its own disposable executable. |
| `scripts/test/install.test` | `doughnut-cli-latest/doughnut` | 10: `DOWNLOAD_URL="$BASE_URL/doughnut-cli-latest/doughnut"` |excluded: installer/local-LB entry is unchanged; subject is authentic already-installed binary update through its real download URL. No installer or local serving implementation changed. |
| `scripts/test/upload-cli-binary-to-gcs.sh.test` | `doughnut-cli-latest/doughnut`, `GCS_FRONTEND_BUCKET` | 61: `export GCS_BUCKET=my-bucket GCS_FRONTEND_BUCKET= STORAGE_LOG="$log"` |excluded: unchanged canonical uploader interfaces; new fixture calls real scripts and observes actual shared stored bytes rather than changing this independent retained test boundary. |
| `scripts/test/upload-frontend-static-to-gcs.sh.test` | `GCS_FRONTEND_BUCKET` | 36: `export GCS_FRONTEND_BUCKET="${GCS_FRONTEND_BUCKET:-}"` |excluded: unchanged canonical uploader interfaces; new fixture calls real scripts and observes actual shared stored bytes rather than changing this independent retained test boundary. |

### Test support or stand-in

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `scripts/ci/application-release-publication-fixtures.mjs` | `makePublication`, `repositoryRoot`, `GCS_FRONTEND_BUCKET` | 16: `const repositoryRoot = fileURLToPath(new URL('../../', import.meta.url))` |run: acceptance drives actual workflow version/build/upload commands, application publication shell, shared object bytes, authentic update and executed replacement; setup/assertions mapped below. |

### Journey or feature

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `e2e_test/config/cliE2ePluginTasks.ts` | `doughnut-cli-latest/doughnut` | 101: `` `installCli: donut binary not found at ${donutPath} after install. Check that ${baseUrl}/doughnut-cli-latest/doughnut is served.` `` |excluded: installer/local-LB entry is unchanged; subject is authentic already-installed binary update through its real download URL. No installer or local serving implementation changed. |

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/cli/SKILL.md` | `doughnut-cli-latest/doughnut` | 104: `` - **`@bundleCliE2eInstall`:** Builds `cli/dist/e2e-install-donut-cli.bundle.mjs` before each scenario and removes it after; the local LB (`scripts/local-lb.mjs… `` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.agents/skills/dough-execute-plan/references/runtime-setup.md` | `positional` | 106: `The branch is a required positional argument for the authorized target, never` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `.planning/slice-plans/088-publish-large-notebooks-without-losing-history/consumer-trace-application-release.md` | `makePublication`, `doughnut-cli-latest/doughnut` | 31: `` \| `backend/src/main/resources/install.ps1` \| `doughnut-cli-latest` \| 4: `$DownloadUrl = "$BaseUrl/doughnut-cli-latest/doughnut"` \| excluded: shared installer/u… `` |excluded: retained dated planning/proof provenance; no new fixture or runtime caller. |
| `docs/gcp/conditional-backend-deploy.md` | `GCS_FRONTEND_BUCKET` | 196: `` Do not confuse this object with frontend trees under **`GCS_FRONTEND_BUCKET`**; only the deploy bucket holds the jar deploy record. `` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `docs/gcp/prod_env.md` | `doughnut-cli-latest/doughnut`, `GCS_FRONTEND_BUCKET` | 96: `` `gs://<GCS_FRONTEND_BUCKET>/frontend/<GITHUB_SHA>/` `` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `docs/gcp/prod-frontend-static-lb.md` | `doughnut-cli-latest/doughnut`, `GCS_FRONTEND_BUCKET` | 5: `` **Related:** For a qualifying `vMAJOR.MINOR.PATCH` release with successful applicable main CI, the [deploy workflow](../../.github/workflows/deploy.yml) upload… `` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
| `README.md` | `doughnut-cli-latest/doughnut` | 140: `` Expect the Vue production build under `frontend/dist`. The CLI install URL is served from GCS in prod; locally, `pnpm cli:bundle` produces `cli/dist/donut-cli.… `` |excluded: local generic symbol or documentation match only; new standalone fixture does not alter this implementation or its callers. |
## Acceptance observations

Command: `CURSOR_DEV=true nix develop -c node scripts/acceptance/cli-publication-update.mjs --predecessor /Users/terryyin/.codex/tmp/seed121-incident/authentic-cli-0.5.9.mjs`.
Initial terminal session14768 exited0; one selected scenario passed, no failures/skips,12.076s; log `/tmp/seed121-slice12-acceptance.log`. Removed only unused outer imports afterward. A mechanically unnecessary same-command rerun session59847 also exited0, onepass/0fail/0skip in6.112s; final log `/tmp/seed121-slice12-acceptance-final.log`. This extra run changed no proof boundary or outcome.

Actualhost Territory.local48GiB; prelaunch load19.35/14.23/11.48,812991inactive16KiBpages. Parent confirmed no competing owned heavy proof. Choice one bounded native-Git/build/HTTP fixture rather than backend/E2E stack; no fullCLI/backend run because production sources and reused fixture remain unchanged. Reconsider on measured contention/failure; none observed.

Setup: `makePublication` creates selected immutable synthetic application source and SPA/jar artifacts plus fake external GCP/GCS/curl. `installPublicationStore` wraps only this disposable fixture's gcloud, copies actual CLI and SPA upload bytes into one filesystem object store, then delegates retained application semantics. No shared production fixture changed. `makeLargeNotebookCheckout` creates actual native accepted A with4200valid typed Markdown files and one three-edit child C. Backend HTTP is an external transport fixture; it does not establish backend materialization or production acceptance (accepted slice10 owns that).

| Promise | Actual observation |
| --- | --- |
| J01.1 authentic affected installation | Required predecessor SHA256 checked against5b868908f33576604389ad9b41f26a21eaa6efef982c570e2a60488d3365525c before work; owned installed copy actually executes and reports donut0.5.9. No current-source relabel. |
| C12/J01.6 update target usable after application publication | Real CLI Release workflow tag-version and build commands create0.5.28, actual uploader writes shared public object; actual application publication through same transport stores selected SPA and succeeds. Object digest remains builtDigest; subsequent authentic normal update/download/replacement executes the exact bytes. |
| J01.2 owner runs update | Owned authentic binary executes `update`; stdout Updateddonutfrom0.5.9to0.5.28; installed file digest equals build and executed version reports0.5.28. |
| Fixed replacement is usable | Executed installed replacement runs notebookPublish over independently measured1142400-byte full accepted listing; real HTTP POST capture expectedA and native bundle headC, local mainC/parentA/clean and exact3edited bytes all asserted. |

Observed synthetic ref refs/tags/cli-v0.5.28 (no real tag created), version0.5.28, build/installed/public SHA256aa2dc8c1b66ec10eb959fe3627096cab6cc2084c4212406b1d3d7ba3251b7d4a; A270cd822f4865f01b27a1c08c7bbadac11f0f4e1, Ce358a5abb01e8ce213445efc413cd372616a2e77. Actual public GitHub/GCP delivery and public updater input remain slice13; no platform release or host installation/config mutation is claimed.

All three new fixture files are below250lines. No product code, shared fixture, generation, API, schema or host state changed. Cleanup hooks close actual HTTP server and remove owned disposable fixture. No mapped slice12 observation remains uncovered.

Active implementation/read/proof/trace work approximately four minutes; no failed attempt or outstanding command. Whitespace check passed. No selective formatting, hook-owned lint, refactor, commit, push or done mark performed.
