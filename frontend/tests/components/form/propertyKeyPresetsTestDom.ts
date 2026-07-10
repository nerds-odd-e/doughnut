import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"

export const INSERT_KEY_INPUT = "rich-note-property-key"
export const ROW_KEY_INPUT = "rich-note-property-row-key-input"

export function keyInputEl(testId: string): HTMLInputElement {
  const el = document.querySelector(
    `[data-testid="${testId}"]`
  ) as HTMLInputElement | null
  expect(el).not.toBeNull()
  return el!
}

export function keyInputValue(testId: string): string {
  return keyInputEl(testId).value
}

export async function focusKeyInput(testId: string) {
  keyInputEl(testId).focus()
  await nextTick()
  await flushPromises()
}

export function presetOptionEls(): HTMLElement[] {
  return Array.from(
    document.querySelectorAll(
      '[data-testid="rich-note-property-key-preset-option"]'
    )
  )
}

export function assertPresetOptionsVisible(expectedKeys: readonly string[]) {
  const options = presetOptionEls()
  expect(options.length).toBe(expectedKeys.length)
  for (const key of expectedKeys) {
    expect(options.find((o) => o.dataset.presetKey === key)).toBeDefined()
  }
}

export async function selectPresetKey(presetKey: string) {
  const btn = presetOptionEls().find((o) => o.dataset.presetKey === presetKey)
  expect(btn).toBeDefined()
  btn!.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }))
  btn!.click()
  await flushPromises()
}
