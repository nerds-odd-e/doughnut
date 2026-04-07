# Lucide icons migration (Vue: `lucide-vue-next`)

Informal plan; update as work proceeds; remove when done.

## Goal

- Add **Lucide for Vue** via the official package **`lucide-vue-next`** (npm; tree-shakable icon components). Use those components for **standard UI icons** that currently live as `frontend/src/components/svgs/Svg*.vue` (excluding **`link_types/`** entirely: no changes to that folder or its consumers).
- **Delete or stop registering** Svg components that are fully replaced, so one concept has one representation (Lucide) where it fits.
- **Keep** existing Vue SVGs where there is no good Lucide equivalent (branding, flags, composite recall/assessment art, etc.).

## Out of scope

- **`frontend/src/components/svgs/link_types/`** — all files; **`SvgRelationTypeIcon.vue`** stays as the dispatcher (it only composes `link_types/` icons).
- **`frontend/src/components/svgs/flags/`** — country flags stay as custom SVGs.

## Dependency

- Add **`lucide-vue-next`** to the **`frontend`** workspace package (pin a current version; follow [Lucide Vue docs](https://lucide.dev/guide/packages/lucide-vue-next): import named icon components, pass `class`, `size`, `stroke-width` / `absoluteStrokeWidth` as needed).
- Icons use **`currentColor`** by default; pair with Tailwind/DaisyUI text classes for theme alignment.

## Usage conventions (when executing)

- Prefer **direct imports** of the few icons a file needs (`import { Menu, Search } from 'lucide-vue-next'`) rather than a global icon barrel, unless the repo later standardizes a thin wrapper.
- Match **visible size** to today’s Svg (often `width`/`height` 20–25px); use `size` prop or `class="w-5 h-5"` consistently.
- Replace **global** `<SvgFoo />` (from `components.d.ts` auto-registration) with explicit Lucide components in `<script setup>` so call sites stay obvious.
- After removing Svg files, **regenerate or let tooling refresh** `frontend/components.d.ts` (typically via the existing Vue components resolver / dev build).

## Keep as custom Vue SVG (no satisfactory single Lucide glyph)

| Asset | Reason |
|-------|--------|
| `flags/SvgFlagEN.vue`, `flags/SvgFlagID.vue` | Real flags, not in Lucide set |
| `SvgWikidata.vue` | Wikidata wordmark |
| `SvgCertifiedAssessment.vue` | Branded “Odd-e” badge + text |
| `SvgAssessment.vue` | Dense composite (clipboard + figure + UI chrome) |
| `SvgRecallSetting.vue` | Dense composite (eye + recall machine metaphor) |
| `SvgSearchForLink.vue` | Composite (search + graph); optional later: **two** Lucide icons in a wrapper if product wants |
| `SvgTranslationEdit.vue` | Distinctive translation-service style mark; Lucide `Languages` is a generic alternative if team accepts the visual change |

**Emoji-style / strongly illustrative (decision when executing):**

- `SvgHappy.vue`, `SvgSad.vue`, `SvgSatisfying.vue`, `SvgFailed.vue`, `SvgAgree.vue` — filled emoji-like circles. Either **keep** for character or replace with **`Smile` / `Frown` / `Smile` / `Frown` or `Angry` / `ThumbsUp`** knowing the UI will look more “system” and less playful.

**Colored media controls (decision when executing):**

- `SvgPause.vue`, `SvgStop.vue` — green/red filled circles. Either **keep** for recall UX clarity or replace with **`Pause` / `Square`** plus DaisyUI/Tailwind color classes on a wrapper.

## Mapping: replace with Lucide (best fit)

Excludes everything under `link_types/` and `flags/`. Lucide names are PascalCase component names.

| Current | Lucide | Notes |
|---------|--------|--------|
| `SvgAdd.vue` | `Plus` | |
| `SvgMenu.vue` | `Menu` | |
| `SvgClose.vue` | `X` | |
| `SvgSearch.vue` | `Search` | |
| `SvgSearchWikidata.vue` | `Search` | Same magnifier semantics |
| `SvgCog.vue` | `Settings` | |
| `SvgChevronRight.vue` | `ChevronRight` | |
| `SvgDown.vue` | `ArrowDown` | |
| `SvgUp.vue` | `ArrowUp` | |
| `SvgEdit.vue` | `Pencil` | |
| `SvgUndo.vue` | `Undo2` | |
| `SvgDownload.vue` | `Download` | |
| `SvgExport.vue` | `Upload` | Upload-from-tray metaphor matches current glyph |
| `SvgImage.vue` | `Image` | |
| `SvgClipboard.vue` | `ClipboardCheck` | |
| `SvgCalendarCheck.vue` | `CalendarCheck` | |
| `SvgClockHistory.vue` | `History` | |
| `SvgGithub.vue` | `Github` | |
| `SvgNotebook.vue` | `BookText` | Bootstrap “journal-text” equivalent |
| `SvgNewNotebook.vue` | `BookPlus` | Journal + plus |
| `SvgNote.vue` | `FileText` or `ClipboardList` | Pick one per context |
| `SvgMarkdown.vue` | `FileCode` | Not the Bootstrap “M”; acceptable generic |
| `SvgPeople.vue` | `Users` | |
| `SvgChat.vue` | `MessageCircle` | |
| `SvgShop.vue` | `Store` | |
| `SvgRobot.vue` | `Bot` | |
| `SvgAudioInput.vue` | `Mic` | |
| `SvgRaiseHand.vue` | `Hand` | |
| `SvgPopup.vue` | `ExternalLink` or `SquareArrowOutUpRight` | |
| `SvgUrlIndicator.vue` | `Globe` | |
| `SvgDescriptionIndicator.vue` | `AlignJustify` | |
| `SvgEditText.vue` | `FilePenLine` | |
| `SvgRichContent.vue` | `LayoutTemplate` or `PanelsTopLeft` | Choose one; both read as “structured content” |
| `SvgUnsubscribe.vue` | `Minus` | Horizontal bar |
| `SvgAssimilate.vue` | `CircleCheck` | |
| `SvgAssociation.vue` | `Link2` | |
| `SvgMoveToCircle.vue` | `GitMerge` | Branch-into-nodes metaphor |
| `SvgContest.vue` | `Target` | Circular / goal metaphor |
| `SvgResume.vue` | `Play` | |
| `SvgForward.vue` | `Play` | Filled triangle → outline; style with class |
| `SvgBackward.vue` | `Play` (rotate 180°) or `SkipBack` | |
| `SvgFastForward.vue` | `FastForward` | |
| `SvgFastBackward.vue` | `Rewind` | |
| `SvgSkip.vue` | `SkipForward` | |
| `SvgExpand.vue` | `ChevronsUpDown` | |
| `SvgCollapse.vue` | `ChevronsUpDown` + rotation or `FoldVertical` | If `FoldVertical` unavailable in base set, use chevrons + `class` |
| `SvgGoBack.vue` | `Reply` | |
| `SvgLastResult.vue` | `Reply` | Duplicate of go-back shape today |
| `SvgLogin.vue` | `KeyRound` | Drops yellow circle key art |
| `SvgRemove.vue` | `Trash2` | |
| `SvgAddChild.vue` | `FolderPlus` | Loses “parent folder ghost”; acceptable |
| `SvgAddSibling.vue` | `FolderPlus` or `Folders` | Weaker fit; prefer `Folders` + `Plus` only if a single icon is unclear |
| `SvgBazaarShare.vue` | `Share2` | |
| `SvgMissingAvatar.vue` | `User` | |
| `SvgNoRecall.vue` | `EyeOff` | |
| `SvgRelationTypeIcon.vue` | — | **No change** (not an icon; composes `link_types/`) |

## Phases (execution order)

1. **Bootstrap** — Add `lucide-vue-next` to `frontend/package.json`; run install via workspace; fix any peer warnings.
2. **Inventory call sites** — `rg '<Svg[A-Z]' frontend/src frontend/tests` and `rg '@/components/svgs/'` to list every usage (many Svg components are globally registered and may not appear as imports).
3. **Migrate in batches** (suggested) — e.g. toolbars/menus → notes → conversations → pages → composables/stories/tests; one PR-sized slice at a time if preferred.
4. **Remove dead Svg files** — Delete `Svg*.vue` that are fully migrated; **do not** delete kept list above or anything under `link_types/` / `flags/`.
5. **Types / registration** — Ensure `components.d.ts` (or resolver) no longer references removed Svg components; run `pnpm frontend:test` and a quick smoke of `pnpm sut` UI.
6. **Optional polish** — Standardize default icon size (e.g. `size={20}`) in shared patterns; document in rule file (already updated in `.cursor/rules/frontend.mdc`).

## Testing

- **`CURSOR_DEV=true nix develop -c pnpm frontend:test`** after each batch or at end.
- Spot-check Storybook stories that embed Svg props (e.g. `HorizontalMenu.stories.ts`) and update passed icon components.
- No E2E change expected unless selectors depended on Svg-specific DOM (unlikely); if any test targets Svg internals, switch to `data-testid` or visible labels.

## Done criteria

- No remaining imports/usages of replaced `Svg*.vue` files; removed files gone from tree.
- `link_types/` and `flags/` unchanged; `SvgRelationTypeIcon` still works.
- Kept custom SVGs still used where listed; Lucide used everywhere else from the mapping table.
- Frontend tests green; lint/format clean.
