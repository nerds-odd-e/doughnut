import type {
  NoteExtractionResult,
  NoteRefinementLayoutItem,
} from "@generated/doughnut-backend-api"
import { flushPromises } from "@vue/test-utils"
import { expect } from "vitest"

export const extractNoteButtonTitle = "Extract selected to a new note"

export const threePointLayoutTexts = ["Point 1", "Point 2", "Point 3"] as const

export function threePointLayout() {
  return refinementLayoutItems([...threePointLayoutTexts])
}

export const sampleExtractionPreview = (
  overrides?: Partial<NoteExtractionResult>
): NoteExtractionResult => ({
  newNoteTitle: "Extracted title",
  newNoteContent: "Extracted content",
  updatedOriginalNoteContent: "Updated original content",
  ...overrides,
})

export function refinementLayoutItems(
  texts: string[],
  options?: { ledToQuestion?: boolean[] }
): NoteRefinementLayoutItem[] {
  return texts.map((text, index) => ({
    id: `p${index + 1}`,
    text,
    alreadyExtracted: false,
    ledToQuestion: options?.ledToQuestion?.[index] ?? false,
    children: [],
  }))
}

export function layoutCheckbox(
  wrapper: {
    find: (s: string) => { element: Element }
  },
  itemId: string
): HTMLInputElement {
  return wrapper.find(`[data-test-id="refinement-layout-checkbox-${itemId}"]`)
    .element as HTMLInputElement
}

export async function selectRefinementLayoutItem(
  wrapper: {
    find: (s: string) => { setValue: (v: boolean) => Promise<unknown> }
  },
  itemId: string,
  checked = true
) {
  await wrapper
    .find(`[data-test-id="refinement-layout-checkbox-${itemId}"]`)
    .setValue(checked)
  await flushPromises()
}

export function refinementActionButton(
  wrapper: { find: (s: string) => { element: Element } },
  testId:
    | "extract-refinement-layout"
    | "export-extract-request"
    | "export-breakdown-request"
    | "remove-refinement-layout"
): HTMLButtonElement {
  return wrapper.find(`[data-test-id="${testId}"]`).element as HTMLButtonElement
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
