import { useStableModalTop } from "@/composables/modalTopAnchor"
import { mount, type VueWrapper } from "@vue/test-utils"
import { afterEach, describe, expect, it, vi } from "vitest"
import {
  ModalComp,
  closeButtonEl,
  modalPanelWrapperEl,
  modalRouter,
  mountDefaultModal,
  mountModal,
  settleModalAutofocus,
  waitForDialog,
  waitForTopAlignedDialog,
} from "./modalTestSupport"

describe("Modal", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    vi.useRealTimers()
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
  })

  it.each([
    {
      name: "close button",
      close: async () => {
        closeButtonEl()!.click()
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

  it("focuses autofocus target and prefers text controls in a marked autofocus container", async () => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })

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
    wrapper.unmount()

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
    expect(document.activeElement?.id).toBe("search-input")
  })

  it("closes when modal backdrop is clicked", async () => {
    wrapper = mountDefaultModal()
    await waitForDialog()
    modalPanelWrapperEl()!.dispatchEvent(
      new MouseEvent("mousedown", { bubbles: true, cancelable: true })
    )
    expect(wrapper.emitted().close_request).toHaveLength(1)
  })

  it("closes only topmost modal when ESC is pressed with stacked modals", () => {
    const outerClosed = vi.fn()
    const innerClosed = vi.fn()
    const StackedModalsComponent = {
      template: `
        <div>
          <Modal @close_request="outerClosed">
            <template #body>Outer modal</template>
          </Modal>
          <Modal @close_request="innerClosed">
            <template #body>Inner modal</template>
          </Modal>
        </div>
      `,
      components: { Modal: ModalComp },
      setup() {
        return { outerClosed, innerClosed }
      },
    }

    wrapper = mount(StackedModalsComponent, {
      global: { plugins: [modalRouter] },
      attachTo: document.body,
    })

    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))

    expect(innerClosed).toHaveBeenCalledTimes(1)
    expect(outerClosed).not.toHaveBeenCalled()

    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))

    expect(outerClosed).toHaveBeenCalledTimes(1)
  })
})
