import Modal from "@/components/commons/Modal.vue"
import routes from "@/routes/routes"
import { mount } from "@vue/test-utils"
import { vi } from "vitest"
import { createRouter, createWebHistory } from "vue-router"

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

  const mountWithoutTeleport = () =>
    mount(TestComponent, {
      global: {
        plugins: [router],
        stubs: {
          Teleport: true, // Stub the Teleport component
        },
      },
    })

  it("click on note when doing review - close-button", async () => {
    const wrapper = mountWithoutTeleport()
    expect(wrapper.find(".close-button").exists()).toBe(true)
    await wrapper.find(".close-button").trigger("click")
    expect(wrapper.emitted().close_request).toHaveLength(1)
  })

  it("click on note when doing review - mouse-click", async () => {
    const wrapper = mountWithoutTeleport()
    await wrapper.find(".modal-wrapper").trigger("mousedown")
    expect(wrapper.emitted().close_request).toHaveLength(1)
  })
})
