# Note realm JSON lists each sidebar field once

## Source

- Story: [SEED-046#story-15](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-15)
- **Identity:** SEED-046#story-15
- Correction of [SEED-046#story-9](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-9)
  ("responses describe the domain, not the persistence layer"); provenance
  `5d384c2665:.planning/slice-plans/010-responses-carry-no-orm-internals/PLAN.md`,
  commits 9365a11c7d (slice 1) and 5d384c2665 (slice 2).

### Current findings (rechecked 2026-09-27 at 5d384c2665)

1. `NoteRealm` JSON repeats `notebookRealm`, `ancestorFolders` and
   `scopedReadmeContent` at the top level. `controllers/dto/NoteRealm.java`
   has `@JsonUnwrapped RealmNotebookSidebar sidebar` and also public delegate
   getters `getNotebookRealm`, `getAncestorFolders`, `getScopedReadmeContent`,
   so the web mapper writes each key twice. Every note-realm response (note
   show, create, update, and `RelationController` moves returning
   `List<NoteRealm>`) carries the whole notebook realm, readme included,
   twice. The web (Jackson 3) mapper has no global non-null inclusion and the
   delegate getter lacks the sidebar field's `@JsonInclude(NON_NULL)`, so
   `"scopedReadmeContent":null` is most likely written when none applies
   (inferred; slice 1's red run confirms). Plan 010 recorded the duplication
   as pre-dating story 7. `FolderRealm` and `NotebookAttachmentRealm` are
   records exposing only `sidebar()` and are not affected.
2. `services/NoteRealmJsonSerializationTest` serializes with
   `new ObjectMapperConfig().objectMapper()`, the Jackson 2 mapper that
   `ObjectMapperConfig`'s Javadoc says web responses do not use, and asserts
   nothing. It gives false confidence. The wire proof is
   `ResponsesCarryNoOrmInternalsMvcTest` (MockMvc, committed fixture,
   `entityManager.clear()`, so lazy proxies are real). Its "live references"
   case is covered at the wire by `e2e_test/features/note_topology/markdown_link.feature`
   (References section shown on the web), so nothing moves.

## Goal and scope

Each of `notebookRealm`, `ancestorFolders` and `scopedReadmeContent` appears at
most once in a note realm body, with unchanged values; `scopedReadmeContent` is
absent when none applies. The misleading Jackson 2 note-realm serialization
test is retired.

Preserved: every note-realm consumer (frontend, CLI, MCP), Java callers of the
`NoteRealm` accessors (services and about a dozen controller tests), and the
generated API client and `open_api_docs.yaml`, unchanged.

Excluded: the folder trail's conversion placement across its four call sites
(cohesion-only, low); the notebook object's shape; folder and file page
realms.

## Outside-in proof

| Key example | Proof |
| --- | --- |
| 1 note in `outer/inner`; `GET /api/notes/{note}` → raw body contains `"notebookRealm"` and `"ancestorFolders"` once each | slice 1, `ResponsesCarryNoOrmInternalsMvcTest` |
| 2 note at notebook root, no scoped title pattern → body has no `"scopedReadmeContent"` key | slice 1, same test (`noteAtNotebookRootHasAnEmptyTrail` or a sibling) |

Assert on the raw response string (count of `"notebookRealm":`,
`"ancestorFolders":`, `"scopedReadmeContent"` occurrences), not through
`jsonPath`: JSON parsers collapse duplicate keys, so a parsed assertion cannot
go red. Run red first.

Commands:

- `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*ResponsesCarryNoOrmInternals*' --tests '*NoteControllerShow*' --tests '*NoteRealm*' --tests '*NotebookGitProposal*' --tests '*RelationController*'`
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`, then
  `git diff --exit-code open_api_docs.yaml packages/generated/` (expected: no
  diff, since the schema already lists each property once).

## Slices

### 1. Note realm JSON lists each sidebar field once

Type: Behavior
Status: planned
Proof: examples 1–2 as raw-body occurrence assertions in
`ResponsesCarryNoOrmInternalsMvcTest`, red first; focused backend command
green; `generateTypeScript` leaves no diff.

Behavior: a note in a nested folder, or at notebook root → `GET
/api/notes/{note}` → the body lists `notebookRealm` and `ancestorFolders`
once with today's values, and `scopedReadmeContent` once when it applies and
not at all otherwise.

Change: give the three keys one JSON source in `NoteRealm`. Preferred: keep the
unwrapped sidebar (it already carries the schema descriptions, `@NotNull` and
`@JsonInclude(NON_NULL)`, shared with the folder and file page realms) and keep
the delegate accessors out of JSON, so Java callers stay unchanged. Delete
`NoteRealmJsonSerializationTest`.

## Current decisions

- 2026-09-27: proof is a raw-body occurrence count at the MockMvc wire; the
  Jackson 2 mapper is not a proof path for web JSON.
- 2026-09-27: `open_api_docs.yaml` and the generated client must not change;
  a diff means the fix changed the published contract and must be revisited,
  not accepted.

## Learnings

- 2026-09-27 (plan 010): the local test database may refuse to start the
  backend test context because migration
  `V300000342__DefaultNotebookGitBindingAttachmentRepresentationToLfs` rejects
  leftover committed test notebooks storing files as RAW. If it does,
  recreate the local `doughnut_test` database and run `migrateTestDB`; do not
  change the migration.
