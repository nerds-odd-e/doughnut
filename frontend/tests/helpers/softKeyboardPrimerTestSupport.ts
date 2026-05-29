import SoftKeyboardPrimer from "@/components/commons/SoftKeyboardPrimer.vue"
import {
  scheduleFocusTargetWithin,
  softKeyboardPrimerId,
} from "@/utils/focusTarget"
import { mount } from "@vue/test-utils"
import { expect, vi } from "vitest"

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

export async function waitUntilFocused(selector: string) {
  const element = await vi.waitUntil(
    () => {
      const el = document.querySelector(selector) as HTMLElement | null
      return el !== null && document.activeElement === el ? el : null
    },
    { timeout: 2000 }
  )
  expect(document.activeElement).toBe(element)
  return element
}

/** Avoid `<dialog>` focus trap when testing in-modal v-if swaps. */
export const modalBodyStub = {
  template: '<div class="modal-stub"><slot name="body" /></div>',
}
