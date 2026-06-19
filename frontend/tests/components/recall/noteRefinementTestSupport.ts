import {
  AiController,
  NoteController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NoteRefinement from "@/components/recall/NoteRefinement.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import usePopups from "@/components/commons/Popups/usePopups"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import type {
  Note,
  NoteRefinementLayoutItem,
} from "@generated/doughnut-backend-api"
import { afterEach, beforeEach, vi } from "vitest"
import { defineComponent, type PropType } from "vue"

export const noteRealm = makeMe.aNoteRealm.please()
export const memoryTracker = makeMe.aMemoryTracker.ofNote(noteRealm).please()
export const { note } = memoryTracker

const NoteRefinementWithGlobalLoading = defineComponent({
  components: { GlobalApiLoadingModal, NoteRefinement },
  props: {
    note: { type: Object as PropType<Note>, required: true },
  },
  emits: ["contentUpdated"],
  template: `
    <NoteRefinement
      :note="note"
      @contentUpdated="$emit('contentUpdated', $event)"
    />
    <GlobalApiLoadingModal />
  `,
})

export let renderer: RenderingHelper<typeof NoteRefinementWithGlobalLoading>

export function setupNoteRefinementTests() {
  beforeEach(() => {
    mockSdkService(AiController, "removeRefinementSuggestion", {
      content: "Updated content",
    })
    mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
    mockSdkService(AiController, "extractNote", makeMe.aNoteRealm.please())
    renderer = helper.component(NoteRefinementWithGlobalLoading).withRouter()
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.clearAllMocks()
    teardownGlobalClientForTesting()
    const popups = usePopups()
    while (popups.popups.peek().length) {
      popups.popups.done(false)
    }
  })
}

export function mountNoteRefinement(
  suggestions: string[],
  overrides?: { note?: typeof note }
) {
  mockSdkService(AiController, "generateRefinementSuggestions", {
    items: refinementLayoutItems(suggestions),
  })
  return renderer
    .withCleanStorage()
    .withProps({
      note: overrides?.note ?? note,
    })
    .mount()
}

export function refinementSuggestionsPanel(wrapper: {
  find: (s: string) => { findAll: (s: string) => unknown[] }
}) {
  return wrapper.find('[data-test-id="refinement-suggestions"]')
}

export async function selectFirstSuggestion(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  const checkboxes = refinementSuggestionsPanel(wrapper).findAll(
    'input[type="checkbox"]'
  ) as {
    setValue: (v: boolean) => Promise<void>
  }[]
  await checkboxes[0]?.setValue(true)
  await flushPromises()
}

export const extractNoteButtonTitle = "Extract to a new note"

export function refinementLayoutItems(
  suggestions: string[]
): NoteRefinementLayoutItem[] {
  return suggestions.map((text, index) => ({
    id: `p${index + 1}`,
    text,
    alreadyExtracted: false,
    children: [],
  }))
}

export function refinementSuggestionsApiCall(
  noteId: number,
  suggestions: string[]
) {
  return {
    path: { note: noteId },
    body: { suggestions },
  }
}
