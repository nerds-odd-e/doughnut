# Rename internal naming: doughnut → donut

## Goal

Every internal artifact, identifier, path, config key, command, and doc
reference uses `donut`. Only these stay `doughnut`, because they are the
external URL or the project/brand name, or because they are the literal
identifier of a live external resource that this plan does not migrate:

- The public product/repo/brand name "Doughnut" (README title, GitHub repo
  name `nerds-odd-e/doughnut`, `frontend/index.html` `<title>`).
- The CLI's hardcoded external API URL `https://doughnut.odd-e.com`.
- Production external resource identifiers: prod MySQL database/username
  `doughnut`, GCS bucket `doughnut-book-pdf-carbon-syntax-298809`, GitHub repo
  paths `nerds-odd-e/doughnut` / `nerds-odd-e/doughnut_sandbox`, the Gitpod
  base image `yeongsheng/doughnut-gitpod:...`.
- Everything under `infra/gcp/**` (Salt states/pillars, Packer template,
  deploy scripts) — these name real, currently-deployed GCP resources; a
  rename there requires a coordinated infra migration, out of scope here.

Exactly one place documents this decision with full history: the ADR written
in Slice 1. No other file in the repo may contain negation/history language
("renamed from", "previously", "used to be", "no longer") about this change —
everywhere else states only the current, clean state.

## Key design decisions

- **Scope resolution from user Q&A:** root `package.json` name → `donut`
  (internal identifier, not "the project name"); `infra/gcp/**` fully
  excluded; the Java base package is one dedicated Structure slice; the CLI's
  invoked command name changes to `donut` too.
- **Exclusion boundary is "live external resource identifier," not
  "directory."** `infra/gcp/**` is excluded wholesale *except* where a script
  there references an artifact filename that this rename actually changes
  (e.g. the CLI's release bundle filename) — that reference must still be
  corrected for the deploy to keep working; see Slice 6.
- **Internal artifact descriptions (npm `description` fields, MCP server
  identity string, log messages) rename to "Donut ..."** — they describe the
  internal tool, not the public brand name.
- Slices are grouped by mechanical area rather than force-fit to the usual
  ~5 minute budget: this is a large, low-judgment rename, and `planning.mdc`'s
  "big structural change gets its own slice" exception applies to every
  bulk-rename slice below, matching the already-agreed Java package slice.
  Each slice is still independently stop-safe: it builds, its tests pass, and
  it commits on its own before the next slice starts.
- Every slice after Slice 1 renames unconditionally — no dual-naming, no
  compatibility aliases, no `@deprecated` shims for the old names.

## Slices

### 1. Decision record (Structure)

Status: done

Write `docs/adrs/0005-rename-internal-naming-to-donut-accepted.md` (status
Accepted — the human already decided this in conversation) covering: why
`donut` replaces `doughnut` internally, what stays `doughnut` and why (the
exclusion list above), and that this ADR is the sole place in the repo
allowed to describe the change's history.

No code changes. Verify: `adr-awareness` conventions followed (frontmatter,
`-accepted` filename suffix, next free `NNNN`).

### 2. Workspace & build identity (Structure)

Status: done

- Root `package.json` `name` → `donut`; rename its scripts/env vars that
  spell out `doughnut` (`DOUGHNUT_API_BASE_URL` / `DOUGHNUT_CONFIG_DIR` in the
  `cli` script → `DONUT_*`, consistent with Slice 6).
- `settings.gradle` and `backend/settings.gradle`: `rootProject.name` →
  `'donut'`.
- Rename directories and `package.json` `name` fields: `packages/doughnut-api`
  → `packages/donut-api`, `packages/doughnut-test-fixtures` →
  `packages/donut-test-fixtures`. Update `pnpm-workspace.yaml` paths and every
  importer (~34 + ~199 files) from `doughnut-api` / `doughnut-test-fixtures`
  to `donut-api` / `donut-test-fixtures`.

Verify: `pnpm install`, repo-wide typecheck/build for the touched packages.

### 3. Generated backend API client rename (Structure)

Status: done

- `openapi-ts.config.ts`: output path `packages/generated/doughnut-backend-api`
  → `packages/generated/donut-backend-api`; generator `name: 'DoughnutApi'` →
  `'DonutApi'`.
- Update every alias/path mapping to the new package path (tsconfig paths,
  bundler aliases, package exports).
- Bulk-update the ~535 import references from
  `@generated/doughnut-backend-api` to `@generated/donut-backend-api`.
- Regenerate via the `generate-api-client` skill; confirm
  `assert_generated_type_script_up_to_date.sh` passes.

Verify: frontend, cli, mcp-server typecheck and their test suites.

### 4. Java backend package rename (Structure)

Status: done

Move `backend/src/main/java/com/odde/doughnut` and
`backend/src/test/java/com/odde/doughnut` to `com/odde/donut` (~903 files);
update every package/import statement; update `build.gradle` `mainClass`
references (`com.odde.doughnut.DoughnutApplication` → `com.odde.donut.DonutApplication`,
including the class rename) and the `odd-e.doughnut.task` system property key
→ `odd-e.donut.task`.

Verify: full backend build + test suite green (`./gradlew build`).

### 5. Backend config namespace rename (Structure)

Status: done

Rename the custom Spring config prefix `doughnut:` (e.g.
`doughnut.book-pdf.gcs.bucket`) to `donut:` in `application.yml` /
`application-prod.yml` and its `@ConfigurationProperties` binding class,
including the `com.odde.doughnut: <level>` logging keys (now
`com.odde.donut:` after Slice 4). Leave the actual config *values* that name
live external resources untouched: prod DB name/user `doughnut`, GCS bucket
`doughnut-book-pdf-carbon-syntax-298809`, GitHub repo paths
`nerds-odd-e/doughnut[_sandbox]`.

Verify: backend config-binding tests green.

### 6. CLI internal rename (Structure)

Status: done

- `cli/package.json`: `name` → `donut-cli`, `description` → `Donut CLI`, and
  the invoked command name → `donut`.
- Release bundle filename `doughnut-cli.bundle.mjs` → `donut-cli.bundle.mjs`
  everywhere it's produced or referenced: the CLI's own bundle script, root
  `package.json`, `.github/workflows/cli-release.yml`,
  `backend/src/main/resources/install.sh` / `install.ps1`, and — despite
  `infra/gcp/**` being otherwise excluded —
  `infra/gcp/scripts/upload-cli-binary-to-gcs.sh`, since that script must keep
  matching the artifact filename this rename actually changes.
- Env vars across `cli/src/**` and tests: `DOUGHNUT_CONFIG_DIR`,
  `DOUGHNUT_API_BASE_URL`, `DOUGHNUT_CLI_DISABLE_INPUT_HISTORY`,
  `DOUGHNUT_MINERU_*`, `DOUGHNUT_NO_BROWSER` → `DONUT_*` equivalents. The
  hardcoded value `'https://doughnut.odd-e.com'` in `cli/src/main.ts` stays
  unchanged (external URL).
- Default local config dir `~/.config/doughnut-dev` → `~/.config/donut-dev`.

Verify: `cli` vitest suite green.

### 7. MCP server internal rename (Structure)

Status: done

`mcp-server/package.json` `name` → `donut-mcp-server`, `description` → "...
for donut". `mcp-server/src/server.ts` server identity `name:
'doughnut-mcp-server'` → `'donut-mcp-server'`.

Verify: `mcp-server` test suite green.

### 8. Frontend internal identifiers (Structure)

Status: done

Rename internal-only identifiers (variables, types, component/file names,
test names, comments) containing `doughnut`/`Doughnut` across `frontend/src`
(~169 non-import hits) and `frontend/package.json` `name` →
`donut-frontend`. Do **not** touch `frontend/index.html`'s `<title>Doughnut -
Personal Knowledge Management tool</title>` — that's the kept product name.

Verify: frontend unit test suite + typecheck green; confirm the `<title>` is
unchanged.

### 9. E2E / Cypress internal references (Structure)

Status: done

Update `e2e_test/**` and Cypress config/support files for the renames from
Slices 2, 3, 4, 6 (fixture imports, CLI env var names/binary name, any Java
package path strings). Leave assertions on the kept external URL or the
product-name UI text untouched.

Verify: run the affected spec files (not the full E2E suite).

### 9b. CLI installed binary name rename (Structure)

Status: done

Discovered while wrapping up Slice 8: the plan's own key design decision
("the CLI's invoked command name changes to `donut` too") was never actually
executed — Slice 6 found no npm `bin` field and concluded there was nothing
to rename, missing that the real invoked name is the *local install target
filename* written by `install.sh`/`install.ps1`, independent of the GCS
source object name (which stays `doughnut` — a live external resource, see
Goal). Rename the local install target and every place that names or
displays the running binary:

- `backend/src/main/resources/install.sh` / `install.ps1`: local install
  target `$INSTALL_DIR/doughnut` / `$InstallPrefix\doughnut` → `donut`.
  Leave the GCS `DOWNLOAD_URL`/`$DownloadUrl` (`doughnut-cli-latest/doughnut`)
  unchanged — live external resource.
- `cli/src/cliExit.ts`: error prefix `doughnut: ${message}` → `donut: ...`.
- `cli/src/commands/version.ts`: self-identification string `doughnut
  ${getVersion()}` and its parsing regex → `donut ...`.
- `cli/src/commands/update.ts`: local temp-file prefix `doughnut-update-` and
  the "Updated doughnut from..." log message → `donut`. Leave
  `DOWNLOAD_PATH` (GCS source) unchanged.
- `e2e_test/config/cliE2ePluginTasks.ts`,
  `e2e_test/start/pageObjects/cli/ttyAssertTerminal.ts`,
  `e2e_test/start/pageObjects/cli/execution.ts`: rename the `doughnutPath`
  variable/property/Cypress-alias plumbing to `donutPath`, the installed
  binary path segment (`join(installDir, 'bin', 'doughnut')`) to `'donut'`,
  and the temp-dir prefix `cypress-doughnut-cli-` → `cypress-donut-cli-`.
  Leave GCS-URL references inside log/error messages unchanged.
- `cli/tests/update.test.ts`, `e2e_test/config/cliVersion.ts`,
  `e2e_test/start/pageObjects/recallLearningSessionMethods.ts`, and any other
  test asserting on the self-identification strings above — update to match.
- `frontend/src/pages/HomePage.vue`: "Doughnut CLI" heading → "Donut CLI"
  (describes the internal tool, matching `cli/package.json`'s already-renamed
  description) and the bare `doughnut` command shown in the copy-paste
  install snippet → `donut` (must match what actually works post-rename).
  Leave the `https://doughnut.odd-e.com/install` URLs, the "To Doughnut"
  welcome name, and the "Doughnut will eventually become..." tagline
  unchanged — external URL / kept product brand text.

Verify: `cli` vitest suite green; run the CLI install E2E spec
(`e2e_test/features/cli/cli_install_and_run.feature`, the only non-`@ignore`
CLI spec CI runs) to confirm the renamed local binary installs and runs.

### 10. Scripts & root dev tooling (Structure)

Status: done

- `setup-doughnut-dev.sh` → `setup-donut-dev.sh`; update its content and
  every doc/CI reference to it.
- `.doughnut-pnpm-lock.sha256` → `.donut-pnpm-lock.sha256`; update whatever
  generates/consumes it.
- Sweep `scripts/**` (excluding `infra/gcp/**`, already excluded) for
  remaining internal `doughnut` references in filenames, identifiers, and
  comments.

Verify: re-run the script(s) that consume the renamed lockfile/hash to
confirm they still pass.

### 11. CI workflow internal naming (Structure)

Status: done

Rename internal artifact/job/step names and script paths (updated by earlier
slices) in `.github/workflows/*.yml` and the composite actions under
`.github/*/action.yml`. Leave the literal external repo path
`nerds-odd-e/doughnut` untouched.

Verify: YAML/actionlint check if available; manually confirm every renamed
path/script referenced actually exists post-rename.

### 12. Docs sweep (Structure)

Status: done

Aligned technical refs in `docs/**`, `.cursor/rules/**`, `.cursor/agent-map.md`,
`.agents/**`, and non-history `.planning/**` with slices 2–11 (`com.odde.donut`,
`DonutApplication` / `DonutApp.vue`, `donut-cli.bundle.mjs`, `donut:` config).
Brand, repo, URL, and live-external `doughnut` names left unchanged.

### 13. Final verification sweep (Structure)

Status: done

Repo-wide case-insensitive `doughnut` grep excluding: the Slice 1 ADR,
`infra/gcp/**`, generated/build/log/lockfile artifacts, and the documented
live-external-resource values and brand-name text listed in "Goal" above.
Fix anything else found. Close with one full run of the backend, frontend,
cli, and mcp-server test suites together as the final gate.

Fixed the "known leftovers" plus a further sweep: `cli/src/backendApi/
donutBackendClient.ts` (renamed from `doughnutBackendClient.ts`, plus its
internal identifiers and 9 importers), `cli/src/configDir.ts`'s production
config-dir default (`~/.config/donut`), `logback-spring.xml`'s logger
categories and log filenames, `backend/HELP.md`, `README.md`'s CLI bundle
path, `flake.nix`, `pyproject.toml`, `e2e_test/support/mcp_client.ts`,
`e2e_test/step_definitions/hook.ts`, and `.cursor/rules/mcp-server.mdc`'s
stale frontmatter description. Full backend/frontend/cli/mcp-server test
suites all green as the closing gate.

## Discoveries affecting remaining work

- `@generated/doughnut-backend-api` has ~535 import references — the single
  largest mechanical find/replace outside the Java package move; do it as a
  scripted bulk replace, then let `generate-api-client` regenerate the actual
  files rather than hand-editing generated output.
- `infra/gcp/scripts/upload-cli-binary-to-gcs.sh` is the one file inside the
  otherwise-excluded `infra/gcp/**` that must still change, because it
  references the CLI bundle filename this rename renames (Slice 6).
- Slice 6's env var rename (`DOUGHNUT_API_BASE_URL`) is actually read in
  `packages/donut-api/src/index.ts` (`getApiConfig()`, shared by cli and
  mcp-server), not directly in `cli/src/**` — Slice 6 updated that read too,
  since renaming only `cli/src/**` would have left the CLI's env var override
  inert.
- After Slice 6, `e2e_test/config/cliE2eRepo.ts`, `cliGmailE2eConfig.ts`, and
  `scripts/local-lb.mjs` still reference the pre-rename bundle path
  `cli/dist/doughnut-cli.bundle.mjs` (now producing `donut-cli.bundle.mjs`) —
  CI runs `e2e_test/features/cli/**` on push, so Slice 9 must land (and be
  pushed together with Slice 6, not separately) to avoid a red CI window.
- Many `doughnut`-named identifiers remain inside `cli/src`/`cli/tests` after
  Slice 6 (e.g. `doughnutSdkOptions`, `DoughnutSdkCallOptions`,
  `doughnutBackendClient.ts`, `cliExit.ts`'s `doughnut: ` prefix, "Run
  doughnut login" text) — out of Slice 6's scope by plan text; the Slice 13
  final sweep must catch these.
