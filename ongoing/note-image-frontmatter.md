# Plan: Note image in frontmatter (remove `note_accessory`)

**Status:** planned  
**Context:** Historical image data already lives in `note.content` YAML (see `V300000187__migrate_image_and_wikidata_to_frontmatter.sql`). `note_accessory` and the accessory API remain only for legacy editing and redundant reads.

## Design decisions

- **Single source of truth:** `image:` and `image_mask:` scalars in leading YAML frontmatter, same line-based shape as `wikidata_id` (backend: `NoteYamlFrontmatterScalars` / `NoteContentMarkdown.splitLeadingFrontmatter`; frontend: `parseNoteContentMarkdown` in `frontend/src/utils/noteContentFrontmatter.ts`).
- **Wire format for `image`:** `/attachments/images/{id}/{filename}` after upload (matches migrated data and existing attachment URLs).
- **Orphan cleanup:** On note content persist, delete `Image` rows (via `note_id`) that are no longer referenced by the **parsed** `image:` scalar in the saved content. Implementation should parse the attachment path to numeric image id(s); edge case: multiple images over time — only keep rows referenced by current frontmatter (typically one).
- **Upload API:** Today upload is only on `NoteAccessoriesDTO` / `updateNoteAccessories`. Plan a dedicated endpoint (e.g. `POST /api/notes/{note}/images` or multipart on an existing note-scoped route) that returns `{ id, name }` (or full path) so the rich editor can set the property without touching `note_accessory`.
- **OpenAPI / SDK:** Regenerate client after backend contract changes (`generate-api-client` skill).

## Key touchpoints (for implementers)

| Area | Files / symbols |
|------|-----------------|
| Display | `NoteShow.vue`, `NoteAccessoryAsync.vue`, `NoteAccessoryDisplay.vue`, `ShowImage.vue` |
| Legacy edit | Removed in Phase 3 (`NoteEditImageForm`, `ImageFormBody`; more-options no longer edits image) |
| Rich editor | `RichMarkdownEditor.vue`, `RichFrontmatterProperties.vue`, `RichFrontmatterEditablePropertyRow.vue`, `noteContentFrontmatter.ts` |
| Backend accessory | `NoteController` (`showNoteAccessory`, `updateNoteAccessories`), `NoteService`, `NoteAccessoriesDTO`, `ValidateNoteImage*`, `NoteAccessory` entity, `Note.getOrInitializeNoteAccessory` / relation |
| Frontmatter algorithms | `NoteContentMarkdown.java`, `NoteYamlFrontmatterScalars.java` (extend or add `image` / `image_mask` helpers for server-side) |
| Image entity | `Image.java` (`note_id`), migrations under `backend/src/main/resources/db/migration/` |
| Tests / mocks | `NoteControllerTests.java`, `NoteBuilder.java`, `TestabilityRestController.java`, frontend `mockShowNoteAccessory` / `updateNoteAccessories` in `frontend/tests/helpers/index.ts` |
| Docs | Regenerate `docs/database-erd.md` after Flyway drops `note_accessory` |

---

## Phases

### Phase 1 — **Behavior:** Show note image from frontmatter (read path)

**Pre-condition:** Note has valid leading frontmatter with `image:` (and optional `image_mask:`).  
**Trigger:** User opens note show or recall surface that should show the header image.  
**Post-condition:** Image renders using values parsed from `note.content`, not from `GET /api/notes/{id}/accessory`.

- Add a small shared helper (frontend) to derive `{ noteImage, imageMask }` from content, aligned with backend scalar rules (quotes, trim, case-insensitive keys).
- Refactor display stack so `ShowImage` is fed from that helper + `noteRealm` content (rename/remove `NoteAccessory*` coupling as appropriate; avoid leaving dead `showNoteAccessory` calls for display-only).
- **Tests:** Extend or add unit tests on the parser; E2E on note show (capability-named feature, e.g. extend an existing `note` / recall feature) asserting visible image when frontmatter contains `image: /attachments/images/...`.
- **Stop-safe:** Notes with migrated frontmatter display correctly even before upload or DB cleanup exist.

### Phase 2 — **Behavior:** Rich mode — `image` property + upload with blocked UI

**Pre-condition:** Editable note in rich mode, `noteId` available (already passed into `RichMarkdownEditor`).  
**Trigger:** User adds or edits the `image` property and chooses a file.  
**Post-condition:** UI shows a clear loading/blocked state until the server responds; frontmatter updates to `image: /attachments/images/{id}/{name}`; optional `image_mask` row still edited as plain text.

- New backend upload endpoint + service using existing `ImageBuilder` / validation patterns from `NoteAccessoriesDTO`.
- Rich UI: special handling for key `image` (file picker + progress), analogous in spirit to Wikidata dialog flow but simpler (no external API).
- **Tests:** Controller test for happy path + validation failure; frontend unit for compose/update; E2E `@wip` → green for upload + path visible in properties or persisted note.
- **Stop-safe:** Users can attach images without the toolbar flow.

### Phase 3 — **Behavior:** Remove legacy “Edit note image” path

**Post-condition:** Toolbar / more-options no longer opens image-only accessory form; no frontend calls to `updateNoteAccessories` for images; `NoteEditImageForm` / `ImageFormBody` removed or stripped to dead-code-free state.

**Done.** Removed more-options image UI and components; dropped `note-accessory-updated` / `reloadKey` reload path (`NoteShow`, `NoteToolbar`); deleted `note_edit_accessories.feature` and related page objects / step; Vitest + Cypress `note_frontmatter_image.feature` passing.

### Phase 4 — **Behavior:** Orphan `Image` cleanup when note content is saved

**Post-condition:** After a successful save of note content, any `Image` with `note_id` equal to that note whose id does **not** appear in the saved frontmatter `image:` path is deleted (DB + blob lifecycle consistent with existing delete behavior).

- Hook in the same code path that persists `Note.content` (single transactional boundary).
- Unit tests: table-driven cases for “no image line”, “new path replaces old id”, “invalid path skips delete”, etc.

### Phase 5 — **Structure:** Remove accessory REST surface and domain wiring

**Post-condition:** No `showNoteAccessory` / `updateNoteAccessories`; OpenAPI + generated TS client updated; `Note` entity has no `NoteAccessory` relation; `NoteService` / controller tests updated; testability endpoints adjusted.

- Remove DTOs/validators used only by accessory updates.
- **Verify:** All unit + targeted E2E green; no references to removed SDK methods.

### Phase 6 — **Structure:** Drop `note_accessory` (and accessory-only columns)

**Done.** Flyway `V300000190__drop_note_accessory.sql`; removed `NoteAccessory` and `NoteAccessoryRepository`; `NoteService.deleteOrphanImagesForPersistedContent` no longer clears accessory FKs; `docs/database-erd.md` regenerated from migrated schema.

- User confirmed **no data migration** — table must be empty or already redundant in production per team assumption; migration is `DROP TABLE` only.
- If any environment still has rows, resolve operationally before deploy (out of scope for code plan).

---

## Phase checklist (when closing each phase)

1. Targeted `pnpm backend:test_only` / `backend:verify` as appropriate; targeted Cypress `--spec` for touched features.  
2. No failing tests; remove `@wip` once green.  
3. Regenerate API client when OpenAPI changes.  
4. Update this doc: mark phase done, note discoveries.

---

## Risks / open questions

- **Concurrency:** Two tabs editing — last write wins; orphan cleanup still correct for final persisted content.  
- **`image_mask` validation:** Today `NoteAccessoriesDTO` has a regex; decide whether rich-mode free text keeps the same validation on save (server-side on full note update vs. property row only).  
- **Testability / fixtures:** Any remaining seeds or builders that assumed `note_accessory` rows should use frontmatter-only setup (resolved before Phase 6 deploy).
