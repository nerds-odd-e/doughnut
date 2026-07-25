import { describe, it, expect } from "vitest"
import { page } from "vitest/browser"
import { render } from "@testing-library/vue"
import LoadingModal from "@/components/commons/LoadingModal.vue"
import Modal from "@/components/commons/Modal.vue"
import helper from "@tests/helpers"
import { defineComponent, nextTick } from "vue"

describe("LoadingModal top layer", () => {
  it("paints the spinner above an already-open native modal dialog", async () => {
    const originalWidth = window.innerWidth
    const originalHeight = window.innerHeight
    const HostDialog = defineComponent({
      components: { Modal },
      template: `
        <Modal :show-close-button="false">
          <template #body>
            <div data-test="refine-host">Refine note host content</div>
          </template>
        </Modal>
      `,
    })
    const hostWrapper = helper.component(HostDialog).withRouter().render() as {
      unmount: () => void
    }
    const { unmount: unmountLoading } = render(LoadingModal, {
      props: { show: true, message: "AI is generating layout..." },
    })
    try {
      await page.viewport(1280, 720)
      await nextTick()
      const hit = document.elementFromPoint(640, 360) as Element | null
      expect(hit?.closest(".loading-modal-mask")).toBeTruthy()
      expect(hit?.closest('[data-test="refine-host"]')).toBeNull()
    } finally {
      hostWrapper.unmount()
      unmountLoading()
      await page.viewport(originalWidth, originalHeight)
    }
  })
})
