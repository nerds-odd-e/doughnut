# Consumer trace

Preparation trace, 2026-11-10 at c2958ecb8e5a. `run` dispositions below are
planned execution proof, not claims of completed product changes. Q is the
affected mounted command in PLAN.md; T is its required frontend typechecks.
Matching preparation observations are recorded separately in PLAN.md.

## Effect terms searched

| Term | Origin |
| --- | --- |
| `.planning/seeds/SEED-117-recall-note-location.md` | changed path |
| `SEED-117-recall-note-location.md` | basename of `.planning/seeds/SEED-117-recall-note-location.md` |
| `NoteUnderQuestion` | supplied |
| `note-under-question` | supplied |
| `Focused property:` | supplied |
| `NoteTitleWithLink` | supplied |
| `memoryTrackerPageViewTestSupport` | supplied |
| `I follow the note under question` | supplied |
| `the note under question should focus on property` | supplied |
| `expectAccidentalMatchRevealForNotes` | supplied |
| `expectAccidentalMatchReveal` | supplied |
| `I should see an accidental match reveal for spelling answer` | supplied |
| `.planning/seeds/SEED-117-recall-note-location` | import path of `.planning/seeds/SEED-117-recall-note-location.md` |
| `SEED-117-recall-note-location` | import path of `.planning/seeds/SEED-117-recall-note-location.md` |
| `not-refined` | removed from `.planning/seeds/SEED-117-recall-note-location.md`:19 |

## Scope

Whole checkout at `c2958ecb8e5a`, including uncommitted and untracked files: 4198 tracked and untracked non-ignored files searched with fixed-string, case-insensitive matching.

Changed files not searched: `.planning/seeds/SEED-117-recall-note-location.md`.

## Located consumers

Kinds are path hints for inspection; each file appears once. Fill every Disposition with `run`, `reused` or `excluded: <reason the effect does not reach its boundary>`.

### Production

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-story-state-home.mjs` | `not-refined` | 121: `* @property {"not-refined" \| "refined"} refinement` | excluded: recorder/schema guidance is unchanged; only a selected story's recorded facts change |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-story-state-preparation.mjs` | `not-refined` | 12: `const refinements = new Set(["not-refined", "refined"]);` | excluded: recorder/schema guidance is unchanged; only a selected story's recorded facts change |
| `.agents/skills/dough-product-backlog/scripts/product-backlog-usage.mjs` | `not-refined` | 36: `--refinement not-refined\|refined` | excluded: recorder/schema guidance is unchanged; only a selected story's recorded facts change |
| `frontend/components.d.ts` | `NoteUnderQuestion`, `NoteTitleWithLink` | 148: `NoteTitleWithLink: typeof import('./src/components/notes/NoteTitleWithLink.vue')['default']` | run — Q and T; regenerate declarations/list only if component/story identity changes |
| `frontend/src/components/notes/Card.vue` | `NoteTitleWithLink` | 10: `<NoteTitleWithLink v-bind="{ noteTopology }" />` | excluded: caller supplies no focused-property title prop; its whole-note destination is unchanged (T checks signature) |
| `frontend/src/components/recall/AccidentalMatchResolveRow.vue` | `NoteTitleWithLink` | 6: `<NoteTitleWithLink :note-topology="matched" />` | excluded: caller supplies no focused-property title prop; its whole-note destination is unchanged (T checks signature) |
| `frontend/src/components/recall/AnsweredQuestionComponent.vue` | `NoteUnderQuestion` | 3: `<NoteUnderQuestion` | run — Q and T; regenerate declarations/list only if component/story identity changes |
| `frontend/src/components/recall/AnsweredSpellingQuestion.vue` | `NoteUnderQuestion` | 30: `<NoteUnderQuestion` | run — Q and T; regenerate declarations/list only if component/story identity changes |
| `frontend/src/components/recall/FocusedPropertyIndicator.vue` | `Focused property:` | 7: `Focused property: {{ propertyKey` | run — Q and T; regenerate declarations/list only if component/story identity changes |
| `frontend/src/components/recall/NoteUnderQuestion.stories.ts` | `NoteUnderQuestion` | 2: `import NoteUnderQuestion from "./NoteUnderQuestion.vue"` | run — align note-context fixture and Full path cases; T plus mounted equivalent fixture proof |
| `frontend/src/components/recall/NoteUnderQuestion.vue` | `note-under-question`, `NoteTitleWithLink` | 2: `<div class="note-under-question bg-base-200 border-base-300">` | run — Q and T; regenerate declarations/list only if component/story identity changes |
| `frontend/src/components/recall/recalledNoteUnderQuestionProps.ts` | `NoteUnderQuestion` | 3: `export function recalledNoteUnderQuestionProps(recalledNote: RecalledNote) {` | run — Q and T; regenerate declarations/list only if component/story identity changes |
| `frontend/src/components/recent/RecentlyAssimilatedNotes.vue` | `NoteTitleWithLink` | 17: `<NoteTitleWithLink :noteTopology="point.note.noteTopology" />` | excluded: caller supplies no focused-property title prop; its whole-note destination is unchanged (T checks signature) |
| `frontend/src/components/recent/RecentlyRecalledNotes.vue` | `NoteTitleWithLink` | 22: `<NoteTitleWithLink :noteTopology="point.note.noteTopology" />` | excluded: caller supplies no focused-property title prop; its whole-note destination is unchanged (T checks signature) |
| `frontend/src/components/search/SearchDropdownHitList.vue` | `NoteTitleWithLink` | 17: `<NoteTitleWithLink :note-topology="hit.noteSearchResult.noteTopology" />` | excluded: caller supplies no focused-property title prop; its whole-note destination is unchanged (T checks signature) |
| `frontend/src/components/search/SearchResultListItem.vue` | `NoteTitleWithLink` | 18: `<NoteTitleWithLink` | excluded: caller supplies no focused-property title prop; its whole-note destination is unchanged (T checks signature) |
| `frontend/src/components/toolbars/UndoConfirmationForm.vue` | `NoteTitleWithLink` | 7: `<NoteTitleWithLink` | excluded: caller supplies no focused-property title prop; its whole-note destination is unchanged (T checks signature) |
| `frontend/src/pages/MemoryTrackerPageView.vue` | `NoteUnderQuestion` | 42: `<NoteUnderQuestion` | run — Q and T; regenerate declarations/list only if component/story identity changes |
| `frontend/storyFiles.generated.ts` | `NoteUnderQuestion` | 8: `import * as _story_5 from "@/components/recall/NoteUnderQuestion.stories"` | run — Q and T; regenerate declarations/list only if component/story identity changes |

### Test

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts` | `NoteUnderQuestion`, `note-under-question` | 51: `const noteUnderQuestion = wrapper.find(".note-under-question")` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |
| `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatchNoteContext.spec.ts` | `note-under-question` | 41: `.find(".note-under-question")` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |
| `frontend/tests/components/recall/NoteUnderQuestion.spec.ts` | `NoteUnderQuestion`, `Focused property:` | 1: `import NoteUnderQuestion from "@/components/recall/NoteUnderQuestion.vue"` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |
| `frontend/tests/pages/MemoryTrackerPage.privateHelp.spec.ts` | `memoryTrackerPageViewTestSupport` | 16: `import { historyFromPrompts } from "./memoryTrackerPageViewTestSupport"` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |
| `frontend/tests/pages/MemoryTrackerPage.spec.ts` | `memoryTrackerPageViewTestSupport` | 18: `} from "./memoryTrackerPageViewTestSupport"` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |
| `frontend/tests/pages/MemoryTrackerPageView.history.spec.ts` | `memoryTrackerPageViewTestSupport` | 7: `} from "./memoryTrackerPageViewTestSupport"` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |
| `frontend/tests/pages/MemoryTrackerPageView.privateHelp.spec.ts` | `memoryTrackerPageViewTestSupport` | 11: `import { historyFromPrompts } from "./memoryTrackerPageViewTestSupport"` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |
| `frontend/tests/pages/MemoryTrackerPageView.recallPrompts.spec.ts` | `NoteUnderQuestion`, `memoryTrackerPageViewTestSupport` | 7: `noteUnderQuestionSections,` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |
| `frontend/tests/pages/MemoryTrackerPageView.spec.ts` | `Focused property:`, `memoryTrackerPageViewTestSupport` | 25: `} from "./memoryTrackerPageViewTestSupport"` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |
| `frontend/tests/pages/MemoryTrackerPageView.spellingRefinement.spec.ts` | `memoryTrackerPageViewTestSupport` | 18: `import { historyFromPrompts } from "./memoryTrackerPageViewTestSupport"` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |
| `frontend/tests/pages/memoryTrackerPageViewTestSupport.ts` | `NoteUnderQuestion`, `note-under-question` | 151: `export function noteUnderQuestionSections(wrapper: VueWrapper) {` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |
| `frontend/tests/pages/RecallPage.spelling.spec.ts` | `NoteUnderQuestion`, `note-under-question` | 107: `const noteUnderQuestion = wrapper.find(".note-under-question")` | run — Q; keep preview, source identity, history and action observables; update HTTP fixtures at the external boundary |

### Test support or stand-in

none found

### Journey or feature

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `e2e_test/features/recall/accidental_match_scheduling.feature` | `I should see an accidental match reveal for spelling answer` | 25: `Then I should see an accidental match reveal for spelling answer "sedation" with reviewed note "sedition" and matched note "sedation"` | run — Q's AnsweredSpellingQuestion accidental-match specs retain reveal/resolve behavior; include accidental_match_scheduling.feature if this boundary changes |
| `e2e_test/features/recall/property_memory_tracker.feature` | `I follow the note under question`, `the note under question should focus on property` | 56: `When I follow the note under question "Vitamins"` | run — property_memory_tracker.feature; distinguish note/property actions and raw item identity from displayed reference text |
| `e2e_test/start/pageObjects/answeredQuestionAccidentalMatch.ts` | `note-under-question`, `expectAccidentalMatchRevealForNotes`, `expectAccidentalMatchReveal` | 13: `export function expectAccidentalMatchRevealForNotes(` | run — Q's AnsweredSpellingQuestion accidental-match specs retain reveal/resolve behavior; include accidental_match_scheduling.feature if this boundary changes |
| `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` | `expectAccidentalMatchRevealForNotes`, `expectAccidentalMatchReveal` | 2: `import { expectAccidentalMatchRevealForNotes } from './answeredQuestionAccidentalMatch'` | run — Q's AnsweredSpellingQuestion accidental-match specs retain reveal/resolve behavior; include accidental_match_scheduling.feature if this boundary changes |
| `e2e_test/start/pageObjects/noteUnderQuestion.ts` | `NoteUnderQuestion`, `note-under-question`, `Focused property:` | 4: `export function followNoteUnderQuestion(noteTitle: string) {` | run — property_memory_tracker.feature; distinguish note/property actions and raw item identity from displayed reference text |
| `e2e_test/step_definitions/accidental_match.ts` | `expectAccidentalMatchReveal`, `I should see an accidental match reveal for spelling answer` | 9: `'I should see an accidental match reveal for spelling answer {string} with reviewed note {string} and matched note {string}',` | run — Q's AnsweredSpellingQuestion accidental-match specs retain reveal/resolve behavior; include accidental_match_scheduling.feature if this boundary changes |
| `e2e_test/step_definitions/assimilation_memory_tracker.ts` | `NoteUnderQuestion`, `I follow the note under question`, `the note under question should focus on property` | 9: `expectNoteUnderQuestionFocusedProperty,` | run — property_memory_tracker.feature; distinguish note/property actions and raw item identity from displayed reference text |

### Guidance reader

| File | Terms matched | First matching line | Disposition |
| --- | --- | --- | --- |
| `.agents/skills/dough-product-backlog/references/record-preparation.md` | `not-refined` | 41: `` \| Decomposition leaves a candidate with a recorded identity \| `--refinement not-refined` and `--approach unselected` \| `` | excluded: recorder/schema guidance is unchanged; only a selected story's recorded facts change |
| `.planning/PRODUCT-BACKLOG.md` | `SEED-117-recall-note-location.md`, `SEED-117-recall-note-location` | 16: `- [Find the note and property after recall](seeds/SEED-117-recall-note-location.md#find-the-note-and-property-after-recall) — SEED-117#find-the-note-and-proper…` | run — installed recorder validates source identity/link; preserve queue position |
| `.planning/seeds/SEED-116-agent-shell-independent-commands.md` | `not-refined` | 16: `{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}` | excluded: sibling stories' content and state are outside this source's change |
| `.planning/seeds/SEED-120-export-highlighted-property.md` | `not-refined` | 19: `{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}` | excluded: sibling stories' content and state are outside this source's change |
