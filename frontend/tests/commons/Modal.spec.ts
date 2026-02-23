import Modal from "@/components/commons/Modal.vue"
import routes from "@/routes/routes"
import { mount, type VueWrapper } from "@vue/test-utils"
import { vi, afterEach, describe, it, expect } from "vitest"
import { createRouter, createWebHistory } from "vue-router"
import { reactive } from "vue"

// Browser Mode: Mock AiReplyEventSource to prevent import errors
// (Modal doesn't use it, but Browser Mode hoists mocks globally)
vi.mock("@/managedApi/AiReplyEventSource", () => ({
  default: class {},
}))

// Browser Mode: Use real Vue Router instead of mocking
const router = createRouter({
  history: createWebHistory(),
  routes,
})

describe("Modal", () => {
  const Comp = Modal
  const TestComponent = {
    template: `
      <Modal @close_request="$emit('close_request')">
        <template v-slot:header>
        </template>
        <template v-slot:body>
        </template>
        <template v-slot:footer>
        </template>
      </Modal>
    `,
    components: { Modal: Comp },
    emits: ["close_request"],
  }

  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  const mountModal = () => {
    wrapper = mount(TestComponent, {
      global: {
        plugins: [router],
      },
      attachTo: document.body,
    })
    return wrapper
  }

  it("click on note when doing review - close-button", async () => {
    wrapper = mountModal()
    await vi.waitUntil(() => document.querySelector(".close-button"), {
      timeout: 1000,
    })
    const closeButton = document.querySelector(".close-button") as HTMLElement
    expect(closeButton).toBeTruthy()
    closeButton.click()
    expect(wrapper.emitted().close_request).toHaveLength(1)
  })

  it("closes when ESC is pressed", async () => {
    wrapper = mountModal()
    await vi.waitUntil(() => document.querySelector(".modal-wrapper"), {
      timeout: 1000,
    })
    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
    expect(wrapper.emitted().close_request).toHaveLength(1)
  })

  it("click on note when doing review - mouse-click", async () => {
    wrapper = mountModal()
    await vi.waitUntil(() => document.querySelector(".modal-wrapper"), {
      timeout: 1000,
    })
    const modalWrapper = document.querySelector(".modal-wrapper") as HTMLElement
    expect(modalWrapper).toBeTruthy()
    modalWrapper.dispatchEvent(
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
      components: { Modal: Comp },
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
      global: { plugins: [router] },
      attachTo: document.body,
    })

    await vi.waitUntil(
      () => document.querySelectorAll(".modal-wrapper").length === 2,
      { timeout: 1000 }
    )

    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
    await wrapper.vm.$nextTick()

    expect(innerClosed).toHaveBeenCalledTimes(1)
    expect(outerClosed).not.toHaveBeenCalled()

    document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape" }))
    await wrapper.vm.$nextTick()

    expect(outerClosed).toHaveBeenCalledTimes(1)
  })
})
