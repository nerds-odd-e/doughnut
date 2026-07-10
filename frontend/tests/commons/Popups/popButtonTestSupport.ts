import PopButton from "@/components/commons/Popups/PopButton.vue"
import SoftKeyboardPrimer from "@/components/commons/SoftKeyboardPrimer.vue"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { nextTick } from "vue"
import { createMemoryHistory, createRouter } from "vue-router"
import { expect } from "vitest"

export const popButtonRouter = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: "/", component: { template: "<div />" } }],
})

const defaultSlot = "<div>Test Content</div>"

export function mountPopButton(slot = defaultSlot) {
  return mount(PopButton, {
    props: { title: "Test Button" },
    slots: { default: slot },
    global: { plugins: [popButtonRouter] },
    attachTo: document.body,
  })
}

export function mountPopButtonWithPrimer(slot = defaultSlot) {
  mount(SoftKeyboardPrimer, { attachTo: document.body })
  return mountPopButton(slot)
}

export async function openPopButtonDialog(wrapper: VueWrapper) {
  await wrapper.find("button").trigger("click")
  await flushPromises()
}

export function popButtonEl(wrapper: VueWrapper) {
  return wrapper.find("button").element as HTMLButtonElement
}

export function modalCloseButtonEl() {
  return document.body.querySelector(".close-button") as HTMLElement | null
}

export async function waitForActiveElementId(id: string, attempts = 20) {
  for (let i = 0; i < attempts; i++) {
    await flushPromises()
    await nextTick()
    if (document.activeElement?.id === id) return
  }
  expect(document.activeElement?.id).toBe(id)
}
