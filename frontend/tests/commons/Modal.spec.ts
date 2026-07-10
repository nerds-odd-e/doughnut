import { useStableModalTop } from "@/composables/modalTopAnchor"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { vi, afterEach, describe, it, expect } from "vitest"
import { reactive } from "vue"
import {
  ModalComp,
  closeButtonEl,
  modalPanelWrapperEl,
  modalRouter,
  mountDefaultModal,
  mountModal,
  settleModalAutofocus,
  topAlignedDialogEl,
  waitForActiveElementId,
  waitForDialog,
  waitForDialogCount,
  waitForTopAlignedDialog,
} from "./modalTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", () => ({
  default: class {},
}))

describe("Modal", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("adds top alignment class when content requests stable modal top", async () => {
    const AnchorChild = {
      template: `<div>anchor</div>`,
      setup() {
        useStableModalTop()
      },
    }
    const TopAligned = {
      template: `
        <Modal @close_request="$emit('close_request')">
          <template #body><AnchorChild /></template>
        </Modal>
      `,
      components: { Modal: ModalComp, AnchorChild },
      emits: ["close_request"],
    }
    wrapper = mount(TopAligned, {
      global: { plugins: [modalRouter] },
      attachTo: document.body,
    })

    await waitForTopAlignedDialog()
    expect(topAlignedDialogEl()).toBeTruthy()
  })

  it.each([
    {
      name: "close button",
      close: async () => {
        const button = closeButtonEl()
        expect(button).toBeTruthy()
        button!.click()
      },
    },
    {
      name: "ESC key",
      close: async () => {
        document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
      },
    },
  ])("closes when $name is used", async ({ close }) => {
    wrapper = mountDefaultModal()
    await waitForDialog()
    await close()
    expect(wrapper.emitted().close_request).toHaveLength(1)
  })

  it("omits close button when showCloseButton is false", async () => {
    wrapper = mountModal(`
      <Modal :show-close-button="false" @close_request="$emit('close_request')">
        <template #body>x</template>
      </Modal>
    `)
    await waitForDialog()
    expect(closeButtonEl()).toBeNull()
    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
    expect(wrapper.emitted("close_request")).toHaveLength(1)
  })

  it("focuses the explicit autofocus target after opening the dialog", async () => {
    wrapper = mountModal(`
      <Modal @close_request="$emit('close_request')">
        <template #body>
          <button id="before-input">Before</button>
          <input id="target-input" autofocus />
        </template>
      </Modal>
    `)

    await settleModalAutofocus()
    expect(document.activeElement?.id).toBe("target-input")
  })

  it("prefers text controls inside a marked autofocus container", async () => {
    wrapper = mountModal(`
      <Modal @close_request="$emit('close_request')">
        <template #body>
          <div data-autofocus>
            <button id="history-button">History</button>
            <input id="search-input" />
          </div>
        </template>
      </Modal>
    `)

    await settleModalAutofocus()
    await waitForActiveElementId("search-input")
    expect(document.activeElement?.id).toBe("search-input")
  })

  it("closes when modal backdrop is clicked", async () => {
    wrapper = mountDefaultModal()
    await waitForDialog()
    const panelWrapper = modalPanelWrapperEl()
    expect(panelWrapper).toBeTruthy()
    panelWrapper!.dispatchEvent(
      new MouseEvent("mousedown", { bubbles: true, cancelable: true })
    )
    expect(wrapper.emitted().close_request).toHaveLength(1)
  })

  it("closes only topmost modal when ESC is pressed with stacked modals", async () => {
    const outerClosed = vi.fn()
    const innerClosed = vi.fn()
    const state = reactive({ showOuter: true, showInner: true })
    const StackedModalsComponent = {
      template: `
        <div>
          <Modal v-if="state.showOuter" @close_request="onOuterClose">
            <template #body>Outer modal</template>
          </Modal>
          <Modal v-if="state.showInner" @close_request="onInnerClose">
            <template #body>Inner modal</template>
          </Modal>
        </div>
      `,
      components: { Modal: ModalComp },
      setup() {
        return {
          state,
          onOuterClose: () => {
            state.showOuter = false
            outerClosed()
          },
          onInnerClose: () => {
            state.showInner = false
            innerClosed()
          },
        }
      },
    }

    wrapper = mount(StackedModalsComponent, {
      global: { plugins: [modalRouter] },
      attachTo: document.body,
    })

    await waitForDialogCount(2)

    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
    await flushPromises()

    expect(innerClosed).toHaveBeenCalledTimes(1)
    expect(outerClosed).not.toHaveBeenCalled()

    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
    await flushPromises()

    expect(outerClosed).toHaveBeenCalledTimes(1)
  })
})
