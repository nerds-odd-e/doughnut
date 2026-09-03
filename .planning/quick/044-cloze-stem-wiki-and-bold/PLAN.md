# Cloze stem: wiki paths and bold

Drive both bugs from `ClozeDescriptionTest` (stable algorithm contract). No E2E. Do not use the full いかにも note.

## Design

- **Wiki paths:** Skip pronunciation matches that overlap a `WikiLinkMarkdown.INNER_LINK_PATTERN` span. Title/alias masking still runs on the full string.
- **Bold:** Stop consuming `*`/`_` in `ignoreConjunctions`. Expected stem: `**[...]**`.

## Slices

### 1. Cloze leaves wiki-link path slashes unmasked

Type: Behavior
Status: done

- Pre-condition: note body is exactly `[[漢字/全/全く]]`; title `moon`.
- Trigger: `ClozedString.hide(NoteTitle).maskedContentAsMarkdown()`.
- Post-condition: stem is exactly `[[漢字/全/全く]]`.

### 2. Cloze keeps markdown bold around a masked title

Type: Behavior
Status: planned

- Pre-condition: body `**moon**`; title `moon`.
- Trigger: same cloze API.
- Post-condition: stem is exactly `**[...]**` (not `*[...]*`).
