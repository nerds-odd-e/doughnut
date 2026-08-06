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
  NoteRefinementQuestionContextDto,
} from "@generated/doughnut-backend-api"
import { afterEach, beforeEach, vi } from "vitest"
import { defineComponent, type PropType } from "vue"
import {
  refinementLayoutItems,
  sampleExtractionPreview,
  selectRefinementLayoutItem,
} from "./noteRefinementLayoutFixtures"

export const noteRealm = makeMe.aNoteRealm.please()
export const memoryTracker = makeMe.aMemoryTracker.ofNote(noteRealm).please()
export const { note } = memoryTracker

export {
  extractNoteButtonTitle,
  threePointLayoutTexts,
  threePointLayout,
  sampleExtractionPreview,
  layoutCheckbox,
  selectRefinementLayoutItem,
  refinementActionButton,
  refinementLayoutItems,
  refinementLayoutSelectionApiCall,
} from "./noteRefinementLayoutFixtures"

const NoteRefinementWithGlobalLoading = defineComponent({
  components: { GlobalApiLoadingModal, NoteRefinement },
  props: {
    note: { type: Object as PropType<Note>, required: true },
    questionContext: {
      type: Object as PropType<NoteRefinementQuestionContextDto>,
      required: false,
    },
  },
  emits: ["contentUpdated"],
  template: `
    <NoteRefinement
      :note="note"
      v-bind="questionContext ? { questionContext } : {}"
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
    mockSdkService(
      AiController,
      "extractNotePreview",
      sampleExtractionPreview()
    )
    mockSdkService(
      AiController,
      "createExtractedNote",
      makeMe.aNoteRealm.please()
    )
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
  layoutItemTexts: string[],
  overrides?: {
    note?: typeof note
    questionContext?: NoteRefinementQuestionContextDto
  }
) {
  return mountNoteRefinementWithLayout(refinementLayoutItems(layoutItemTexts), {
    note: overrides?.note,
    questionContext: overrides?.questionContext,
  })
}

export function mountNoteRefinementWithLayout(
  items: NoteRefinementLayoutItem[],
  overrides?: {
    note?: typeof note
    questionContext?: NoteRefinementQuestionContextDto
  }
) {
  mockSdkService(AiController, "generateRefinementSuggestions", {
    items,
  })
  return renderer
    .withCleanStorage()
    .withProps({
      note: overrides?.note ?? note,
      ...(overrides?.questionContext !== undefined && {
        questionContext: overrides.questionContext,
      }),
    })
    .mount()
}

export function refinementLayoutPanel(wrapper: {
  find: (s: string) => { findAll: (s: string) => unknown[] }
}) {
  return wrapper.find('[data-test-id="refinement-layout"]')
}

export async function selectFirstLayoutItem(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await selectRefinementLayoutItem(wrapper, "p1")
}

export async function mountNoteRefinementReady(layoutItemTexts: string[]) {
  const wrapper = mountNoteRefinement(layoutItemTexts)
  await flushPromises()
  return wrapper
}

export async function mountNoteRefinementWithLayoutReady(
  items: NoteRefinementLayoutItem[],
  overrides?: {
    note?: typeof note
    questionContext?: NoteRefinementQuestionContextDto
  }
) {
  const wrapper = mountNoteRefinementWithLayout(items, overrides)
  await flushPromises()
  return wrapper
}

export async function clickExtractRefinementLayout(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper
    .find('[data-test-id="extract-refinement-layout"]')
    .trigger("click")
}

export async function mountNoteRefinementWithFirstItemSelected(
  layoutItemTexts: string[] = ["Point 1", "Point 2"],
  overrides?: { note?: typeof note }
) {
  const wrapper = mountNoteRefinement(layoutItemTexts, overrides)
  await flushPromises()
  await selectFirstLayoutItem(wrapper)
  return wrapper
}

export {
  createDeferredGate,
  loadingModalMask,
  mountNoteRefinementPendingLayout,
  clickLoadingModalCancel,
  clickRetryRefinementLayout,
} from "./noteRefinementLayoutLoadingTestSupport"
