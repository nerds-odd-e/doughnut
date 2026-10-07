import type { WikidataSearchEntity } from "@generated/donut-backend-api"
import type { Ref } from "vue"
import {
  appendAliasToNoteContent,
  calculateNewTitle,
} from "@/utils/wikidataTitleActions"
import { contentWithWikidataFrontmatter } from "./noteNewFormSubmit"

export function applyWikidataEntryToNewNoteForm(
  selectedSuggestion: WikidataSearchEntity,
  titleAction: "replace" | "append" | undefined,
  state: {
    wikidataIdSelection: Ref<string>
    newTitle: Ref<string>
    noteContentMarkdown: Ref<string | undefined>
    hasTitleBeenEdited: Ref<boolean>
  }
) {
  state.wikidataIdSelection.value = selectedSuggestion.id ?? ""

  if (titleAction === "append") {
    const baseContent =
      state.noteContentMarkdown.value ??
      contentWithWikidataFrontmatter(state.wikidataIdSelection.value) ??
      ""
    const appended = appendAliasToNoteContent(
      baseContent,
      selectedSuggestion.label
    )
    if (appended !== null) {
      state.noteContentMarkdown.value = appended
    }
    state.hasTitleBeenEdited.value = true
    return
  }

  if (titleAction) {
    state.newTitle.value = calculateNewTitle(
      state.newTitle.value,
      selectedSuggestion,
      titleAction
    )
  } else {
    state.newTitle.value = selectedSuggestion.label
  }
  state.hasTitleBeenEdited.value = true
}
