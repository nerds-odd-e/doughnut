import SoftKeyboardPrimer from "@/components/commons/SoftKeyboardPrimer.vue"
import { scheduleFocusTargetWithin } from "@/utils/focusTarget"
import { mount } from "@vue/test-utils"

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

/** Avoid `<dialog>` focus trap when testing in-modal v-if swaps. */
export const modalBodyStub = {
  template: '<div class="modal-stub"><slot name="body" /></div>',
}
