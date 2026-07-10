import Modal from "@/components/commons/Modal.vue"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { nextTick } from "vue"
import { createMemoryHistory, createRouter } from "vue-router"
import { expect } from "vitest"

export const modalRouter = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: "/", component: { template: "<div />" } }],
})

export const ModalComp = Modal

export function dialogEl() {
  return document.body.querySelector("dialog")
}

export function topAlignedDialogEl() {
  return document.body.querySelector("dialog.modal-align-top")
}

export function closeButtonEl() {
  return document.body.querySelector(".close-button") as HTMLElement | null
}

export function modalPanelWrapperEl() {
  return document.body.querySelector(
    ".modal-panel-wrapper"
  ) as HTMLElement | null
}

export async function waitForDialog(attempts = 20) {
  for (let i = 0; i < attempts; i++) {
    await flushPromises()
    await nextTick()
    if (dialogEl()) return
  }
  expect(dialogEl()).toBeTruthy()
}

export async function waitForTopAlignedDialog(attempts = 20) {
  for (let i = 0; i < attempts; i++) {
    await flushPromises()
    await nextTick()
    if (topAlignedDialogEl()) return
  }
  expect(topAlignedDialogEl()).toBeTruthy()
}

export async function waitForDialogCount(count: number, attempts = 20) {
  for (let i = 0; i < attempts; i++) {
    await flushPromises()
    await nextTick()
    if (document.querySelectorAll("dialog").length === count) return
  }
  expect(document.querySelectorAll("dialog").length).toBe(count)
}

export async function settleModalAutofocus() {
  await waitForDialog()
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
  await flushPromises()
  await nextTick()
}

export async function waitForActiveElementId(id: string, attempts = 20) {
  for (let i = 0; i < attempts; i++) {
    await flushPromises()
    await nextTick()
    if (document.activeElement?.id === id) return
  }
  expect(document.activeElement?.id).toBe(id)
}

const defaultTestComponent = {
  template: `
    <Modal @close_request="$emit('close_request')">
      <template v-slot:header></template>
      <template v-slot:body></template>
      <template v-slot:footer></template>
    </Modal>
  `,
  components: { Modal: ModalComp },
  emits: ["close_request"],
}

export function mountDefaultModal(): VueWrapper {
  return mount(defaultTestComponent, {
    global: { plugins: [modalRouter] },
    attachTo: document.body,
  })
}

export function mountModal(template: string): VueWrapper {
  const Comp = {
    template,
    components: { Modal: ModalComp },
    emits: ["close_request"],
  }
  return mount(Comp, {
    global: { plugins: [modalRouter] },
    attachTo: document.body,
  })
}
