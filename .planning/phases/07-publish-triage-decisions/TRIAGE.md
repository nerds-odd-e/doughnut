# Portable workspace triage (stories 1–6)

**Published:** 2026-08-03
**Author filter:** exclude Terry Yin / Tan Yeong Sheng / `terryyin` variants (HYG-02)
**Oracle:** `.planning/notes/2026-07-24-portable-notebook-workspace.md`
**Consumers:** Phases 8–13 (sole action source)

## Summary

| Story | Capability | Verdict | Consumer phase |
|-------|------------|---------|----------------|
| 1 | pull/export | strengthen | 8 |
| 2 | preview-before-pull | Pending | 9 |
| 3 | incremental pull | Pending | 10 |
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
| CLI (pull overlap → stories 2–3) | `/sync` non-dry-run via `applyPull` — shared candidate with Story 3 |
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
| `cli/src/sync/exportNotebook.ts` | keep | shared → Stories 2–3 (same zip fetch shape) |
| `cli/src/sync/exportDestination.ts` | keep | — |
| `cli/src/sync/unzip.ts` | keep | shared → Stories 2–3 |
| `cli/src/sync/contentDispositionFileName.ts` | keep | — |
| `cli/src/sync/pushBaseline.ts` | keep | shared → Stories 5–6 |
| `cli/src/commands/notebook/notebookStageSlashCommands.ts` | keep | shared → all notebook-stage stories |
| `e2e_test/features/cli/cli_export.feature` | keep | — |
| `cli/tests/writeNotebookExport.test.ts` | keep | — |
| `backend/.../NotebookZipBuilder.java` (identity + link/attachment gaps) | strengthen | shared → Stories 2–3 (zip consumers) |
| `cli/src/sync/applyPull.ts` | keep (defer action to Story 3) | shared → Story 3 |
| `e2e_test/features/cli/cli_sync_pull.feature` | keep (defer action to Story 3) | shared → Story 3 |

### Participant-touched inventory

Whole participant-touched inventory under the Story 1 export/pull surface (author filter applied; paths listed once here; shared tags note later-story overlap):

| Path | Participant authors (sample) | Notes |
|------|------------------------------|-------|
| `cli/src/commands/notebook/exportSlashCommand.tsx` | XinxinKao, Ben Huang | `/export` slash command |
| `cli/src/sync/writeNotebookExport.ts` | XinxinKao, etta.huang | unzip → filesystem; baseline seed |
| `cli/src/sync/exportNotebook.ts` | XinxinKao, Logan, etta.huang, Eric Yeh | zip fetch type / contract |
| `cli/src/sync/exportDestination.ts` | Ben Huang, Eric Yeh, etta.huang | destination parse / missing-dir reject |
| `cli/src/sync/unzip.ts` | Logan | zip → entries |
| `cli/src/sync/contentDispositionFileName.ts` | XinxinKao, etta.huang | download filename |
| `cli/src/sync/pushBaseline.ts` | etta.huang | shared with push |
| `cli/src/sync/readWorkspace.ts` | Logan, Ben Huang, Eric Yeh | shared with sync pull |
| `cli/src/sync/applyPull.ts` | Joy-kgo, XinxinKao | shared → Story 3 |
| `cli/tests/writeNotebookExport.test.ts` | XinxinKao, Ben Huang, etta.huang | unit coverage |
| `e2e_test/features/cli/cli_export.feature` | Joy-kgo, Ben Huang, etta.huang, XinxinKao | capability E2E |
| `e2e_test/features/cli/cli_sync_pull.feature` | Joy-kgo | shared → Story 3 |
| `backend/.../NotebookExportService.java` | Ben Huang | zip orchestration |
| `backend/.../notebookExport/NotebookZipBuilder.java` | Ben Huang, Eric Yeh | hierarchy, index.md, frontmatter shape |
| `backend/.../notebookExport/NotebookExportFilenames.java` | Ben Huang | deterministic path names |

Likely shared candidates for plan 02 D-03 tagging (not fully duplicated yet): `exportNotebook.ts`, `unzip.ts`, `applyPull.ts`, `readWorkspace.ts`, `cli_sync_pull.feature`, `pushBaseline.ts`, `notebookStageSlashCommands.ts`.

### WIP / gap signals

| Label | Proof |
|-------|-------|
| wrong acceptance — missing stable Doughnut identity | Oracle bullet "Every exported note has stable Doughnut identity in frontmatter"; Eric Yeh `b03ac76f8a` removed `doughnut_id`; `NotebookZipBuilder.noteFileContent` writes properties-only / no-id frontmatter |
| wrong acceptance — no ordinary Markdown link rewrite | Oracle bullet "Internal references are emitted as usable ordinary Markdown links"; no rewrite in `NotebookZipBuilder` / CLI export path |
| wrong acceptance — attachment refs not proven usable | Oracle bullet "Attachments remain remote but their references remain usable"; zip has no attachment entries and no ref-rewrite tests under export/pull |

**Phase 8 finish sketch (discretion):** restore or introduce a stable identity in exported frontmatter that round-trips with push/lint; cover wiki→ordinary links and attachment remote refs with E2E proofs; keep existing hierarchy / `index.md` / sync-state separation behavior.
