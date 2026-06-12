import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteMoreOptionsForm from "@/components/notes/widgets/NoteMoreOptionsForm.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import usePopups from "@/components/commons/Popups/usePopups"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { useAssimilationView } from "@/composables/useAssimilationView"
import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import { setupGlobalClient } from "@/managedApi/clientSetup"

const mockToast = {
  error: vi.fn(),
  warning: vi.fn(),
}

vi.mock("vue-toastification", () => ({
  useToast: () => mockToast,
}))

let renderer: RenderingHelper<typeof NoteMoreOptionsForm>
let router: ReturnType<typeof createRouter>
const apiStatus: ApiStatus = { states: [] }

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
})

beforeEach(() => {
  useAssimilationView().dismiss()
  usePopups().popups.register({ popupInfo: [] })
  setupGlobalClient(apiStatus)
  mockToast.error.mockClear()
  mockToast.warning.mockClear()
  mockSdkService(NoteController, "deleteNote", undefined)
  router = createRouter({
    history: createWebHistory(),
    routes,
  })
  renderer = helper
    .component(NoteMoreOptionsForm)
    .withRouter(router)
    .withCleanStorage()
})

describe("NoteMoreOptionsForm", () => {
  const note = makeMe.aNote.please()

  describe("action buttons", () => {
    it("displays all action buttons", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      expect(wrapper.find('button[title="Export..."]').exists()).toBe(true)
      expect(wrapper.get("ul").text()).toContain("Export...")
      expect(
        wrapper.find('button[title="Questions for the note"]').exists()
      ).toBe(true)
      expect(wrapper.get("ul").text()).toContain("Questions for the note")
      expect(
        wrapper.find('button[title="Assimilation settings"]').exists()
      ).toBe(true)
      expect(wrapper.get("ul").text()).toContain("Assimilation settings")
      expect(wrapper.find('button[title="Delete note"]').exists()).toBe(true)
      expect(wrapper.get("ul").text()).toContain("Delete note")
    })
  })

  describe("assimilation settings toggle", () => {
    it("turns assimilation settings on and shows check without changing route", async () => {
      await router.push("/")
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilateButton = wrapper.find(
        'button[title="Assimilation settings"]'
      )
      await assimilateButton.trigger("click")

      await flushPromises()

      expect(router.currentRoute.value.path).toBe("/")
      const { showAssimilationSettings, targetNoteId } = useAssimilationView()
      expect(showAssimilationSettings.value).toBe(true)
      expect(targetNoteId.value).toBe(note.id)
    })

    it("emits close-dialog when assimilation settings button is clicked", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilateButton = wrapper.find(
        'button[title="Assimilation settings"]'
      )
      await assimilateButton.trigger("click")

      expect(wrapper.emitted()).toHaveProperty("close-dialog")
    })

    it("turns assimilation settings off when toggled while already on", async () => {
      const { openForNote } = useAssimilationView()
      openForNote(note.id)

      const wrapper = renderer.withProps({ note }).mount()
      await flushPromises()

      const assimilateButton = wrapper.find(
        'button[title="Assimilation settings"]'
      )
      await assimilateButton.trigger("click")
      await flushPromises()

      const { showAssimilationSettings, targetNoteId } = useAssimilationView()
      expect(showAssimilationSettings.value).toBe(false)
      expect(targetNoteId.value).toBe(null)
    })
  })
})
