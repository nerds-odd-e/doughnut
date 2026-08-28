# Complete doughnut → donut naming

## Goal

Product-facing prose, UI copy, protocol identifiers, env vars, and remaining
internal names use `donut` / `Donut`. Only **live external resource
identifiers** stay `doughnut`.

This finishes the rename started by
`.planning/quick/001-rename-doughnut-to-donut` (executed then deleted). That
pass left a keep-list that treated product-facing "Doughnut" as brand, deferred
the `doughnut-note-md` fence, and missed a handful of internal names. The
keep-list now lives in [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md)
Alignment policy (ADR 0005 was removed).

## Keep `doughnut` (do not migrate)

- GitHub repo paths `nerds-odd-e/doughnut` and `nerds-odd-e/doughnut_sandbox`
- Live site `https://doughnut.odd-e.com` (install URLs, failure-report links,
  CLI default API URL)
- GCS install object `/doughnut-cli-latest/doughnut` and bucket
  `doughnut-book-pdf-carbon-syntax-298809`
- **Local and prod MySQL** user `doughnut` and databases `doughnut`,
  `doughnut_test`, `doughnut_development`, `doughnut_e2e_test` (including
  `scripts/sql/init_doughnut_db.sql` and CI `MYSQL_*_DB`)
- Gitpod image `yeongsheng/doughnut-gitpod:…`
- Everything under `infra/gcp/**`, and code that must name those files
  (`scripts/local-lb.mjs` `DoughnutRouting` / `doughnut-routing.json`)
- Clone-directory language that means the GitHub checkout folder (`cd doughnut`,
  `git clone …/doughnut.git`, MCP `"<doughnut root folder>"`)
- Retired protocol filename `.doughnut-sync` in ADR 0002 / PROJECT.md
- Gitter `Odd-e-doughnut` badge URLs
- `docs/gcp/**` quotations of live GCP resource names

## Rename / remove (in scope)

- Product-facing "Doughnut" → "Donut": README title and blurb, CLAUDE/AGENTS
  opening, homepage “To Doughnut” / tagline, menu “Doughnut by”, bazaar
  “doughnut users”, `frontend/index.html` `<title>`, ADRs 0000–0004, protocol
  docs, E2E Gherkin, CLI user-visible strings, Java comments, agent-skill
  blurbs
- Fence language `doughnut-note-md` → `donut-note-md`
- Env var `DOUGHNUT_API_AUTH_TOKEN` → `DONUT_API_AUTH_TOKEN` (no alias)
- README install snippet command `doughnut` → `donut` (HomePage already `donut`)
- HTTP User-Agent product token `Doughnut/1.0` → `Donut/1.0` (repo URL in the
  same header stays)
- CI **display** names in `mig_status_check.yml` (keep `MIG_NAME=doughnut-app-group`)
- Dead: `DOUGHNUT_SPA_PUBLIC_BASE_URL` (planning-only, unused in code);
  `biome.json` `doughnut_mobile` ignore (no such tree); `.gitignore`
  `infra/salt/.../doughnut_env.sh` (path moved under `infra/gcp/`)
- `backend/HELP.md` Initializr history sentence (delete it; do not move it elsewhere)

## Key design decisions

- **No ADR 0005.** Human instruction after this plan was drafted: delete the
  rename ADR; put the current keep-list in accepted ADRs (ADR 0001 Alignment
  policy); do not keep rename history in ADRs. Do not restore ADR 0005.
- **Product name in prose/UI is Donut.** The GitHub repository and live
  endpoints remain `doughnut`. That split is the new boundary — not "public
  brand stays Doughnut."
- **No dual-naming, no env-var aliases.** `DOUGHNUT_API_AUTH_TOKEN` is a
  cutover, not a deprecated alias. MCP config samples must show only
  `DONUT_API_AUTH_TOKEN`.
- **Dead `DOUGHNUT_*` is deleted, not renamed.** `DOUGHNUT_SPA_PUBLIC_BASE_URL`
  never existed in runtime code.
- Bulk-rename slices may exceed the ~5 minute fuzzy budget (same exception as
  the original rename). Each slice is still independently stop-safe.

## Learnings

- `ec3507e38f` already deleted ADR 0005 and rewrote ADRs 0000–0004 / README
  index to current Donut prose. Slice 1 is done by that commit. Slice 8 still
  needs `docs/commissioned-learning-session-protocol.md`.

## Slices

### 1. Amend ADR 0005 keep-list (Structure)

Status: done

Superseded before execution: ADR 0005 deleted; keep-list is ADR 0001 Alignment
policy. Do not restore the file.

### 2. Web UI product copy (Behavior)

Status: done

Homepage fallback + tagline, sidebar “Donut by”, bazaar “donut users”, tab
title. Install URLs stay `https://doughnut.odd-e.com`.

### 3. README product copy and CLI command (Behavior)

Status: planned

- **Pre-condition:** README title is `# Doughnut`, About/CLI headings say
  Doughnut, install snippet runs `doughnut`.
- **Trigger:** read README.
- **Post-condition:** title/headings/blurb say **Donut**; run command is
  `donut`. GitHub badge URLs, Gitter, backlog `doughnut.odd-e.com`, clone
  `cd doughnut`, and `mysql -u doughnut` stay.

No dual “Doughnut CLI” heading. Match HomePage (`Donut CLI` + `donut`).

Verify: grep README for leftover product-prose `Doughnut` that is not a URL,
repo path, or DB user.

### 4. CLI user-visible copy (Behavior)

Status: planned

- **Pre-condition:** unavailable-service message, access-token slash-command
  description, and Gmail demo subject use “Doughnut”.
- **Trigger:** run CLI / those tests.
- **Post-condition:** they say **Donut**. Download path
  `/doughnut-cli-latest/doughnut` and `https://doughnut.odd-e.com` stay.

Files: `donutBackendClient.ts`, `setAccessTokenSlashCommand.ts`,
`interactiveInkSession.ts` comment, `cli_gmail.feature` +
`InteractiveCliApp.lastEmail.test.tsx`.

Tests: existing `recallMcqInteractive.contest.test.tsx` (“Donut service is not
available”) and last-email test. No new test class.

Verify: `pnpm -C cli test` for those files (or the cli vitest suite if cheaper).

### 5. Fence language `donut-note-md` (Behavior)

Status: planned

- **Pre-condition:** focus-context / Learning Session Request markdown fences
  notes with `doughnut-note-md`.
- **Trigger:** render focus context or a Learning Session Request.
- **Post-condition:** fence is `donut-note-md`. Parser, tests, and design
  examples match.

Files: `FocusContextMarkdownRenderer.java` + its test,
`LearningSessionRequestTests.java`,
`e2e_test/start/pageObjects/recallLearningSessionMethods.ts`
(`donutNoteBodiesIn`), `docs/focus-context/focus_context_retrieval_design.md`,
any `.planning` note that quotes the fence.

Verify: backend tests for the renderer and Learning Session Request; no
`doughnut-note-md` left in the repo.

### 6. Auth env var `DONUT_API_AUTH_TOKEN` (Behavior)

Status: planned

- **Pre-condition:** MCP/CLI auth token is read from `DOUGHNUT_API_AUTH_TOKEN`.
- **Trigger:** start MCP client / `getApiConfig()`.
- **Post-condition:** only `DONUT_API_AUTH_TOKEN` is read. Samples in
  `.cursor/rules/mcp-server.mdc` match. Planning docs that listed the old name
  are updated in this slice (they document the live env var).

Files: `packages/donut-api/src/index.ts`, `e2e_test/support/mcp_client.ts`,
mcp-server rule samples, `.planning/codebase/STACK.md` + `INTEGRATIONS.md` +
`SEED-002` token mentions.

No `DOUGHNUT_API_AUTH_TOKEN` alias. Do **not** invent
`DONUT_SPA_PUBLIC_BASE_URL` — delete the dead SPA env var in slice 9.

Verify: MCP E2E that spawns the client
(`e2e_test/features/mcp/mcp_services.feature`) still authenticates.

### 7. E2E Gherkin product language (Structure)

Status: planned

Rename Gherkin/step text: “Donut Access Token”, “Donut MCP service”, “Donut
focus context”, “changed in Donut” / “in Donut should still hold”, bazaar and
MCP feature blurbs. Step-definition strings must match features.

Settings UI already says “Access Tokens” without a product prefix — this slice
is test vocabulary only. Existing scenarios must keep passing.

Verify: targeted `pnpm cypress run --spec` for
`e2e_test/features/users/user_access_token.feature`,
`e2e_test/features/mcp/mcp_services.feature`,
`e2e_test/features/messages/conversation_about_a_note.feature`,
`e2e_test/features/cli/cli_access_token.feature` (if not `@ignore`).

### 8. ADRs 0000–0004 and protocol docs (Structure)

Status: planned

ADRs 0000–0004 and the README index title are already current Donut prose
(`ec3507e38f`). Remaining: product-prose “Doughnut” → “Donut” in
`docs/commissioned-learning-session-protocol.md`.

Keep `.doughnut-sync`, GitHub paths, and any live URLs. No “renamed from”
language.

Verify: grep that protocol file for product-prose `Doughnut`.

### 9. Remaining prose, dead artifacts, final grep (Structure)

Status: planned

Sweep leftover **internal / product-prose** `doughnut`/`Doughnut` outside the
keep-list:

- `CLAUDE.md` / `AGENTS.md` opening blurb; `.planning/PROJECT.md`; agent
  skills (“doughnut web app” / “doughnut dev environment”); Java comments
  (`ClozedString`, `NoteContentTitleHeading`); `backend/HELP.md` (title Donut
  Backend; **delete** the Initializr history note); `backend/build.gradle`
  task description; `.github/workflows/mig_status_check.yml` display names
  (keep `MIG_NAME=doughnut-app-group`); User-Agent `Donut/1.0`
- Docs that say “doughnut development environment” as project prose (`docs/nix.md`,
  `docs/wsl2.md`, `docs/cursor.md`, `docs/cloud_agent_backend_testing.md`, …)
  while leaving clone paths and DB names
- Dead: drop `DOUGHNUT_SPA_PUBLIC_BASE_URL` from codebase maps; drop
  `biome.json` `doughnut_mobile` ignore; drop stale `.gitignore`
  `infra/salt/.../doughnut_env.sh` exception

Repo-wide case-insensitive `doughnut` grep excluding the keep-list in Goal.
Anything else found is a bug in this slice.

Verify: that grep is empty of in-scope hits; targeted tests for any code
file this slice edits (User-Agent has no dedicated test — do not add one
unless an existing HTTP-client test already asserts the header).
