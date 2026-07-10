import {
  AiController,
  NoteController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NoteRefinement from "@/components/recall/NoteRefinement.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
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

export type ExtractionPreviewFieldValues = {
  newTitle?: string
  newContent?: string
  originalContent?: string
}

export function extractionPreviewFieldsFor(
  label: string
): ExtractionPreviewFieldValues {
  return {
    newTitle: `${label} title`,
    newContent: `${label} content`,
    originalContent: `${label} original`,
  }
}

export function previewFieldValue(
  wrapper: ReturnType<typeof mountNoteRefinement>,
  testId:
    | "extraction-preview-new-title"
    | "extraction-preview-new-content"
    | "extraction-preview-original-content"
): string {
  return (
    wrapper.find(`[data-test-id="${testId}"]`).element as HTMLTextAreaElement
  ).value
}

export function expectPreviewFields(
  wrapper: ReturnType<typeof mountNoteRefinement>,
  expected: ExtractionPreviewFieldValues
) {
  if (expected.newTitle !== undefined) {
    expect(previewFieldValue(wrapper, "extraction-preview-new-title")).toBe(
      expected.newTitle
    )
  }
  if (expected.newContent !== undefined) {
    expect(previewFieldValue(wrapper, "extraction-preview-new-content")).toBe(
      expected.newContent
    )
  }
  if (expected.originalContent !== undefined) {
    expect(
      previewFieldValue(wrapper, "extraction-preview-original-content")
    ).toBe(expected.originalContent)
  }
}

export function expectExtractionPreviewVisible(
  wrapper: ReturnType<typeof mountNoteRefinement>,
  visible = true
) {
  expect(wrapper.find('[data-test-id="extraction-preview"]').exists()).toBe(
    visible
  )
}

export function expectExtractionPreviewError(
  wrapper: ReturnType<typeof mountNoteRefinement>,
  message: string
) {
  expect(wrapper.find('[data-test-id="extraction-preview-error"]').text()).toBe(
    message
  )
}

export async function setPreviewFields(
  wrapper: ReturnType<typeof mountNoteRefinement>,
  fields: ExtractionPreviewFieldValues
) {
  if (fields.newTitle !== undefined) {
    await wrapper
      .find('[data-test-id="extraction-preview-new-title"]')
      .setValue(fields.newTitle)
  }
  if (fields.newContent !== undefined) {
    await wrapper
      .find('[data-test-id="extraction-preview-new-content"]')
      .setValue(fields.newContent)
  }
  if (fields.originalContent !== undefined) {
    await wrapper
      .find('[data-test-id="extraction-preview-original-content"]')
      .setValue(fields.originalContent)
  }
}

export function createDeferredGate() {
  let resolveGate!: () => void
  const gate = new Promise<void>((resolve) => {
    resolveGate = resolve
  })
  return { gate, resolve: () => resolveGate() }
}

export const loadingModalMask = () =>
  document.querySelector(".loading-modal-mask")

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

export async function mountNestedLayoutWithIndeterminateParentSelection() {
  const layout = sampleNestedLayout()
  const wrapper = await mountNoteRefinementWithLayoutReady(layout)
  await selectRefinementLayoutItem(wrapper, "p1")
  await selectRefinementLayoutItem(wrapper, "p1-2", false)
  return { layout, wrapper }
}

export async function openRemoveRefinementConfirmDialog(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper
    .find('[data-test-id="remove-refinement-layout"]')
    .trigger("click")
  await flushPromises()
}

export function expectRemoveConfirmPopup() {
  const popups = usePopups().popups.peek()
  expect(popups).toHaveLength(1)
  expect(popups[0]!.type).toBe("confirm")
  expect(popups[0]!.message).toContain("remove")
}

export async function clickRemoveRefinementLayout(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await openRemoveRefinementConfirmDialog(wrapper)
  usePopups().popups.done(true)
  await flushPromises()
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

export function labeledExtractionPreview(
  label: string,
  overrides?: Partial<NoteExtractionResult>
): NoteExtractionResult {
  const fields = extractionPreviewFieldsFor(label)
  return sampleExtractionPreview({
    newNoteTitle: fields.newTitle,
    newNoteContent: fields.newContent,
    updatedOriginalNoteContent: fields.originalContent,
    ...overrides,
  })
}

export function mockExtractNotePreviewResponses(
  ...previews: NoteExtractionResult[]
) {
  const spy = mockSdkService(AiController, "extractNotePreview", previews[0]!)
  for (const preview of previews) {
    spy.mockResolvedValueOnce(wrapSdkResponse(preview))
  }
  return spy
}

export function extractionPreviewApiCall(
  noteId: number,
  preview: NoteExtractionResult
) {
  return {
    path: { note: noteId },
    body: preview,
  }
}

export const sampleNestedLayout = (): NoteRefinementLayoutItem[] => [
  {
    id: "p1",
    text: "Parent point",
    alreadyExtracted: false,
    children: [
      {
        id: "p1-1",
        text: "Child point A",
        alreadyExtracted: false,
        children: [],
      },
      {
        id: "p1-2",
        text: "Child point B",
        alreadyExtracted: true,
        children: [],
      },
    ],
  },
  {
    id: "p2",
    text: "Separate point",
    alreadyExtracted: false,
    children: [],
  },
]

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

export const exportExtractRequestButtonTitle =
  "Export extract request for ChatGPT"

export const exportBreakdownRequestButtonTitle =
  "Export breakdown request for ChatGPT"

export const sampleExtractExportData = {
  model: "gpt-4",
  instructions: "Extract selected layout points",
  input: "Note content",
  text: {
    format: {
      type: "json_schema",
      schema: { type: "object" },
    },
  },
}

export function exportLoadingEl() {
  return document.body.querySelector('[data-testid="export-loading"]')
}

export function exportTextarea() {
  return document.body.querySelector(
    '[data-testid="export-textarea"]'
  ) as HTMLTextAreaElement | null
}

export async function clickExportExtractRequest(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper.find('[data-test-id="export-extract-request"]').trigger("click")
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
  selectedItemIds: string[]
) {
  return {
    path: { note: noteId },
    body: {
      layout: { items },
      selectedItemIds,
    },
  }
}

export async function openExtractionPreview(
  wrapper: ReturnType<typeof mountNoteRefinement>,
  itemId: string
) {
  await selectRefinementLayoutItem(wrapper, itemId)
  await clickExtractRefinementLayout(wrapper)
  await flushPromises()
}

export async function clickCreateNoteFromExtractionPreview(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper
    .find('[data-test-id="extraction-preview-create"]')
    .trigger("click")
}

export async function createNoteFromExtractionPreview(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await clickCreateNoteFromExtractionPreview(wrapper)
  await flushPromises()
}

export async function clickExtractionPreviewBack(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper
    .find('[data-test-id="extraction-preview-back"]')
    .trigger("click")
}

export async function clickRetryExtractionPreview(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await wrapper
    .find('[data-test-id="retry-extraction-preview"]')
    .trigger("click")
}

export async function retryExtractionPreview(
  wrapper: ReturnType<typeof mountNoteRefinement>
) {
  await clickRetryExtractionPreview(wrapper)
  await flushPromises()
}
