# CLI `/export` — Write the active notebook to a local Markdown tree

**Status:** In progress

**Goal:** Inside the notebook context that `/use` establishes, `/export ~/download` writes the
notebook as a Markdown tree under `~/download/<notebook name>/` — the same shape
`/sync --dry-run` reads, so a user can export, edit in Obsidian, and preview a pull.

## Context

The web side already exports a notebook: the `Export` item in `NotebookButtons.vue` fetches
`GET /api/notebooks/{notebook}/export` and saves the zip. The CLI notebook context offers only
`/attach`, `/sync --dry-run`, and `/exit`, and `/sync --dry-run` deliberately writes nothing.
There is no way to get a notebook onto disk from the CLI.

`/export ./a.path` was agreed at the 2026-07-27 refinement session
(`docs/refinement/2026-07-27/QUESTIONS-for-export-team.md`, "Already agreed"). The team that
picked it up built the backend and the web UI; the CLI side was never implemented.

## Existing architecture — the shared part already lives in the backend

```
NotebookZipBuilder.build()  +  NotebookExportFilenames.sanitize()/uniqueFileNames()
        |  path mapping, filename sanitizing, collisions, index.md, doughnut_id frontmatter
NotebookExportService.exportNotebookAsZip() / exportFileName()
        |
GET /api/notebooks/{notebook}/export   (produces: application/zip,
                                        Content-Disposition: attachment; filename="X.zip")
        |-- Web:  fetch + cookie -> blob -> saveAs()            lands as one .zip
        `-- CLI:  fetch + Bearer -> Buffer -> unzipToEntries() -> ??? write files   <- missing
```

What gets exported is already shared, and it is shared in the right place: the backend. Path
mapping, filename sanitizing, collision handling (`Some Note (123).md`), the `index.md`
convention, and the `doughnut_id` frontmatter have exactly one implementation.

This plan adds no new shared abstraction. The CLI only fills in "zip bytes -> filesystem",
which should not be shared with the browser's `saveAs()`.

| Layer | Web | CLI | Action |
|---|---|---|---|
| Content, endpoint | Backend, one implementation | Same | Leave alone |
| Auth | cookie `same-origin` | `Authorization: Bearer` | Each is right for its carrier |
| Error reporting | toast "Failed to export notebook." | 401/403/404/5xx/transport classes | CLI is richer; do not align |
| Unzip | Not needed | `unzipToEntries()` | Reuse |
| Landing | `saveAs(blob, ...)` | **This plan** | |
| Target name | `` `${notebook.name}.zip` `` (bypasses backend sanitizing) | This plan | Bug; fixed in Slice 6 |

## Decisions

1. Output is an expanded Markdown tree, not a zip file.
2. Target is `<given path>/<notebook directory name>/`. This deliberately diverges from the
   cross-team note's "writes to the directory it is given": the extra level keeps several
   notebooks exported into one folder from mixing. Reply to
   `docs/refinement/2026-07-27/` explaining the divergence when this ships.
3. The directory name comes from the `Content-Disposition` `filename`, minus `.zip` — the CLI
   does not mirror the Java sanitizing rules. This inherits the backend's sanitizing,
   collision, and `Untitled` behaviour with no duplication.
4. Files of the same name are overwritten. Other existing files in the target are left alone —
   no mirroring, no deletion.
5. Fix the web filename bug in the same effort, so both paths stand on the backend's rule.

## Reusable parts — do not reimplement

| Purpose | Location |
| --- | --- |
| Download the export zip (401/403/404/5xx user-readable errors) | `downloadNotebookExportZip()` — `cli/src/backendApi/doughnutBackendClient.ts:350` |
| zip -> `Map<path, content>` | `unzipToEntries()` — `cli/src/sync/unzip.ts:43` |
| Notebook stage slash command registry | `notebookStageSlashCommandsFor()` — `cli/src/commands/notebook/notebookStageSlashCommands.ts:20` |
| "parse argument -> run async -> assistant message" stage template | `syncSlashCommandFor()` — `cli/src/commands/notebook/syncSlashCommand.tsx` + `AsyncAssistantFetchStage` |
| Argument-parsing return shape (`{ value } \| { error }`) | `parseSyncArgument()` — `cli/src/sync/syncArgument.ts:16` |
| Real zip bytes for tests | `zipOfNotes()` / `buildZip()` — `cli/tests/zipFixture.ts` |
| Ink stage test template | `cli/tests/useNotebookSlashCommand.test.tsx` + `tempConfigWithToken()` |
| e2e file helpers | `readCliWorkspaceFile`, `writeCliWorkspaceFile`, `listCliWorkspaceFiles` — `e2e_test/config/cliE2ePluginTasks.ts` |

## Execution protocol — Definition of Done and working agreements

Per `docs/teams/definition_of_done.md` and `docs/teams/initial_working_agreement.md`.

Every slice's commit must satisfy the full DoD, because "Production deployment is automatic
upon each successful CI build" — every push deploys. This is not a "push then fix" flow.

```
outer e2e red -> work inward, red-first at each layer -> outer e2e green + cli green + lint clean
  -> commit (do not push yet)
  -> hand the diff and test output to the pair for review (the DoD review gate)
  -> push only after review (push deploys to production)
  -> next slice
```

| DoD item | How this plan satisfies it |
|---|---|
| Changes committed to trunk | Directly on `main`, no branch ("continuously integrate our code on the trunk") |
| Code, tests, docs free of lint errors | `CURSOR_DEV=true nix develop -c pnpm lint:all` |
| English for all code, tests, documentation | Code, tests, `CommandDoc`, and this plan are English |
| Warnings treated as errors | lint and tsc clean, no warnings left |
| All code is fully small tested | Red unit test before each layer ("test-drive all our code") |
| Remove all unused code, regardless of test coverage | Slice 7 deletes `exportNotebookAsZipUnavailable` — required, not optional tidying |
| Eliminate duplicate code, domain language | No new shared abstraction; the one duplication (`contentDispositionFileName`) is justified in Slice 1 |
| Automated end-to-end tested, services integrated | Each slice's Cucumber scenario is green in that slice's commit, never `@ignore`d |
| Exploratory testing | The manual run below is required, not a bonus: once at the end of Slice 4 and once at the end |
| No known defects | Defects found on the way (web filename, stale 404 message, non-ASCII) are each in a slice or an explicitly raised follow-up |
| User manual kept up to date | The CLI has no separate manual; `/help` renders each command's `CommandDoc`, so `/export`'s doc must be right in the first slice that ships |
| CI green in 10 minutes or less | Confirm CI after each push; report if these tests push CI past 10 minutes |
| Reviewed by at least one other developer (pair/mob counts) | The pair reviews the diff and test output before each push |

Commit messages follow `<type>(<scope>): <summary>` — imperative, lowercase, no period.
Scope is mostly `cli`; `frontend` for Slice 6, `docs` for this plan.

If a slice stalls, or the plan turns out not to match reality, stop and report rather than
changing scope.

## Implementation — outside-in

Rhythm for every slice:

1. Write a failing Cucumber scenario at the outermost layer first. Confirm it fails because the
   behaviour is missing, not because a step is undefined.
2. Work inward one layer at a time: page object -> step definitions -> slash command -> landing
   function -> backend client. Write that layer's failing unit test before implementing it, so
   the inner interface grows out of what the outer layer needs instead of being guessed.
3. The slice ends with the outer scenario green and `cli/` green. Never commit a red test.

Edge cases not worth a scenario (`~` expansion, malicious zip paths, plural wording) are
covered by unit tests in Slice 5. Outside-in does not mean one scenario per branch.

### Slice 1 — Walking skeleton: the narrowest path end to end

Write this scenario first, in `e2e_test/features/cli/cli_export.feature`, tagged as
`cli_sync_dry_run.feature` is (`@withCliConfig @interactiveCLI @disableOpenAiService`):

```gherkin
  Background:
    Given I am logged in as an existing user
    And I set the access token for "old_learner" in the interactive CLI

  Scenario: Export a notebook into an empty directory
    Given I have a notebook "Ben Notebook" with notes:
      | Title | Content |
      | less  | Hello   |
    And an empty directory "./ExportTarget"
    And I enter the slash command "/use Ben Notebook" in the interactive CLI
    When I export the notebook into "./ExportTarget"
    Then the directory "./ExportTarget" should hold only:
      | Path                 |
      | Ben Notebook/less.md |
```

`I am logged in...`, `I set the access token...`, `I have a notebook ... with notes:`, and
`I enter the slash command ...` are existing steps.

Then work inward, red-first at each layer:

1. **e2e glue (outermost)** — add a cy task to `e2e_test/config/cliE2ePluginTasks.ts`. The old
   `createCliWorkspace` was removed in `ed1373fa95` along with the trimmed `/sync` scenarios,
   and it seeded initial files, which `/export` does not need:

   ```ts
   /** An empty directory for a command to write into. */
   createCliEmptyDirectory() {
     return mkdtempSync(join(tmpdir(), 'cypress-cli-export-'))
   },
   ```

   New `e2e_test/start/pageObjects/cli/exportDestination.ts`: `emptyDirectory(name)` uses that
   task, `exportNotebook(name)` goes through
   `interactiveCli().enterSlashCommandInInteractiveCli('/export <dir>')`, and
   `directoryShouldHoldOnly(name, paths)` uses the existing `listCliWorkspaceFiles`. Structure
   it like `syncWorkspace.ts`, including the `Map<name, realDir>` that maps a scenario's name
   to a real temporary directory. Register it in `e2e_test/start/pageObjects/cli/index.ts`. New
   `e2e_test/step_definitions/cli_export.ts` is one-line glue only.

   At this point the scenario should fail because the CLI does not know `/export`. Confirm that
   before going deeper.

   Note: `cli_sync_dry_run.feature` is still `@ignore`d, so `cli_export.feature` will be the
   first CLI e2e under `e2e_test/features/cli/` that actually runs.

2. **Slash command layer** — write the "`/export` is registered" case in
   `cli/tests/notebookStageSlashCommands.test.ts` (red), then add
   `cli/src/commands/notebook/exportSlashCommand.tsx` and register it in
   `notebookStageSlashCommandsFor()`, before `leaveNotebookStageSlashCommand`. Structure it like
   `syncSlashCommand.tsx`: `AsyncAssistantFetchStage` with
   `spinnerLabel="Exporting the notebook…"`. The `CommandDoc` must be right here
   (`usage: '/export <destination directory>'`, and a description covering "creates a
   subdirectory named after the notebook" and "files of the same name are overwritten") —
   this slice deploys on push and `/help` is the user manual.

3. **Landing layer** — write the "nested zip lands under `<dest>/<name>/`" case in
   `cli/tests/writeNotebookExport.test.ts` (red), then add
   `cli/src/sync/writeNotebookExport.ts`. Do the minimum here: take bytes and fileName,
   `unzipToEntries`, `mkdirSync` plus `writeFileSync`, return a one-line summary. No safety
   checks, no empty-notebook message, no plural handling — Slice 5's tests drive those out.

4. **Backend client layer (innermost)** — write the `filename="X.zip"` case in
   `cli/tests/contentDispositionFileName.test.ts` (red), then add
   `cli/src/sync/contentDispositionFileName.ts`, then change `downloadNotebookExportZip()` to
   return `{ bytes, fileName }`.

5. **Fix the affected callers** — the `ExportNotebookAsZip` type
   (`cli/src/sync/exportNotebook.ts:11`) and its only existing caller `previewPull()`
   (`cli/src/sync/previewPull.ts:57`, which needs `bytes` only). The fixture in
   `cli/tests/previewPull.test.ts` has to return the new shape.

6. Scenario green, then commit.

Interfaces, as grown by steps 3 and 4:

```ts
// cli/src/backendApi/doughnutBackendClient.ts
export type NotebookExportDownload = {
  readonly bytes: Buffer
  readonly fileName: string   // as the backend named it: "Ben Notebook.zip"
}

// cli/src/sync/writeNotebookExport.ts
export type WriteNotebookExportRequest = {
  readonly notebookId: number
  readonly destinationDirectory: string        // already absolute
  readonly exportNotebookAsZip: ExportNotebookAsZip
  readonly signal?: AbortSignal
}
export function writeNotebookExport(request: WriteNotebookExportRequest): Promise<string>
```

`writeNotebookExport()` is deliberately a plain function with no slash-command dependency —
this is the `exportNotebook(notebook, targetDir)` shape the CLI team asked for as blocking
ask #1, which `docs/plans/2026-07-28-export-notebook-markdown-zip.md:40` records as "Not yet
addressed". `exportNotebookAsZip` is injected, as in `previewPull()`, so tests can feed a
fixture zip.

The directory name is `basename(fileName, '.zip')`; `basename()` also blocks any path
separator that reached the header.

Keeping `contentDispositionFileName` in both the CLI and the frontend is deliberate: there is
no shared package between them (`doughnut-api` is used by cli and mcp-server, not frontend),
and what repeats is parsing a standard HTTP header, not a business rule. The business rule
(sanitizing) still has one home, in the backend.

### Slice 2 — e2e: folder structure lands — DONE

Pinned at the outermost layer: `cli_export.feature` now exports a notebook with
readme (`index.md`), a root note (`intro.md`), and a note in a folder
(`LeSS in Action/team.md`), and asserts the note body via `readCliWorkspaceFile`.

Inward: Slice 1 already used `{ recursive: true }` and path-ordering in
`writeNotebookExport.test.ts`. The only new glue was
`testability.setNotebookReadmeContent` so e2e can seed notebook readme without UI.

### Slice 3 — e2e: what a repeated export means — DONE

Pinned at the outermost layer: `cli_export.feature` gained "Exporting again reflects a changed
note" (export, change the note in Doughnut via the existing `the note ... is changed in
Doughnut to ...` step, export again, assert the file and the tree shape) and "An unrelated file
in the destination survives an export" (seed a file inside the notebook subdirectory before
exporting, assert it and the exported note both survive). New `addExtraDestinationFile` page
object helper (guards against writing into an unregistered destination — see
`exportDestination.ts`), wired through `index.ts` and a new step in `cli_export.ts`.

Inward: `writeNotebookExport.test.ts` gained "overwrites a file of the same name on a repeated
export" and "leaves files it did not write alone" (covers a file inside the notebook
subdirectory and a sibling at the destination root). No production code changed —
`writeNotebookExport.ts` already never clears its destination and only writes the paths in the
zip, so this slice locks in existing behaviour with tests, the same pattern Slice 2 used.

### Slice 4 — e2e: failures are reported, not silently succeeded — DONE

Pinned at the outermost layer: `cli_export.feature` gained "A destination that does not exist
reports a readable error" (`/export ./NoSuchDirectory` where no page-object directory is
registered for that name, so the literal string reaches the CLI unresolved). Confirmed red
first: without a destination-existence check, `writeNotebookExport`'s `mkdirSync(...,
{recursive: true})` silently created the missing tree and the notebook exported successfully.

Inward: `cli/tests/exportDestination.test.ts` covers "rejects a missing argument", "rejects a
blank argument", "resolves an existing directory", "rejects a path that does not exist", and
"rejects a path that is a file" — then `cli/src/sync/exportDestination.ts` was added:

```ts
export type ExportDestination =
  | { readonly directory: string; readonly error?: undefined }
  | { readonly error: string; readonly directory?: undefined }
export function parseExportDestination(argument: string | undefined): ExportDestination
```

- Empty (after trimming) -> `error: 'Usage: /export <destination directory>'`
- Otherwise strip surrounding shell-style quotes (a new shared
  `cli/src/sync/stripSurroundingQuotes.ts`, deduped out of `syncArgument.ts`, which carried its
  own copy — a quoted destination is exactly what a user types for a path with spaces), then
  `resolve(process.cwd(), ...)`, then `error: 'No directory at <resolved>.'` unless that path is
  an existing directory (symlinks included) — same wording `readWorkspace.ts` already uses for
  `/sync`, via a new shared `cli/src/sync/isDirectory.ts` (deduped out of `readWorkspace.ts`,
  which carried its own copy).

Wired into `exportSlashCommand.tsx` by splitting the stage in two (`ExportRunStage` for the
happy path, the parse error otherwise), rather than the `Promise.reject` this section
originally sketched: `c657c674ad` ("fix(cli): report a /sync usage error without a spinner",
merged the day this slice was implemented) had already found that raising a usage error from
inside the async work flashes the spinner label first. That fix's `UsageErrorStage` component
was lifted out of `syncSlashCommand.tsx` into shared `cli/src/commands/UsageErrorStage.tsx`
(not `commands/notebook/`, since it has no notebook dependency) and reused here instead of
duplicating it; a component test in `cli/tests/exportSlashCommand.test.tsx` pins that the
export spinner never renders before the error. Parsing moved into `useMemo` so a re-render
(e.g. a terminal resize) does not re-run `statSync` or flip the stage mid-export. The
`onSettled`/`onAbortWithError` pair each stage split needs is now the shared
`InteractiveSlashCommandSettleProps` type (`interactiveSlashCommand.ts`) rather than a
`SettleProps` alias redefined in both `syncSlashCommand.tsx` and `exportSlashCommand.tsx`.
`exportDoc`'s description now says "into an existing directory".

### Slice 5 — Inner edge cases (unit tests only, no new scenario) — DONE

Not worth covering end to end, but the behaviour has to exist:

- **`~` expansion** (`exportDestination.ts`): the argument comes from the Ink prompt, never a
  shell, so `~/download` is not expanded for us. `~` and `~/...` become `os.homedir()`;
  `~otheruser` returns a clear error rather than silently creating a directory named
  `~otheruser`.
- **Unsafe zip entry paths** (`writeNotebookExport.ts`): absolute paths, `..` segments, and `\`
  throw `The export contained an unsafe path: <path>.`, with nothing written outside the target.
- **Empty notebook**: zero entries returns `Nothing to export: the notebook has no notes.` and
  creates no directory.
- **Summary wording**: the list-then-count style `/sync` uses, with `1 file written.` in the
  singular.

```
Exported to /Users/me/download/Ben Notebook
  index.md
  LeSS in Action/team.md

2 files written.
```

- **Other `contentDispositionFileName` forms**: unquoted, extra parameters, missing header, no
  `filename` parameter -> `undefined`. When the client cannot read a filename it throws
  `The export response did not name a file.` — it does not guess, and does not fall back to an
  unsanitized name, which is exactly the web bug. Cases go in
  `cli/tests/doughnutBackendClient.errors.test.ts`.
- **`/ex` tab-completion ambiguity**: `/export` and `/exit` share a prefix.
  `resolveInteractiveSlashCommand()` (`cli/src/commands/interactiveSlashCommands.ts:32`) tries
  an exact match first, then prefixes by descending literal length, so `/export ~/download` is
  not read as `/exit`. `getSlashTabCompletion()` uses the longest common prefix, so `/ex` stops
  at `/ex` and reports a count of 2 — a change from today's behaviour, so pin it in
  `slashCommandCompletion.test.ts`.
- **`CommandDoc`**: already written in Slice 1; adjust wording here only as behaviour is added.

Each bullet above was driven red-first. The `/ex` tab-completion case needed no production
change: `getSlashTabCompletion()` already stopped at the longest shared prefix, so its test only
pins existing behaviour, the same as Slice 3's pattern.

Writing the "does not guess" test uncovered a real defect, not just missing coverage: the
"no filename" throw inside `downloadNotebookExportZip()` was nested inside `withBackendClient()`,
whose SDK-error classifier reclassifies any plain `Error` without a `.status` into the generic
"Doughnut service is not available…" message — so the specific message never reached the user.
Fixed by moving the `contentDispositionFileName()` parse and its throw to after the
`withBackendClient()` call returns, so it escapes unclassified. The 404 short-circuit inside
`withBackendClient()` likely has the same defect, but its message text is Slice 7's job to fix, not
this slice's, and reclassifying it now would collide with that slice's diff — left as found, so
Slice 7 should check it lands correctly once it edits that string.

### Slice 6 — The web reads `Content-Disposition` too — DONE

`NotebookExportService.exportFileName()` computes `sanitize(name) + ".zip"` and puts it in the
header, but `NotebookButtons.vue:136` uses `` saveAs(blob, `${props.notebook.name}.zip`) `` —
the raw name. So `exportFileName()` is effectively dead for the web path, and the download name
for a notebook with special characters is decided by browser rules (Chrome turns `/` into `_`;
others differ).

- Add a small frontend helper (for example `frontend/src/utils/contentDispositionFileName.ts`)
  and its spec, equivalent to the CLI one (including the unquoted-filename form the CLI's
  version grew in Slice 5 — no reason for the frontend copy to start out narrower).
- **Guard against the non-ASCII hole this inherits.** Spring encodes response headers as
  ISO-8859-1, so a non-ASCII notebook name (a Chinese title, say) does not come back as
  `undefined` from a missing header — it comes back as a *defined but mangled* string. A plain
  `parsed ?? fallback` would happily hand that mangled string to `saveAs`, replacing today's
  correct `notebook.name` with garbage for exactly the notebooks most likely to hit this. Add a
  second small pure helper, `isPrintableAscii(value: string): boolean` (`/^[\x20-\x7E]+$/`), and
  only trust `parsed` when it passes:
  `` const fileName = parsed !== undefined && isPrintableAscii(parsed) ? parsed : `${props.notebook.name}.zip` ``.
  This keeps the win (special characters sanitized the way the backend already computes) without
  regressing the notebooks this plan cannot fix in this slice — the real fix is still the backend
  RFC 5987 change noted below, raised as its own item.
- `NotebookButtons.vue`: use `fileName` from the guard above in `saveAs(blob, fileName)`, so
  behaviour only improves when the header is both present and printable-ASCII.
- `frontend/tests/pages/NotebooksPage.spec.ts:444` currently asserts `"Owned Catalog.zip"`, a
  name with no special characters, which is why the bug is invisible. Have the fetch mock
  return a `Content-Disposition` header, and add: a special-character case proving the header's
  sanitized value is what gets used, and a non-ASCII case proving a mangled header is rejected in
  favor of `notebook.name`.

The backend is not touched; the frontend simply starts using what it already computes.

Implemented as designed: `frontend/src/utils/contentDispositionFileName.ts` (parses quoted and
unquoted forms, mirroring the CLI's) and `frontend/src/utils/isPrintableAscii.ts`
(`/^[\x20-\x7E]+$/`) each got their own red-then-green spec, then `NotebookButtons.vue`'s
`exportNotebook` was wired to use the header only when both parseable and printable-ASCII.

Writing the two new `NotebooksPage.spec.ts` cases (sanitized-name and non-ASCII-fallback)
surfaced a pre-existing test-isolation bug, not a bug in this slice's code: `vi.mock("file-saver",
() => ({ saveAs: vi.fn() }))` creates one `saveAs` mock shared for the whole file, never cleared
between tests, so `vi.mocked(saveAs).mock.calls[0]` — what the original "downloads a zip…" test
and my first draft of the new tests asserted on — is the *first* call `saveAs` ever received
across the file's run, not the most recent one. It only ever looked correct because that original
test happened to be the first (and, before this slice, only) caller of `saveAs` in the file. Fixed
by asserting on `mock.calls.at(-1)` in the two new tests instead of touching the shared mock's
lifecycle (out of scope for this slice); the original assertion was left alone since it is still
correct for its own case (it really is the first call).

### Slice 7 — Clear out leftovers from when the endpoint did not exist — DONE

The CLI never had an export feature — only the HTTP client `/sync` wrote to fetch a notebook's
current state (`downloadNotebookExportZip()`, whose only caller is `syncSlashCommand.tsx:37`).
That code was written before the backend endpoint shipped and left three statements that are no
longer true, and that read as though CLI export had already been built:

1. **Delete dead code** — `exportNotebookAsZipUnavailable`
   (`cli/src/sync/exportNotebook.ts:21`, documented as "The export that is not reachable yet").
   It has zero references, including tests. Keep the `ExportNotebookAsZip` type (Slice 1
   changed its signature). Rewrite the file header comment about another team owning the
   endpoint and reconciling once it exists — that reconciliation is done.
2. **Rewrite the `downloadNotebookExportZip()` doc comment**
   (`cli/src/backendApi/doughnutBackendClient.ts:343-348`). "The endpoint is owned by the team
   building export and is not serving yet" is false: the backend plan is complete and
   `1c215b26ca` proved a real zip downloads end to end. Describe its actual role — the shared
   zip download entry point for `/export` and `/sync --dry-run`.
3. **Fix the 404 message** (same file, `369-372`), which still says
   `Exporting a notebook is not available yet, or the notebook no longer exists in Doughnut.`
   Now that the endpoint serves, a 404 only means the notebook is gone or unreadable, so the
   first clause misleads. No test asserts this string today, so add a failing 404 case to
   `cli/tests/doughnutBackendClient.errors.test.ts` first, then change it.

No new behaviour, but it is what keeps the next reader from misjudging this code. Do it last so
it does not collide with earlier slices in the same files.

Task 1's dead-code deletion was already done, incidentally, by `c3a1bf842f` ("test(cli): unignore
and fix e2e tests for cli sync dry run") before this slice started — its commit message says
"Remove the unused fallback `exportNotebookAsZipUnavailable`." The file header comment it left
behind was still stale, so that part of Task 1 remained. Rewrote both `exportNotebook.ts`'s file
header and `downloadNotebookExportZip()`'s doc comment (Task 2) to describe the shared
`/export` + `/sync --dry-run` role instead of the old "another team owns this, not serving yet"
framing.

Task 3's fix followed Slice 5's exact same discovery: the 404 short-circuit threw a plain `Error`
inside `withBackendClient()`, so its message was reclassified into the generic "service
unreachable" text by the same classifier that swallowed Slice 5's "did not name a file" message
(a plain `Error` has no `.status`). Rather than moving this one outside the wrapped call too, it
throws `{ status: 404, message: '...' }` instead — an object shape the classifier already knows
how to read correctly (the same pattern the adjacent `!res.ok` branch already used), so the fix is
one line smaller than Slice 5's and doesn't need a second escape hatch. New message: "The notebook
no longer exists in Doughnut, or you no longer have read access to it." — pinned red-first in
`cli/tests/doughnutBackendClient.errors.test.ts`.

Not done here, as it belongs to the plan's final wrap-up rather than any one slice: the reply
under `docs/refinement/2026-07-27/` (decision 2) confirming `writeNotebookExport()` satisfies
blocking ask #1 and explaining the notebook-subdirectory divergence.

The e2e feature is never `@ignore`d. `cli_sync_dry_run.feature:1` is `@ignore`d to this day,
its header saying "Enable them in the commit that does" — a commit that never came. Outside-in
exists to avoid repeating that.

## Out of scope

- **Non-ASCII notebook names.** Known hole: `NotebookExportFilenames.sanitize()` does not strip
  non-ASCII, and Spring encodes response headers as ISO-8859-1, so a name like a Chinese title
  is mangled in `Content-Disposition`, affecting both web and CLI. `NotebookExportControllerTest`
  only asserts `containsString("attachment;")` and never checks the filename, which is why this
  went unnoticed. The fix is for the backend to use
  `ContentDisposition.attachment().filename(name, UTF_8)`, emitting RFC 5987
  `filename*=UTF-8''...` — small, and it fixes the web download too. **Raise as its own item.**
- Mirroring or deleting extra files; `--force` or `--dry-run` flags; an option to land the zip.
- `/sync` itself, including its own missing `~` expansion. (The scratch-directory
  divergence noted here is settled: `/sync` unzips the export in memory, and the
  refinement note that specified a scratch directory has been removed.)
- Un-`@ignore`ing `cli_sync_dry_run.feature`. (Done in `c3a1bf842f`.)
- The rest of the portable workspace epic
  (`.planning/notes/2026-07-24-portable-notebook-workspace.md`, `promoted: false`). Its story 1
  mentions internal references becoming Markdown links and usable attachment references, which
  the backend listed as out of scope for v1.
- The backend and the generated API.

## Backend and generated API guard

This plan does not touch the backend: the endpoint exists
(`packages/generated/doughnut-backend-api/api-summary.md:129`), and the CLI fetches zip bytes
directly rather than through the generated SDK, so no generated file changes.

If a backend controller signature or DTO does change, regenerate before committing or CI's
`assert_generated_type_script_up_to_date.sh` fails:

```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
CURSOR_DEV=true nix develop -c ./assert_generated_type_script_up_to_date.sh
```

`open_api_docs.yaml` and `packages/generated/doughnut-backend-api/**` are always regenerated,
never hand-edited (`.cursor/rules/linting_formating.mdc:62`).

## Verification

Each slice starts from its outer scenario and narrows to unit tests:

```bash
# Outermost, first and last thing in every slice
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_export.feature

# CLI, focused
cd cli && CURSOR_DEV=true nix develop -c pnpm vitest run \
  tests/contentDispositionFileName.test.ts tests/exportDestination.test.ts \
  tests/writeNotebookExport.test.ts tests/exportSlashCommand.test.tsx \
  tests/notebookStageSlashCommands.test.ts tests/slashCommandCompletion.test.ts \
  tests/doughnutBackendClient.errors.test.ts tests/previewPull.test.ts

# All of the CLI (previewPull is affected by the Slice 1 signature change)
cd cli && CURSOR_DEV=true nix develop -c pnpm test

# Frontend (Slice 6)
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/NotebooksPage.spec.ts

# Lint and format
CURSOR_DEV=true nix develop -c pnpm lint:all

# Web export e2e regression (Slice 6 touches NotebookButtons.vue)
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/notebooks/*.feature
```

e2e needs `pnpm sut` running; if unsure, run
`CURSOR_DEV=true nix develop -c pnpm sut:healthcheck` first.

Exploratory testing, required by the DoD — actually run it, do not rely on the tests alone:

```bash
cd cli && CURSOR_DEV=true nix develop -c pnpm tsx src/index.ts
# /use <a notebook>
# /export ~/download
# in another terminal: ls -R ~/download/<notebook name>
# change a note, run /export again: same-named files are overwritten, others untouched
# click Export in the web app once, to check the download name (Slice 6 regression)
```

When everything is done, follow the wrap-up in CLAUDE.md: Jidoka, post-change refactor, update
this plan, commit, push. Per decision 2, also add a short reply under
`docs/refinement/2026-07-27/` explaining that `writeNotebookExport()` satisfies blocking ask #1
and why the extra notebook subdirectory diverges from the original agreement.
