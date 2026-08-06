import PopButton from "@/components/commons/Popups/PopButton.vue"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { createMemoryHistory, createRouter } from "vue-router"

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
