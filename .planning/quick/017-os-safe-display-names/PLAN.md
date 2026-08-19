# Plan: OS-safe display names are the portable files

**Status:** in progress

**Goal:** Portable filename = Doughnut display name. Unique sibling names that are legal on mainstream OS filesystems need no ZIP collision suffixes and no sanitize-driven `title:` wrap.

**Feeds:** Proposed [ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown.md) (stays Proposed; do not accept). Tracker: [OKF-COMPATIBILITY-GAP.md](../../research/OKF-COMPATIBILITY-GAP.md). Seed: [SEED-003](../../seeds/SEED-003-close-okf-v0-2-compatibility-gaps.md).

## Locked for this plan

- **Filename = name.** Sibling uniqueness already implies unique files. Do not treat `Recipe (2).md` as identity, and do not invert collision suffixes in link resolution.
- **Forbidden characters** (create/rename reject): `\ / : * ? " < > |` and ASCII controls (`U+0000`–`U+001F`). Slice 2 done.
- **Historical rows:** convert those characters to fullwidth (same idea as `:` → `：`): `*` → `＊`, `?` → `？`, `"` → `＂`, `<` → `＜`, `>` → `＞`, `|` → `｜`; controls → space then trim. Fail loud on unique-key collision or empty result. Inbound wiki / path-Markdown tokens must follow (same as a user rename).
- **Out of scope:** path qualification; P9 / import / Git accept (T1); Windows device names (`CON`, …); trailing dots; accepting ADR 0004 as a whole; converting stored `[[…]]` ↔ `[…](…)`.

## Slices

### 1. Lock filename = name in ADR 0004

- **Type:** Structure
- **Status:** done

ADR 0004 Decision is filename = display name (portable path = folder path + display name). Collision suffixes and sanitize-driven `title:` wrap are out of the profile. Tracker/SEED-003 remaining codec work is **P9** only. Status stays **Proposed**. ZIP still sanitizes/suffixes in code until slice 4. ADR 0002 Cons no longer claims `title:` wrap when paths cannot round-trip.

### 2. Reject remaining OS-invalid characters on save

- **Type:** Behavior
- **Status:** done

Create/rename rejects the locked set; message is `Name must not contain \ / : * ? " < > | or ASCII control characters.` Surrounding whitespace/controls still trim then blank-reject. Extract uses `replaceOsInvalidChars` (new chars → fullwidth; controls → space then trim) so it does not hit the reject. E2E: `note_creation.feature` (`Recipe*`).
### 3. Migrate existing illegal display names

- **Type:** Behavior
- **Status:** planned

**Pre-condition:** a live note/folder/notebook still has a newly forbidden character (e.g. title `Recipe*` beside sibling `Recipe`).

**Trigger:** production backfill (next Flyway Java migration after `V300000278`, same three-table pattern as `DisplayNameSurroundingWhitespaceBackfill`).

**Post-condition:** stored names are OS-safe and still unique in their directory; inbound links that used the old spelling resolve to the same targets. Fail loud if uniquify would collide or the name would be blank.

Do not invent `(2)` titles. Fullwidth keeps `Recipe` and `Recipe*` distinct (`Recipe＊`).

### 4. Export the exact display name

- **Type:** Behavior
- **Status:** planned

**Pre-condition:** names are OS-safe and unique (slices 2–3).

**Trigger:** catalog ZIP download.

**Post-condition:** entry path is the folder trail plus `{title}.md` (folder dirs = folder names). No `(2)` suffix, no Untitled fallback, no sanitize-driven `title:` wrap. Author `title:` still copied as stored.

Replace `notebook_export.feature` collision scenario and `NotebookZipBuilder` / `NotebookExportFilenames` suffix tests. Delete collision suffixing and sanitize-for-export once unused. Notebook download name is `{notebook name}.zip`.

After this slice: close collision wording in the gap tracker and SEED-003; remaining codec work is **P9** (and T1). Drop this PLAN directory in wrap-up when the plan is fully executed.

## Not this plan

- Path-shaped vs unqualified wiki identity
- Accept/lint of foreign trees
- Emitting OKF `index.md` / `log.md` / `okf_version`
