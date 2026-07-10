import { TextContentController } from "@generated/doughnut-backend-api/sdk.gen"
import TextContentWrapper from "@/components/notes/core/TextContentWrapper.vue"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import type { ComponentPublicInstance } from "vue"
import { afterEach } from "vitest"
import { h, nextTick } from "vue"

export let wrapper: VueWrapper<ComponentPublicInstance>

export const referencedTitleOriginal = "Original"
export const referencedTitleEdited = "Edited"
export const referencedTitleNoteId = 42

export const titleSlotInputSelector = "[data-testid=title-slot-input]"
export const referencedTitleSavePanelSelector =
  "[data-testid=referenced-title-save-panel]"
export const referencedTitleSaveKeepVisibleTextSelector =
  "[data-testid=referenced-title-save-keep-visible-text]"
export const contentSlotTextareaSelector = "[data-testid=content-slot-textarea]"

export const titleSlot = (slotProps: {
  value: string
  update: (noteId: number, v: string) => void
  blur: () => void
}) =>
  h("input", {
    "data-testid": "title-slot-input",
    value: slotProps.value,
    onInput: (e: Event) =>
      slotProps.update(1, (e.target as HTMLInputElement).value),
    onBlur: slotProps.blur,
  })

export const contentSlot = (slotProps: {
  value: string
  update: (noteId: number, v: string) => void
  blur: () => void
}) =>
  h("textarea", {
    "data-testid": "content-slot-textarea",
    value: slotProps.value,
    onInput: (e: Event) =>
      slotProps.update(1, (e.target as HTMLTextAreaElement).value),
    onBlur: slotProps.blur,
  })

export function titleSlotInput() {
  return document.querySelector(titleSlotInputSelector) as HTMLInputElement
}

export function referencedTitleSavePanel() {
  return document.querySelector(referencedTitleSavePanelSelector)
}

export function referencedTitleSaveKeepVisibleTextButton() {
  return document.querySelector(
    referencedTitleSaveKeepVisibleTextSelector
  ) as HTMLButtonElement
}

export function contentSlotTextarea() {
  return document.querySelector(
    contentSlotTextareaSelector
  ) as HTMLTextAreaElement
}

/** Matches `scheduleReferencedTitleBlurDiscardCheck` double-rAF timing. */
export async function flushReferencedTitleBlurDiscardCheck() {
  await new Promise<void>((resolve) =>
    requestAnimationFrame(() => requestAnimationFrame(() => resolve()))
  )
  await nextTick()
}

export function mountReferencedTitle() {
  wrapper = helper
    .component(TextContentWrapper)
    .withCleanStorage()
    .withProps({
      field: "edit title",
      value: referencedTitleOriginal,
      titleRenameNeedsExplicitReferenceChoice: true,
      titleEditNoteId: referencedTitleNoteId,
    })
    .mount({
      slots: { default: titleSlot },
      attachTo: document.body,
    })
  return wrapper
}

export async function mountReferencedTitleReady() {
  mountReferencedTitle()
  await flushPromises()
  return wrapper
}

export async function editReferencedTitle(
  newValue: string = referencedTitleEdited
) {
  const input = titleSlotInput()
  input.focus()
  input.value = newValue
  input.dispatchEvent(new Event("input", { bubbles: true }))
  await nextTick()
  return input
}

export function mockUpdateNoteTitleSuccess(title = referencedTitleEdited) {
  return mockSdkService(
    TextContentController,
    "updateNoteTitle",
    makeMe.aNoteRealm.title(title).please()
  )
}

export function mountContentWrapper(
  props: {
    value: string
    beforeSaveContent: (lastSaved: string, newValue: string) => Promise<boolean>
  },
  options?: { attachTo?: HTMLElement }
) {
  const chain = helper
    .component(TextContentWrapper)
    .withCleanStorage()
    .withProps({
      field: "edit content",
      ...props,
    })
  wrapper = options?.attachTo
    ? chain.mount({
        slots: { default: contentSlot },
        attachTo: options.attachTo,
      })
    : chain.mount({ slots: { default: contentSlot } })
  return wrapper
}

export function setupTextContentWrapperTests() {
  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })
}
