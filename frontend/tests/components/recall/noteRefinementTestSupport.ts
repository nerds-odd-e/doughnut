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
  NoteExtractionResult,
  NoteRefinementLayoutItem,
} from "@generated/doughnut-backend-api"
import { afterEach, beforeEach, expect, vi } from "vitest"
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
  overrides?: { note?: typeof note }
) {
  return mountNoteRefinementWithLayout(refinementLayoutItems(layoutItemTexts), {
    note: overrides?.note,
  })
}

export function mountNoteRefinementWithLayout(
  items: NoteRefinementLayoutItem[],
  overrides?: { note?: typeof note }
) {
  mockSdkService(AiController, "generateRefinementSuggestions", {
    items,
  })
  return renderer
    .withCleanStorage()
    .withProps({
      note: overrides?.note ?? note,
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

export const extractNoteButtonTitle = "Extract selected to a new note"

export const threePointLayoutTexts = ["Point 1", "Point 2", "Point 3"] as const

export function threePointLayout() {
  return refinementLayoutItems([...threePointLayoutTexts])
}

export async function mountNoteRefinementReady(layoutItemTexts: string[]) {
  const wrapper = mountNoteRefinement(layoutItemTexts)
  await flushPromises()
  return wrapper
}

export async function mountNoteRefinementWithLayoutReady(
  items: NoteRefinementLayoutItem[],
  overrides?: { note?: typeof note }
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

export const sampleExtractionPreview = (
  overrides?: Partial<NoteExtractionResult>
): NoteExtractionResult => ({
  newNoteTitle: "Extracted title",
  newNoteContent: "Extracted content",
  updatedOriginalNoteContent: "Updated original content",
  ...overrides,
})

export function layoutCheckbox(
  wrapper: ReturnType<typeof mountNoteRefinementWithLayout>,
  itemId: string
): HTMLInputElement {
  return wrapper.find(`[data-test-id="refinement-layout-checkbox-${itemId}"]`)
    .element as HTMLInputElement
}

export function refinementActionButton(
  wrapper: ReturnType<typeof mountNoteRefinement>,
  testId:
    | "extract-refinement-layout"
    | "export-extract-request"
    | "export-breakdown-request"
    | "remove-refinement-layout"
): HTMLButtonElement {
  return wrapper.find(`[data-test-id="${testId}"]`).element as HTMLButtonElement
}

export async function selectRefinementLayoutItem(
  wrapper: ReturnType<typeof mountNoteRefinement>,
  itemId: string,
  checked = true
) {
  await wrapper
    .find(`[data-test-id="refinement-layout-checkbox-${itemId}"]`)
    .setValue(checked)
  await flushPromises()
}

export function refinementLayoutItems(
  texts: string[]
): NoteRefinementLayoutItem[] {
  return texts.map((text, index) => ({
    id: `p${index + 1}`,
    text,
    alreadyExtracted: false,
    children: [],
  }))
}

export function refinementLayoutSelectionApiCall(
  noteId: number,
  items: NoteRefinementLayoutItem[],
  selectedItemIds: string[],
  options?: { signal?: boolean }
) {
  const call: {
    path: { note: number }
    body: {
      layout: { items: NoteRefinementLayoutItem[] }
      selectedItemIds: string[]
    }
    signal?: AbortSignal
  } = {
    path: { note: noteId },
    body: {
      layout: { items },
      selectedItemIds,
    },
  }
  if (options?.signal) {
    return { ...call, signal: expect.any(AbortSignal) }
  }
  return call
}

export {
  createDeferredGate,
  loadingModalMask,
  mountNoteRefinementPendingLayout,
  clickLoadingModalCancel,
  clickRetryRefinementLayout,
} from "./noteRefinementLayoutLoadingTestSupport"
