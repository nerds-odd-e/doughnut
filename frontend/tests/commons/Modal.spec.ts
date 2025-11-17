import Modal from "@/components/commons/Modal.vue"
import { mount } from "@vue/test-utils"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRoute: () => ({ path: "/" }),
  useRouter: () => ({
    push: mockedPush,
  }),
}))

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
