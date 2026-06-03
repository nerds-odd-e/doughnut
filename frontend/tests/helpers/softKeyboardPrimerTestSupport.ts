import SoftKeyboardPrimer from "@/components/commons/SoftKeyboardPrimer.vue"
import {
  scheduleFocusTargetWithin,
  softKeyboardPrimerId,
} from "@/utils/focusTarget"
import { mount } from "@vue/test-utils"
import { expect } from "vitest"

/** Mirrors production `v-focus` for mounted-component tests. */
export const focusDirective = {
  mounted(el: HTMLElement) {
    el.setAttribute("data-autofocus", "true")
    scheduleFocusTargetWithin(el)
  },
}

export function mountSoftKeyboardPrimer() {
  mount(SoftKeyboardPrimer, { attachTo: document.body })
}

export function softKeyboardPrimerElement() {
  return document.getElementById(softKeyboardPrimerId)
}

export function expectSoftKeyboardPrimerIsFocused() {
  expect(document.activeElement).toBe(softKeyboardPrimerElement())
}

export function expectSoftKeyboardPrimerIsNotFocused() {
  expect(document.activeElement).not.toBe(softKeyboardPrimerElement())
}

export async function waitUntilFocused(selector: string) {
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
  const element = document.querySelector(selector) as HTMLElement | null
  expect(element).toBeTruthy()
  expect(document.activeElement).toBe(element)
  return element!
}

/** Avoid `<dialog>` focus trap when testing in-modal v-if swaps. */
export const modalBodyStub = {
  template: '<div class="modal-stub"><slot name="body" /></div>',
}
