import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import type { NoteExtractionResult } from "@generated/doughnut-backend-api"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { expect } from "vitest"
import { nextTick } from "vue"
import { createDeferredGate } from "./noteRefinementLayoutLoadingTestSupport"
import {
  clickExtractRefinementLayout,
  mountNoteRefinement,
  mountNoteRefinementReady,
  sampleExtractionPreview,
  selectRefinementLayoutItem,
  threePointLayoutTexts,
} from "./noteRefinementTestSupport"

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

/**
 * Mounts with layout flushed, selects an item, starts Extract with
 * extractNotePreview held pending. Does not flushPromises — caller asserts
 * Cancel while the preview request is open.
 */
export async function mountNoteRefinementPendingExtractionPreview(
  layoutItemTexts: string[] = [...threePointLayoutTexts],
  itemId = "p1",
  previewWhenResolved: NoteExtractionResult = sampleExtractionPreview({
    newNoteTitle: "Should not appear",
  })
) {
  const { gate, resolve } = createDeferredGate()
  const extractSpy = mockSdkServiceWithImplementation(
    AiController,
    "extractNotePreview",
    async () => {
      await gate
      return previewWhenResolved
    }
  )
  const wrapper = await mountNoteRefinementReady(layoutItemTexts)
  await selectRefinementLayoutItem(wrapper, itemId)
  await clickExtractRefinementLayout(wrapper)
  await nextTick()
  return { wrapper, resolve, gate, extractSpy }
}
