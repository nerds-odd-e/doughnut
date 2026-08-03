# Portable workspace triage (stories 1–6)

**Published:** 2026-08-03
**Author filter:** exclude Terry Yin / Tan Yeong Sheng / `terryyin` variants (HYG-02)
**Oracle:** `.planning/notes/2026-07-24-portable-notebook-workspace.md`
**Consumers:** Phases 8–13 (sole action source)

## Summary

| Story | Capability | Verdict | Consumer phase |
|-------|------------|---------|----------------|
| 1 | pull/export | strengthen | 8 |
| 2 | preview-before-pull | strengthen | 9 |
| 3 | incremental pull | strengthen | 10 |
| 4 | workspace lint | Pending | 11 |
| 5 | push dry-run | Pending | 12 |
| 6 | safe push | Pending | 13 |

## Story 1: Pull a notebook into a usable Markdown workspace

### Verdict

strengthen

**Author basis:** Evidence from LIA participant commits only — XinxinKao, Ben Huang, Joy-kgo, Logan, etta.huang, Eric Yeh (and peers). Excluded from triage basis: Terry Yin, Tan Yeong Sheng, `terryyin` variants (HYG-02). Backend zip shape consumed by `/export` also has participant authorship (Ben Huang, Eric Yeh).

**Bar:** Capability is externally valuable and not WIP (`cli_export.feature` has no `@wip`/`@ignore`), but acceptance is incomplete — notably no stable Doughnut identity in exported frontmatter — so Phase 8 should strengthen rather than keep as-is or remove.

### Acceptance citations

- "Notes and folders reproduce the notebook hierarchy with deterministic paths." — match — `e2e_test/features/cli/cli_export.feature` scenario `Export preserves folder structure and note bodies` (paths `Ben Notebook/LeSS in Action/team.md`, `index.md`); participant: Joy-kgo E2E pin + Ben Huang nested-folder zip.
- "Every exported note has stable Doughnut identity in frontmatter." — gap — `NotebookZipBuilder.noteFileContent` writes author properties verbatim and **no** Doughnut id; Eric Yeh `b03ac76f8a` ("Export a note with the properties it holds, and no id of its own") removed prior `doughnut_id` injection; notes without properties get no frontmatter block.
- "Notebook and folder indexes use `index.md` where needed." — match — `NotebookZipBuilder.writeDirectory` writes `index.md` from notebook/folder readme; E2E expects `Ben Notebook/index.md` when readme present (`cli_export.feature`).
- "Internal references are emitted as usable ordinary Markdown links." — gap — export passes note body through unchanged (`NotebookZipBuilder.noteFileContent`); no participant rewrite of wiki/internal refs to ordinary Markdown links under the export/pull surface.
- "Attachments remain remote but their references remain usable." — gap — zip builder emits only notes + `index.md` (attachments stay remote, good), but no participant work rewrites attachment refs into usable remote URLs; no E2E/scenario pins attachment-reference usability after export.
- "Portable content is separated from local synchronization state, and no credentials or secrets are written into the workspace." — match — exported Markdown under notebook root; sync state in `.doughnut-sync/baseline.json` (`writeNotebookExport` / etta.huang baseline seed); E2E inventory shows baseline beside notes; no token/credential paths written by export.
- "A failed pull reports what happened and does not present a partial workspace as successfully synchronized." — match — missing destination: `cli_export.feature` scenario `A destination that does not exist reports a readable error` ("No directory at…"); unsafe zip entries reject before any write (`cli/tests/writeNotebookExport.test.ts` — "writing neither"); failures surface via `AsyncAssistantFetchStage` `onAbortWithError`.

### Capability entrypoints

| Role | Path / command |
|------|----------------|
| CLI | `/export` (`exportSlashCommandFor`) |
| CLI (pull overlap → stories 2–3) | `/sync` non-dry-run via `applyPull` — shared → Stories 2–3 |
| Module | `cli/src/commands/notebook/exportSlashCommand.tsx` |
| Module | `cli/src/sync/writeNotebookExport.ts` |
| Module | `cli/src/sync/exportNotebook.ts` |
| Module | `cli/src/sync/exportDestination.ts` |
| Module | `cli/src/sync/unzip.ts` |
| Module | `cli/src/sync/contentDispositionFileName.ts` |
| Module | `cli/src/sync/pushBaseline.ts` (baseline seeded at export; shared with push stories) |
| Backend (zip source) | `backend/.../NotebookExportService.java`, `notebookExport/NotebookZipBuilder.java` |
| E2E | `e2e_test/features/cli/cli_export.feature` |
| Unit | `cli/tests/writeNotebookExport.test.ts` |
| Registry | `cli/src/commands/notebook/notebookStageSlashCommands.ts` (registers `/export`) |

### Delete / keep file set

| Path | Action | Shared? |
|------|--------|---------|
| `cli/src/commands/notebook/exportSlashCommand.tsx` | keep | — |
| `cli/src/sync/writeNotebookExport.ts` | strengthen | shared → also Stories 5–6 (baseline seed) |
| `cli/src/sync/exportNotebook.ts` | keep | shared → Stories 2–3 |
| `cli/src/sync/exportDestination.ts` | keep | — |
| `cli/src/sync/unzip.ts` | keep | shared → Stories 2–3 |
| `cli/src/sync/readWorkspace.ts` | keep | shared → Stories 2–3 (and Story 4 lint) |
| `cli/src/sync/contentDispositionFileName.ts` | keep | — |
| `cli/src/sync/pushBaseline.ts` | keep | shared → Stories 5–6 |
| `cli/src/commands/notebook/syncSlashCommand.tsx` | keep (defer action to Stories 2–3) | shared → Stories 2–3 |
| `cli/src/commands/notebook/notebookStageSlashCommands.ts` | keep | shared → Stories 2–3 (+ push) |
| `e2e_test/features/cli/cli_export.feature` | keep | — |
| `cli/tests/writeNotebookExport.test.ts` | keep | — |
| `backend/.../NotebookZipBuilder.java` (identity + link/attachment gaps) | strengthen | shared → Stories 2–3 (zip consumers) |
| `cli/src/sync/applyPull.ts` | keep (defer action to Story 3) | shared → Story 3 |
| `e2e_test/features/cli/cli_sync_pull.feature` | keep (defer action to Story 3) | shared → Story 3 |

### Participant-touched inventory

Whole participant-touched inventory under the Story 1 export/pull surface (author filter applied; D-03 shared tags duplicated under Stories 2–3 where overlapping):

| Path | Participant authors (sample) | Notes |
|------|------------------------------|-------|
| `cli/src/commands/notebook/exportSlashCommand.tsx` | XinxinKao, Ben Huang | `/export` slash command |
| `cli/src/sync/writeNotebookExport.ts` | XinxinKao, etta.huang | unzip → filesystem; baseline seed |
| `cli/src/sync/exportNotebook.ts` | XinxinKao, Logan, etta.huang, Eric Yeh | shared → Stories 2–3 |
| `cli/src/sync/exportDestination.ts` | Ben Huang, Eric Yeh, etta.huang | destination parse / missing-dir reject |
| `cli/src/sync/unzip.ts` | Logan | shared → Stories 2–3 |
| `cli/src/sync/contentDispositionFileName.ts` | XinxinKao, etta.huang | download filename |
| `cli/src/sync/pushBaseline.ts` | etta.huang | shared → Stories 5–6 |
| `cli/src/sync/readWorkspace.ts` | Logan, Ben Huang, Eric Yeh | shared → Stories 2–3 (+ Story 4) |
| `cli/src/sync/applyPull.ts` | Joy-kgo, XinxinKao | shared → Story 3 |
| `cli/src/commands/notebook/syncSlashCommand.tsx` | Ben Huang, Eric Yeh, Joy-kgo, Logan | shared → Stories 2–3 |
| `cli/src/commands/notebook/notebookStageSlashCommands.ts` | (registry) | shared → Stories 2–3 |
| `cli/tests/writeNotebookExport.test.ts` | XinxinKao, Ben Huang, etta.huang | unit coverage |
| `e2e_test/features/cli/cli_export.feature` | Joy-kgo, Ben Huang, etta.huang, XinxinKao | capability E2E |
| `e2e_test/features/cli/cli_sync_pull.feature` | Joy-kgo | shared → Story 3 |
| `backend/.../NotebookExportService.java` | Ben Huang | zip orchestration |
| `backend/.../notebookExport/NotebookZipBuilder.java` | Ben Huang, Eric Yeh | shared → Stories 2–3 (zip shape) |
| `backend/.../notebookExport/NotebookExportFilenames.java` | Ben Huang | deterministic path names |

### WIP / gap signals

| Label | Proof |
|-------|-------|
| wrong acceptance — missing stable Doughnut identity | Oracle bullet "Every exported note has stable Doughnut identity in frontmatter"; Eric Yeh `b03ac76f8a` removed `doughnut_id`; `NotebookZipBuilder.noteFileContent` writes properties-only / no-id frontmatter |
| wrong acceptance — no ordinary Markdown link rewrite | Oracle bullet "Internal references are emitted as usable ordinary Markdown links"; no rewrite in `NotebookZipBuilder` / CLI export path |
| wrong acceptance — attachment refs not proven usable | Oracle bullet "Attachments remain remote but their references remain usable"; zip has no attachment entries and no ref-rewrite tests under export/pull |

**Phase 8 finish sketch (discretion):** restore or introduce a stable identity in exported frontmatter that round-trips with push/lint; cover wiki→ordinary links and attachment remote refs with E2E proofs; keep existing hierarchy / `index.md` / sync-state separation behavior.

## Story 2: Preview changes before updating an existing workspace

### Verdict

strengthen

**Author basis:** Evidence from LIA participant commits only — Ben Huang, Logan, XinxinKao, Eric Yeh, Joy-kgo (and peers on shared sync helpers). Excluded from triage basis: Terry Yin, Tan Yeong Sheng, `terryyin` variants (HYG-02).

**Bar:** `/sync --dry-run` + `previewPull` deliver externally valuable, non-mutating previews with E2E/unit coverage and no `@wip`/`@ignore`, but acceptance is incomplete — reserved/duplicate/invalid-mapping reporting is absent, and the report is content-diff oriented rather than full create/update/move/reject actions — so Phase 9 should strengthen rather than keep as-is or remove.

### Acceptance citations

- "The preview reports exact target paths and actions." — match — `previewPull` emits each differing `.md` path plus a unified note diff and a "N note(s) would change" summary (`cli/src/sync/previewPull.ts`, `diffReport.ts`); E2E `Preview one changed note` asserts `less.md` and the would-change body (`cli_sync_dry_run.feature`); unit `reports a changed note as a diff` / `reports the path of a note in a folder` (`cli/tests/previewPull.test.ts`).
- "Reserved filenames, duplicate paths, and invalid mappings are reported clearly." — gap — no participant code under `previewPull` / `syncArgument` / dry-run E2E reports reserved names, duplicate paths, or invalid path mappings; preview only content-diffs intersecting Markdown paths.
- "Running the preview does not mutate Doughnut, the workspace, or sync metadata." — match — `previewPull` only `readWorkspace` + export zip + in-memory unzip (no `writeFile` / baseline writes); E2E Rule `The preview leaves nothing behind` (`The workspace is not written to`, `The preview adds no files of its own`).

### Capability entrypoints

| Role | Path / command |
|------|----------------|
| CLI | `/sync --dry-run` (`syncSlashCommandFor` → `previewPull`) |
| Module | `cli/src/commands/notebook/syncSlashCommand.tsx` |
| Module | `cli/src/sync/previewPull.ts` |
| Module | `cli/src/sync/syncArgument.ts` (`parseSyncArgument`) |
| Module | `cli/src/sync/diffReport.ts`, `unifiedDiff.ts` |
| Module | `cli/src/sync/readWorkspace.ts`, `exportNotebook.ts`, `unzip.ts` (compare inputs) |
| E2E | `e2e_test/features/cli/cli_sync_dry_run.feature` |
| Unit | `cli/tests/previewPull.test.ts`, `cli/tests/syncArgument.test.ts` |
| Registry | `cli/src/commands/notebook/notebookStageSlashCommands.ts` (registers `/sync`) |

### Delete / keep file set

| Path | Action | Shared? |
|------|--------|---------|
| `cli/src/commands/notebook/syncSlashCommand.tsx` | keep | shared → Stories 1, 3 |
| `cli/src/sync/previewPull.ts` | keep | — |
| `cli/src/sync/syncArgument.ts` | keep | shared → Story 3 |
| `cli/src/sync/diffReport.ts` | keep | — |
| `cli/src/sync/unifiedDiff.ts` | keep | — |
| `cli/src/sync/readWorkspace.ts` | keep | shared → Stories 1, 3 (+ Story 4) |
| `cli/src/sync/exportNotebook.ts` | keep | shared → Stories 1, 3 |
| `cli/src/sync/unzip.ts` | keep | shared → Stories 1, 3 |
| `cli/src/commands/notebook/notebookStageSlashCommands.ts` | keep | shared → Stories 1, 3 |
| `e2e_test/features/cli/cli_sync_dry_run.feature` | keep | — |
| `cli/tests/previewPull.test.ts` | keep | — |
| `cli/tests/syncArgument.test.ts` | keep | shared → Story 3 |

### Participant-touched inventory

Whole participant-touched inventory under the Story 2 preview-before-pull surface (author filter applied; D-03 shared paths duplicated):

| Path | Participant authors (sample) | Notes |
|------|------------------------------|-------|
| `cli/src/commands/notebook/syncSlashCommand.tsx` | Ben Huang, Eric Yeh, Joy-kgo, Logan | shared → Stories 1, 3 |
| `cli/src/sync/previewPull.ts` | Ben Huang, Logan, XinxinKao | dry-run compare |
| `cli/src/sync/syncArgument.ts` | Ben Huang, Eric Yeh, Joy-kgo, Logan | shared → Story 3 |
| `cli/src/sync/diffReport.ts` | Ben Huang | report assembly |
| `cli/src/sync/unifiedDiff.ts` | Eric Yeh, Logan | note diff hunks |
| `cli/src/sync/readWorkspace.ts` | Logan, Ben Huang, Eric Yeh | shared → Stories 1, 3 (+ Story 4) |
| `cli/src/sync/exportNotebook.ts` | XinxinKao, Logan, etta.huang, Eric Yeh | shared → Stories 1, 3 |
| `cli/src/sync/unzip.ts` | Logan | shared → Stories 1, 3 |
| `cli/src/commands/notebook/notebookStageSlashCommands.ts` | (registry) | shared → Stories 1, 3 |
| `e2e_test/features/cli/cli_sync_dry_run.feature` | Eric Yeh, Logan | capability E2E |
| `cli/tests/previewPull.test.ts` | Ben Huang, Eric Yeh, Logan, XinxinKao | unit coverage |
| `cli/tests/syncArgument.test.ts` | Eric Yeh, Joy-kgo, Logan | shared → Story 3 |

### WIP / gap signals

| Label | Proof |
|-------|-------|
| wrong acceptance — no reserved/duplicate/invalid-mapping report | Oracle bullet "Reserved filenames, duplicate paths, and invalid mappings are reported clearly"; `previewPull` filters only content inequality on `.md` paths — no reserved/duplicate/mapping diagnostics in module or `cli_sync_dry_run.feature` |
| wrong acceptance — action taxonomy incomplete vs story intent | Story wants create / update / move / leave unchanged / reject; report is "path + content would change" only — remote-only notes and moves are not previewed as distinct actions (`previewPull` iterates exported `.md` and diffs against workspace map) |

**Phase 9 finish sketch (discretion):** keep non-mutating dry-run; add clear reporting for reserved/duplicate/invalid mappings; expand preview actions beyond content overwrite diffs where the oracle expects create/move/reject.

## Story 3: Pull only remote changes

### Verdict

strengthen

**Author basis:** Evidence from LIA participant commits only — Joy-kgo, XinxinKao, Ben Huang, Logan, Eric Yeh (and peers on shared sync helpers). Excluded from triage basis: Terry Yin, Tan Yeong Sheng, `terryyin` variants (HYG-02).

**Bar:** `/sync` non-dry-run + `applyPull` deliver valuable, non-WIP incremental updates of intersecting local Markdown (E2E + unit + `@perfSync`), but acceptance is incomplete — remote-only create / rename / move are not applied, and pull never updates `.doughnut-sync` sync metadata — so Phase 10 should strengthen rather than keep as-is or remove.

### Acceptance citations

- "Unchanged files retain their content and modification time." — match — `applyPull` only `writeFileSync`s when remote content differs for an existing `.md` path; intersecting equals and local-only files are skipped (`cli/src/sync/applyPull.ts`); E2E `Extra local-only file is untouched` / `No-op when already in sync` (`cli_sync_pull.feature`).
- "New, changed, renamed, and moved remote notes produce the expected local changes." — gap — changed intersecting notes update (`Pull updates one remote change`); **new** remote-only notes are intentionally not created (`No new local file for a remote-only note`; `does not create a file for a remote-only note` unit); rename/move not implemented (path-keyed content overwrite only).
- "Running pull twice with no intervening changes produces no filesystem changes." — match — second pull with equal content hits `remote === localContent` continue and returns `No changes to pull.` (`applyPull` + E2E `No-op when already in sync`).
- "Sync metadata is updated only after a successful operation." — gap — `applyPull` never writes `.doughnut-sync/baseline.json` (baseline is seeded by export / used by push via `pushBaseline.ts`); successful pull updates note files only.
- "A no-change pull creates no irrelevant differences for version-control tools." — match — no writes when content already matches (`No changes to pull.`); E2E asserts file content unchanged on no-op.

### Capability entrypoints

| Role | Path / command |
|------|----------------|
| CLI | `/sync` (non-dry-run) (`syncSlashCommandFor` → `applyPull`) |
| Module | `cli/src/commands/notebook/syncSlashCommand.tsx` |
| Module | `cli/src/sync/applyPull.ts` |
| Module | `cli/src/sync/syncArgument.ts` |
| Module | `cli/src/sync/readWorkspace.ts`, `exportNotebook.ts`, `unzip.ts` |
| E2E | `e2e_test/features/cli/cli_sync_pull.feature` |
| Unit | `cli/tests/applyPull.test.ts`, `cli/tests/syncArgument.test.ts` |
| Registry | `cli/src/commands/notebook/notebookStageSlashCommands.ts` |

### Delete / keep file set

| Path | Action | Shared? |
|------|--------|---------|
| `cli/src/commands/notebook/syncSlashCommand.tsx` | keep | shared → Stories 1, 2 |
| `cli/src/sync/applyPull.ts` | strengthen | shared → Story 1 |
| `cli/src/sync/syncArgument.ts` | keep | shared → Story 2 |
| `cli/src/sync/readWorkspace.ts` | keep | shared → Stories 1, 2 (+ Story 4) |
| `cli/src/sync/exportNotebook.ts` | keep | shared → Stories 1, 2 |
| `cli/src/sync/unzip.ts` | keep | shared → Stories 1, 2 |
| `cli/src/sync/pushBaseline.ts` | keep (metadata gap; push/export owns writes) | shared → Stories 1, 5–6 |
| `cli/src/commands/notebook/notebookStageSlashCommands.ts` | keep | shared → Stories 1, 2 |
| `backend/.../NotebookZipBuilder.java` | strengthen (zip shape) | shared → Stories 1, 2 |
| `e2e_test/features/cli/cli_sync_pull.feature` | keep | shared → Story 1 |
| `cli/tests/applyPull.test.ts` | keep | — |
| `cli/tests/syncArgument.test.ts` | keep | shared → Story 2 |

### Participant-touched inventory

Whole participant-touched inventory under the Story 3 incremental-pull surface (author filter applied; D-03 shared paths duplicated under Stories 1–2):

| Path | Participant authors (sample) | Notes |
|------|------------------------------|-------|
| `cli/src/commands/notebook/syncSlashCommand.tsx` | Ben Huang, Eric Yeh, Joy-kgo, Logan | shared → Stories 1, 2 |
| `cli/src/sync/applyPull.ts` | Joy-kgo, XinxinKao | shared → Story 1 |
| `cli/src/sync/syncArgument.ts` | Ben Huang, Eric Yeh, Joy-kgo, Logan | shared → Story 2 |
| `cli/src/sync/readWorkspace.ts` | Logan, Ben Huang, Eric Yeh | shared → Stories 1, 2 (+ Story 4) |
| `cli/src/sync/exportNotebook.ts` | XinxinKao, Logan, etta.huang, Eric Yeh | shared → Stories 1, 2 |
| `cli/src/sync/unzip.ts` | Logan | shared → Stories 1, 2 |
| `cli/src/sync/pushBaseline.ts` | etta.huang, Ben Huang | shared → Stories 1, 5–6; not written by pull |
| `cli/src/commands/notebook/notebookStageSlashCommands.ts` | (registry) | shared → Stories 1, 2 |
| `backend/.../NotebookZipBuilder.java` | Ben Huang, Eric Yeh | shared → Stories 1, 2 |
| `e2e_test/features/cli/cli_sync_pull.feature` | Joy-kgo | shared → Story 1 |
| `cli/tests/applyPull.test.ts` | Joy-kgo, XinxinKao | unit + perf |
| `cli/tests/syncArgument.test.ts` | Eric Yeh, Joy-kgo, Logan | shared → Story 2 |

### WIP / gap signals

| Label | Proof |
|-------|-------|
| wrong acceptance — no remote create / rename / move | Oracle bullet "New, changed, renamed, and moved remote notes…"; E2E `No new local file for a remote-only note`; `applyPull` only overwrites existing intersecting paths |
| wrong acceptance — sync metadata not updated on pull | Oracle bullet "Sync metadata is updated only after a successful operation"; `applyPull` has no `pushBaseline` / `.doughnut-sync` write |

**Phase 10 finish sketch (discretion):** keep intersecting-path update + no-op safety; add create/rename/move behaviors the oracle expects (or document intentional subset only if product decides — currently strengthen); update sync metadata after successful mutate pulls.
