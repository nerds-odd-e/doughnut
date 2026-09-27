# Responses carry no ORM internals and a lean folder trail

## Source

- Story: [SEED-046#story-9](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-9)
- **Identity:** SEED-046#story-9

## Goal and scope

JSON responses never contain Hibernate proxy internals, fixed where web JSON
is configured rather than per endpoint. Each folder-trail entry
(`ancestorFolders`) carries only `id` and `name`. Excluded: the file page's
readable size, the realm's `notebook` object, other entity-bearing
responses, and removing the Jackson 2 mapper used for hand-written
serialization.

## Outside-in proof

| Key example | Proof |
| --- | --- |
| 1 file in `outer/inner`, fresh file page request → clean two-entry trail, no `hibernateLazyInitializer` | slice 1 (no internals), slice 2 (entry keys) — new MockMvc test |
| 2 note realm for a note in a nested folder → same clean trail | slices 1–2, same test via `GET /api/notes/{note}`; the picture upload returns the same `NoteRealmService` build |
| 3 folder with a long readme → trail entry has no `readmeContent`, `createdAt`, `updatedAt` | slice 2, same test (entry has exactly `id`, `name`) |
| 4 note at notebook root → empty trail | slice 2, same test |

The test is `ResponsesCarryNoOrmInternalsMvcTest`: it commits the fixture,
calls `entityManager.clear()`, then requests with `Accept: application/json`,
so nested folders really arrive as lazy proxies. Controller tests that call
methods directly cannot see this.

Commands:

- `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*ResponsesCarryNoOrmInternals*' --tests '*NotebookAttachmentController*' --tests '*NotebookFolderPage*' --tests '*NoteControllerShow*' --tests '*NoteRealm*' --tests '*Recall*'`
- `CURSOR_DEV=true nix develop -c pnpm frontend:test notes/ Breadcrumb`
- `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/folder_organization`

## Slices

### 1. Web JSON never exposes Hibernate proxy internals

Type: Behavior
Status: done
Accepted proof (2026-09-27): `ResponsesCarryNoOrmInternalsMvcTest`
`filePageCarriesTheTrailWithoutProxyInternals` and
`noteRealmCarriesTheTrailWithoutProxyInternals` (`assertCleanTrail`) failed red
on `"hibernateLazyInitializer":{}` and pass; the focused command and the full
backend suite pass. The XML-accept folder page test moved into this class.
Proof: examples 1–2 without `hibernateLazyInitializer` anywhere in the body,
run red first; the existing controller and folder tests stay green.

Behavior: a nested folder loaded lazily → a file page or note page request →
the JSON body has the same values as today and no proxy internals.

Change: add `tools.jackson.datatype:jackson-datatype-hibernate7` (version from
the Spring Boot–managed Jackson 3 BOM, 3.1.5 today; the artifact exists at that
version) and register its `Hibernate7Module` on the web mapper through the
existing `JsonMapperBuilderCustomizer` in `ObjectMapperConfig`, with lazy
loading forced so proxies keep serializing their values. Correct that class's
Javadoc, which says web JSON uses the Jackson 2 `objectMapper()`.

### 2. Folder-trail entries carry only id and name

Type: Behavior
Status: planned
Proof: examples 1–4 asserting each trail entry's keys are exactly `id` and
`name`; regenerated API client; frontend tests for breadcrumbs, folder
pickers, and the sidebar tree; the folder organization E2E feature.

Behavior: a note, folder, file page, or recalled note in a nested folder →
the realm is loaded → `ancestorFolders` lists `{id, name}` outermost first,
and breadcrumbs, folder pickers, trash detection, and recall's folder path
work as before.

Change: `FolderTrailSegments` produces a small trail-segment record for
responses; `RealmNotebookSidebar.ancestorFolders` and
`RecalledNote.ancestorFolders` use it. Regenerate the TypeScript client
(generate-api-client). The frontend stops treating trail entries as `Folder`
rows: `folderChainWithParentIds` / `ancestorsFromChain` already rebuild
parent links from order, so they build from the segment's `id` and `name`.
The CLI reads only segment names.

Sizing: one proof loop, but the generated type change ripples through about
ten frontend files; splitting by layer would leave a failing build. If it
overruns, stop and report the frontend files still failing, not a partial
split.

## Current decisions

- 2026-09-27: web responses are serialized by Spring Boot 4's Jackson 3
  mapper; the fix registers the Jackson 3 Hibernate module there, not per-DTO
  `@JsonIgnoreProperties` or `Hibernate.unproxy` calls.
- The Jackson 2 mapper and its module stay; it serves only hand-written
  serialization (OpenAI handlers, book services, conversations).

## Learnings

- 2026-09-27: the web Hibernate module disables `USE_TRANSIENT_ANNOTATION`, so
  JPA `@Transient` fields that web JSON shows today (for example
  `MemoryTracker.latestTutorFeedbackGrade`, `Answer.matchedNoteId`) stay.
- 2026-09-27: the `GET /api/notes/{note}` body already lists `ancestorFolders`
  and `notebookRealm` twice at the top level (before this story); slice 2's
  key assertions should account for it.

- 2026-09-27: the local test database may fail to start the backend test
  context because migration `V300000342__DefaultNotebookGitBindingAttachmentRepresentationToLfs`
  refuses leftover committed test notebooks that still store files as RAW. If
  it does, recreate the local `doughnut_test` database and run
  `migrateTestDB`; do not change the migration.
