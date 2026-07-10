import { flushPromises, type VueWrapper } from "@vue/test-utils"

export async function openValuePopup(wrapper: VueWrapper) {
  const openBtn = wrapper.find(
    '[data-testid="rich-note-property-value-popup-open"]'
  )
  expect(openBtn.exists()).toBe(true)
  await openBtn.trigger("click")
  await flushPromises()
}

export function clickModeTab(testId: string) {
  const tab = document.querySelector(
    `[data-testid="${testId}"]`
  ) as HTMLButtonElement
  expect(tab).not.toBeNull()
  tab.click()
}

export function clickSave() {
  const saveBtn = document.querySelector(
    '[data-testid="rich-note-property-value-popup-save"]'
  ) as HTMLButtonElement
  saveBtn.click()
}

export function clickListAdd() {
  ;(
    document.querySelector(
      '[data-testid="rich-note-property-value-popup-list-add"]'
    ) as HTMLButtonElement
  ).click()
}

export function clickListRemove(index: number) {
  ;(
    document.querySelector(
      `[data-testid="rich-note-property-value-popup-list-remove-${index}"]`
    ) as HTMLButtonElement
  ).click()
}

function clickListMoveButton(direction: "up" | "down", index: number) {
  const button = document.querySelector(
    `[data-testid="rich-note-property-value-popup-list-move-${direction}-${index}"]`
  ) as HTMLButtonElement
  expect(button).not.toBeNull()
  button.click()
}

export function clickListMoveUp(index: number) {
  clickListMoveButton("up", index)
}

export function clickListMoveDown(index: number) {
  clickListMoveButton("down", index)
}

export function getTextareaValue(): string {
  const textarea = document.querySelector(
    '[data-testid="rich-note-property-value-popup-textarea"]'
  ) as HTMLTextAreaElement
  expect(textarea).not.toBeNull()
  return textarea.value
}

export function setTextareaValue(value: string) {
  const textarea = document.querySelector(
    '[data-testid="rich-note-property-value-popup-textarea"]'
  ) as HTMLTextAreaElement
  textarea.value = value
  textarea.dispatchEvent(new Event("input", { bubbles: true }))
}

export function setListItemValue(index: number, value: string) {
  const input = document.querySelector(
    `[data-testid="rich-note-property-value-popup-list-item-${index}"]`
  ) as HTMLInputElement
  expect(input).not.toBeNull()
  input.value = value
  input.dispatchEvent(new Event("input", { bubbles: true }))
}

export function dialogEl(): HTMLDialogElement | null {
  return document.querySelector("dialog")
}

export function isModeTabActive(testId: string): boolean {
  return (
    document
      .querySelector(`[data-testid="${testId}"]`)
      ?.classList.contains("daisy-tab-active") ?? false
  )
}

export function isListModeTabActive(): boolean {
  return isModeTabActive("rich-note-property-value-popup-mode-list")
}

export function popupValidationText(): string | undefined {
  return (
    document.querySelector(
      '[data-testid="rich-note-property-value-popup-validation"]'
    )?.textContent ?? undefined
  )
}

export async function savePopup() {
  clickSave()
  await flushPromises()
}
