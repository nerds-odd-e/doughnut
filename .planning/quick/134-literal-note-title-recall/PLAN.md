# Treat the whole note title as one recall value

Status: executed — all slices complete, `pnpm backend:test_only` green
Source: [SEED-022 story 1](../../seeds/SEED-022-reconsider-slash-title-alias-removal.md#story-1),
refined 2026-09-17 from the owner's clarified title semantics. The owner
authorized planning, not execution.

## Goal and scope

A learner sees one consistent interpretation of a note title in spelling
recall: the whole title is the answer and masking value, except that a tilde
marker at the actual beginning of the whole title retains its existing suffix
behavior. Alternative answers and masks come from explicit YAML `aliases:`.

Included:

- make spelling verification treat the whole title as one `TitleFragment`;
- make cloze masking treat a fullwidth slash and any later tilde as literal
  title characters;
- make cloze masking treat a trailing bracket pair as literal title characters
  rather than a separately masked qualifier;
- retain the current leading `~`, `〜`, and `～` suffix behavior when the marker
  is at the beginning of the whole title;
- retain frontmatter aliases as independent spelling answers and masks;
- remove the title-segment and qualifier machinery made obsolete by the rule.

Excluded:

- rewriting existing titles or synthesizing frontmatter aliases for implicit
  fragments that cease to work;
- changes to ASCII `/` Portable-path parsing in wiki links, fullwidth-slash
  filename safety, wiki-link resolution, or Accepted ADR 0004;
- changes to frontmatter alias parsing, validation, normalization, or lookup;
- the completed SEED-021 inbound-reference NFKC fix and the separate concern
  that whole authored wiki tokens can be NFKC-deduplicated before parsing;
- API shape, generated clients, frontend behavior, database schema, or ERD.

Key examples:

- title `word／~logical`: the exact whole title is accepted and masked; `word`
  and `logical` are not accepted or masked as title-derived alternatives;
- title `cat(animal)`: the exact whole title is accepted and masked; `cat` and
  `animal` are not accepted or masked separately;
- title `~logical`: `logical` remains an accepted answer and suffix occurrences
  remain maskable;
- an explicit frontmatter alias remains an accepted answer and mask.

## Existing-solution and architecture assessment

PFE inspection covered the backend recall flow and product-wide title callers:
`Note.matchAnswer`, `Note.createMaskedContentForRecall`, `NoteTitle`,
`TitleFragment`, `RecallTitleSegments`, `ClozedString`, and
`ClozeReplacement`, plus frontend, CLI, and MCP searches for competing recall
title parsers.

| Responsibility | Existing solution and decision |
| --- | --- |
| Recall-title interpretation | `NoteTitle` is already the shared boundary used by answer matching and cloze masking. Change this owner; do not add another parser or move the rule into controllers. |
| Retained initial-tilde behavior | `TitleFragment.from` already recognizes a marker only when it begins the complete string supplied to it. Reuse it once on the whole raw title. |
| Explicit alternatives | `Note.matchAnswer` and `Note.createMaskedContentForRecall` already obtain aliases from `FrontmatterAliases`. Preserve that independent path unchanged. |
| Obsolete implicit grammar | `RecallTitleSegments` and `NoteTitle`'s trailing-bracket parser exist only to create title-derived alternatives. Delete them as their behavior is retired; simplify `ClozeReplacement` by removing qualifier-specific replacement state. |
| Other product surfaces | No frontend, CLI, or MCP implementation independently parses recall title fragments. No cross-surface replacement is needed. |

Accepted [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md)
defines Remember spelling as verifying the note title or alias; the selected
model preserves those as distinct sources. Accepted
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
makes the title column the display-name source of truth and reserves slash
syntax for authored Portable-path wiki links; treating `／` in the stored title
as literal is aligned and does not amend the ADR. No current North Star topic
governs recall-title semantics, so no North Star change is warranted.

## Outside-in proof ownership

| Promise | Owning slice | Observable proof |
| --- | --- | --- |
| Spelling accepts only the whole literal title when slash/later-tilde or trailing brackets are present | 1 | Extend `NoteControllerVerifySpellingTests` through `NoteController.verifySpelling` with exact-title successes and fragment rejections for `word／~logical` and `cat(animal)`. |
| A marker at the actual beginning of the title still supplies suffix answer behavior, and a frontmatter alias remains accepted | 1 | Preserve/add controller cases for `~logical` → `logical` and the existing frontmatter-alias case. |
| Fullwidth slash and a later tilde do not create separate cloze masks, while an actual leading tilde still masks suffix occurrences | 2 | Extend `RecallPromptSpellingStemMaskingControllerTest` through recall-prompt creation so the exact `word／~logical` occurrence is hidden, separate `word`/`logical` text remains visible, and a `~logical` title still masks a suffix occurrence. |
| Trailing parentheses are literal rather than a separately masked qualifier | 3 | Extend `RecallPromptSpellingStemMaskingControllerTest` so an exact `cat(animal)` occurrence is hidden while separate `cat` and `animal` text remains visible. |
| Explicit frontmatter aliases remain masks after title-parser removal | 3 | Existing controller coverage `spellingQuestionMasksFrontmatterAliasesInStem` remains green in the full backend suite. |

No E2E or API-client generation is selected: the behavior is owned and
observable at existing backend controller boundaries, with no HTTP response
shape or frontend interaction change. Algorithm tests may be simplified or
removed where they pin the retired implementation, but they do not replace the
controller proofs above.

## Current decisions

- Apply the initial-tilde marker once to the whole title. A tilde after any
  preceding title character, including `／`, is literal.
- Parentheses and other bracket pairs have no title-specific recall semantics,
  regardless of position.
- Do not migrate stored titles. Losing implicit alternatives is the intended
  behavior; authors can state desired alternatives explicitly in frontmatter.
- Keep the story separate from wiki-token normalization/deduplication because
  that concern has a different owner, observable outcome, and proof boundary.

## Ordered slices

### 1. Spelling verifies one whole title value

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` green, including
the extended `NoteControllerVerifySpellingTests` cases.

Behavior: Given a note titled `word／~logical` or `cat(animal)`, when the learner
verifies spelling, then only the complete title (or an explicit frontmatter
alias) is accepted and the former implicit fragments are rejected. Given a
title beginning `~logical`, `logical` remains accepted. Implement the rule by
matching one `TitleFragment` created from the whole raw title; leave the cloze
path temporarily on its existing parser for slices 2 and 3.

### 2. Fullwidth slash and later tildes stay literal while masking

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` green, including
the new slash/later-tilde and retained-leading-tilde cases in
`RecallPromptSpellingStemMaskingControllerTest`.

Behavior: Given a spelling prompt for `word／~logical`, when its body contains
the exact title and separate occurrences of `word` and `logical`, then the exact
title is masked and the separate words remain visible. Given a title beginning
`~logical`, suffix masking still occurs. Replace slash-segment parsing with one
title fragment and remove `RecallTitleSegments` plus tests that exist only for
its retired grammar; retain the trailing-qualifier path until slice 3.

### 3. Trailing brackets stay literal while masking

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` green, including
the new literal-parentheses case in
`RecallPromptSpellingStemMaskingControllerTest` and retained frontmatter-alias
coverage.

Behavior: Given a spelling prompt for `cat(animal)`, when its body contains the
exact title and separate occurrences of `cat` and `animal`, then the exact title
is masked and the separate words remain visible. Remove `NoteTitle`'s trailing
bracket parser and qualifier API, remove qualifier-specific placeholder and
replacement state from `ClozeReplacement`, and collapse `NoteTitle` to the one
whole-title `TitleFragment` shared by answer matching and cloze masking. Update
or delete algorithm tests that assert the retired qualifier behavior while
preserving leading-tilde, ordinary title, pronunciation, and alias coverage.
