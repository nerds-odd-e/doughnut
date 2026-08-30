import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { noteShowLocation } from "@/routes/noteShowLocation"
import type { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

type Harness = ReturnType<typeof createRichMarkdownEditorTestHarness>
type MountEditorOptions = Parameters<Harness["mountEditor"]>[1]

export const PROPERTY_PANEL_NOTE_ID = 42

export const PROPERTY_VALUE_DIALOG_OPEN_SELECTOR =
  '[data-testid="rich-note-property-value-dialog-open"]'

const PROPERTY_VALUE_DIALOG_MODE_TEST_ID = {
  text: "rich-note-property-value-dialog-mode-text",
  list: "rich-note-property-value-dialog-mode-list",
} as const

type PropertyValueDialogMode = keyof typeof PROPERTY_VALUE_DIALOG_MODE_TEST_ID

export async function mountEditorOnNoteShow(
  h: Harness,
  markdown: string,
  options: MountEditorOptions = {}
) {
  const noteId =
    (options.noteId as number | undefined) ?? PROPERTY_PANEL_NOTE_ID
  return h.mountEditor(markdown, {
    attachToBody: true,
    ...options,
    noteId,
    route: options.route ?? noteShowLocation(noteId),
  })
}

export async function openPropertyValueDialog(wrapper: VueWrapper) {
  const openBtn = wrapper.find(PROPERTY_VALUE_DIALOG_OPEN_SELECTOR)
  expect(openBtn.exists()).toBe(true)
  await openBtn.trigger("click")
  await flushPromises()
}

export async function mountPropertyValueDialog(h: Harness, markdown: string) {
  const wrapper = await mountEditorOnNoteShow(h, markdown)
  await openPropertyValueDialog(wrapper)
  return wrapper
}

export function clickSave() {
  const saveBtn = document.querySelector(
    '[data-testid="rich-note-property-value-dialog-save"]'
  ) as HTMLButtonElement
  saveBtn.click()
}

export function clickCancel() {
  const cancelBtn = document.querySelector(
    '[data-testid="rich-note-property-value-dialog-cancel"]'
  ) as HTMLButtonElement
  expect(cancelBtn).not.toBeNull()
  cancelBtn.click()
}

export function modeTabEl(mode: PropertyValueDialogMode): HTMLElement | null {
  return document.querySelector(
    `[data-testid="${PROPERTY_VALUE_DIALOG_MODE_TEST_ID[mode]}"]`
  )
}

export function clickModeTab(mode: PropertyValueDialogMode) {
  const tab = modeTabEl(mode)
  expect(tab).not.toBeNull()
  tab!.click()
}

export function clickListAdd() {
  ;(
    document.querySelector(
      '[data-testid="rich-note-property-value-dialog-list-add"]'
    ) as HTMLButtonElement
  ).click()
}

export function clickListRemove(index: number) {
  ;(
    document.querySelector(
      `[data-testid="rich-note-property-value-dialog-list-remove-${index}"]`
    ) as HTMLButtonElement
  ).click()
}

export function listMoveButtonEl(
  direction: "up" | "down",
  index: number
): HTMLButtonElement | null {
  return document.querySelector(
    `[data-testid="rich-note-property-value-dialog-list-move-${direction}-${index}"]`
  ) as HTMLButtonElement | null
}

function clickListMoveButton(direction: "up" | "down", index: number) {
  const button = listMoveButtonEl(direction, index)
  expect(button).not.toBeNull()
  button!.click()
}

export function clickListMoveUp(index: number) {
  clickListMoveButton("up", index)
}

export function clickListMoveDown(index: number) {
  clickListMoveButton("down", index)
}

export function getTextareaValue(): string {
  const textarea = document.querySelector(
    '[data-testid="rich-note-property-value-dialog-textarea"]'
  ) as HTMLTextAreaElement
  expect(textarea).not.toBeNull()
  return textarea.value
}

export function setTextareaValue(value: string) {
  const textarea = document.querySelector(
    '[data-testid="rich-note-property-value-dialog-textarea"]'
  ) as HTMLTextAreaElement
  textarea.value = value
  textarea.dispatchEvent(new Event("input", { bubbles: true }))
}

export function setListItemValue(index: number, value: string) {
  const input = document.querySelector(
    `[data-testid="rich-note-property-value-dialog-list-item-${index}"]`
  ) as HTMLInputElement
  expect(input).not.toBeNull()
  input.value = value
  input.dispatchEvent(new Event("input", { bubbles: true }))
}

export function propertyValueDialogEl(): HTMLDialogElement | null {
  return document.querySelector("dialog")
}

export function isModeTabActive(mode: PropertyValueDialogMode): boolean {
  return modeTabEl(mode)?.classList.contains("daisy-tab-active") ?? false
}

export function isListModeTabActive(): boolean {
  return isModeTabActive("list")
}

export function propertyValueDialogValidationText(): string | undefined {
  return (
    document.querySelector(
      '[data-testid="rich-note-property-value-dialog-validation"]'
    )?.textContent ?? undefined
  )
}

export async function savePropertyValueDialog() {
  clickSave()
  await flushPromises()
}
