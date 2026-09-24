# Show a note's local picture file in Web Donut

## Source

- Identity: SEED-035#story-3.
- Source: [refined story](../../seeds/SEED-035-ai-workspace-supporting-files.md#story-3)
  (refined in `7e65962c0d`; rechecked 2026-09-24 against the later
  private-note-images, file-listing correction and body-embed deliveries —
  scope unchanged, two current-state notes added to the seed).
- Preparation workspace: `/Users/terryyin/git/doughnut/.claude/worktrees/plan-local-image-display`,
  branch `prep/local-image-display`, session-created from `b2ebe3804f`;
  integration checkout `/Users/terryyin/git/doughnut`; publication target
  `origin/main`. Implementation is not authorized by this plan.

## Goal and scope

A note whose frontmatter `image:` is a path relative to the note's own folder
(`force-diagram.png`, `images/force.png`) shows that notebook file wherever the
note image already appears (note page, recall, conversations — all through
`NoteShow` → `ShowImage`), with `image_mask:` unchanged. Only notebook readers
receive the picture, and only PNG, JPEG, GIF and WebP are served inline.
Existing `/attachments/images/...` and `http(s)` values behave as before.

Excluded (story's deferred promises): Markdown body images, SVG inline, paths
starting with `/`, paths leaving the notebook, remote URLs as notebook files,
reference rewriting on rename/move, special missing-file display, new web
uploads (story 4), converting legacy images (story 5).

## Outside-in proof (key examples)

1. `physics/force.md` with `image: force-diagram.png` and file
   `physics/force-diagram.png` → the note page shows the loaded picture
   (E2E); recall and conversations use the same `NoteShow`.
2. `image: images/force.png` → serves `physics/images/force.png`
   (controller test).
3. Owner and Bazaar reader receive it; a non-reader is refused (controller
   tests).
4. `image: /attachments/images/42/x.png` and `https://…` render unchanged
   (frontend test).
5. Editing the note's text in the rich editor keeps `image: force-diagram.png`
   (frontend test).

## Current decisions

- **Address:** `GET /api/notes/{note}/attachment-image?path=<image value>`.
  It is keyed by the private note id, and the authored `image:` value stays a
  portable path ([ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md)).
  Because the value is in the URL, changing `image:` changes the `<img>` source
  with no cache handling. It is an Attachment with image presentation
  ([ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md#notebook--note-structure)),
  not a second image model and not the legacy `image` table.
- **Resolution — one rule:** target portable path = the note's folder prefix
  (`NotebookGitPortablePath.folderPath(note.getFolder())`) + the value, taken
  literally. The attachment of the note's notebook at that portable path is
  served. There is no normalization. `..`, a leading `/`, `./` or a URL can
  never equal a row's path, so each is 404 without its own recognizer. Reuse
  `NotebookGitPortablePath` for the prefix; do not add a second path builder.
  Do not load bytes of non-matching rows: find candidates by notebook and
  filename through a projection, as the listing correction does.
- **Bytes:** `NotebookAttachmentFile.bytes` (raw and LFS; a missing LFS object
  fails loudly as it does for the download).
- **Response:** read authorization `assertReadAuthorization(note)`, the same
  rule as the file download and the legacy image endpoint. The media type is
  taken from the filename extension, case-insensitively: `png`, `jpg`/`jpeg`,
  `gif`, `webp`. Any other extension is refused with 415, including SVG, and
  bytes are never sniffed. `Content-Disposition: inline` and
  `X-Content-Type-Options: nosniff`. No row → 404, so the picture stays
  visibly broken, as today. The file download endpoint is unchanged.
- **Frontend classification:** one small function next to
  `noteImageScalarsFromMarkdown` maps `(noteId, image)` to an `<img>` source.
  A value starting with `/` or having a URL scheme (`^[a-z][a-z0-9+.-]*:`i) is
  kept as-is. Anything else is built with `client.buildUrl` for the new
  endpoint. `NoteShow` passes the result to `ShowImage`.
- **Legacy cleanup unaffected:** `NoteContentMarkdown.leadingFrontmatterImageReference`
  classifies a relative value as `InvalidPathPresent` and skips orphan `image`
  row cleanup. That is safe and unchanged.

## Slices

### 1. A note shows a picture file from its own folder
Type: Behavior
Status: planned

Behavior: raw notebook with note `physics/force` whose content has
`image: force-diagram.png` and real PNG bytes at `physics/force-diagram.png`
→ open the note page → `#note-image img` has loaded (`naturalWidth > 0`)
from the new endpoint. Through the API: `images/force.png` resolves in the
subfolder, and an LFS notebook serves the stored bytes, not the pointer. A
Bazaar reader is served; a non-reader is refused. `drawing.svg` gets 415, a
missing file or `../x.png` gets 404, and the picture response is inline with
`nosniff`. `/attachments/images/…` and `https://…` sources are unchanged, and
a rich-editor body edit keeps the relative `image:` line.

Proof:
- Backend: new controller test (for example
  `NoteAttachmentImageControllerTest`) covering same-folder, subfolder, LFS
  bytes, Bazaar reader allowed, non-reader refused, SVG 415, missing or `../`
  404, and headers.
- Frontend: `NoteShow`/`ShowImage` test for the relative → endpoint source,
  plus legacy and URL passthrough; a `RichMarkdownEditor` test that a body
  edit keeps `image: force-diagram.png`.
- E2E: a scenario "Note shows a picture file from its own folder" in
  `e2e_test/features/note_view/note_frontmatter_image.feature`. Seed real
  bytes from fixture `example.png` by letting `put_notebook_file_for_testability`
  accept base64 content (a new step such as
  `the notebook "…" has the picture "physics/force-diagram.png" from fixture "example.png"`,
  after `uses legacy raw Git attachment storage`). Create the note with the
  existing `I have a note … under notebook … in folder … with content:`. Add a
  page-object check that the header image has loaded; keep the existing
  presence-only `expectHeaderImage`, because its `example.com` scenarios never
  load.
- Regenerate the API client. Commands B, F, G, E.

Sizing: ~10 min, above the ~5 min target, because one endpoint, one frontend
source function and one E2E scenario with its external wait form a single
proof loop. Delivering the endpoint without its read and raster rules would not
be stop-safe. If it overruns, split at the API boundary: first the endpoint
with all its constraints (stop-safe, because nothing references it yet), then
the display with frontend and E2E proof.

## Proof ownership

| Story promise / key example | Slice |
| --- | --- |
| Example 1: same-folder picture on note page (recall/conversations share `NoteShow`) | 1 |
| Example 2: subfolder path | 1 |
| Example 3: notebook read rule, Bazaar reader, non-reader refused | 1 |
| Raster-only inline; SVG refused; download stays forced | 1 |
| Example 4: legacy `/attachments/images/…` and URL values unchanged | 1 |
| Example 5: rich edit keeps the `image:` line | 1 |
| `image_mask:` unchanged | 1 (`ShowImage` mask untouched) |
| Paths with `/`, `..` not served; broken stays broken | 1 (404 by literal resolution) |

Considered and excluded: a separate recall/conversation E2E (same component);
an LFS E2E (the byte reader is already E2E-proven by the file download);
normalizing `./` or `..` (deferred promise).

## Commands

- B: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (linked worktree
  isolates it; see `docs/worktree-backend-tests.md`).
- F: `CURSOR_DEV=true nix develop -c pnpm frontend:test <spec>` plus the
  frontend typecheck required by the `frontend` skill.
- G: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` after controller
  changes.
- E: `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_view/note_frontmatter_image.feature`.

## Learnings

None yet.
