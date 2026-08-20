import { describe, it, expect } from "vitest"
import LoadingModal from "@/components/commons/LoadingModal.vue"
import Modal from "@/components/commons/Modal.vue"
import helper from "@tests/helpers"
import { defineComponent, nextTick, onMounted, ref } from "vue"

describe("LoadingModal top layer", () => {
  it("paints the spinner above an already-open native modal dialog", async () => {
    const HostDialog = defineComponent({
      components: { Modal, LoadingModal },
      setup() {
        const showLoading = ref(false)
        onMounted(() => {
          showLoading.value = true
        })
        return { showLoading }
      },
      template: `
        <Modal :show-close-button="false">
          <template #body>
            <div data-test="refine-host">Refine note host content</div>
          </template>
        </Modal>
        <LoadingModal
          :show="showLoading"
          message="AI is generating refinement layout..."
        />
      `,
    })
    const hostWrapper = helper.component(HostDialog).withRouter().render() as {
      unmount: () => void
    }
    try {
      await nextTick()
      await nextTick()
      const hit = document.elementFromPoint(
        Math.floor(window.innerWidth / 2),
        Math.floor(window.innerHeight / 2)
      ) as Element | null
      expect(hit?.closest(".loading-modal-mask")).toBeTruthy()
      expect(hit?.closest('[data-test="refine-host"]')).toBeNull()
    } finally {
      hostWrapper.unmount()
    }
  })
})
