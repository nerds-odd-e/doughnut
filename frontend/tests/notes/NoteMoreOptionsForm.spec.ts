import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import NoteMoreOptionsForm from "@/components/notes/widgets/NoteMoreOptionsForm.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import usePopups from "@/components/commons/Popups/usePopups"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useNoteToolbarPanel } from "@/composables/useNoteToolbarPanel"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
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
  useNoteToolbarPanel().close()
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

      expect(
        wrapper.find(`button[title="${noteMoreOptionsTitles.export}"]`).exists()
      ).toBe(true)
      expect(
        wrapper.find(`button[title="${noteMoreOptionsTitles.mcqs}"]`).exists()
      ).toBe(true)
      expect(
        wrapper.find(`button[title="${noteMoreOptionsTitles.audio}"]`).exists()
      ).toBe(true)
      expect(
        wrapper
          .find(`button[title="${noteMoreOptionsTitles.assimilation}"]`)
          .exists()
      ).toBe(true)
      expect(
        wrapper.find(`button[title="${noteMoreOptionsTitles.delete}"]`).exists()
      ).toBe(true)
    })
  })

  describe("audio tools toggle", () => {
    it("opens the audio tools panel", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const audioButton = wrapper.find(
        `button[title="${noteMoreOptionsTitles.audio}"]`
      )
      await audioButton.trigger("click")
      await flushPromises()

      expect(useNoteToolbarPanel().isAudioOpen.value).toBe(true)
    })

    it("emits close-dialog when audio tools button is clicked", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const audioButton = wrapper.find(
        `button[title="${noteMoreOptionsTitles.audio}"]`
      )
      await audioButton.trigger("click")

      expect(wrapper.emitted()).toHaveProperty("close-dialog")
    })

    it("omits audio from the menu when audio is already on", async () => {
      useNoteToolbarPanel().toggleAudio()

      const wrapper = renderer.withProps({ note }).mount()
      await flushPromises()

      expect(
        wrapper.find(`button[title="${noteMoreOptionsTitles.audio}"]`).exists()
      ).toBe(false)
    })
  })

  describe("refine note action", () => {
    it("opens the refine note modal and closes the menu when clicked", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const refineButton = wrapper.find(
        `button[title="${noteMoreOptionsTitles.refine}"]`
      )
      await refineButton.trigger("click")
      await flushPromises()

      const modalEl = document.querySelector('[data-test="refine-note-modal"]')
      expect(modalEl?.classList.contains("daisy-modal-open")).toBe(true)
      expect(wrapper.emitted()).toHaveProperty("close-dialog")
    })
  })

  describe("assimilation settings toggle", () => {
    it("turns assimilation settings on without changing route", async () => {
      await router.push("/")
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilateButton = wrapper.find(
        `button[title="${noteMoreOptionsTitles.assimilation}"]`
      )
      await assimilateButton.trigger("click")

      await flushPromises()

      expect(router.currentRoute.value.path).toBe("/")
      const { showAssimilationPanel, targetNoteId } = useAssimilationView()
      expect(showAssimilationPanel.value).toBe(true)
      expect(targetNoteId.value).toBe(note.id)
    })

    it("emits close-dialog when assimilation settings button is clicked", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilateButton = wrapper.find(
        `button[title="${noteMoreOptionsTitles.assimilation}"]`
      )
      await assimilateButton.trigger("click")

      expect(wrapper.emitted()).toHaveProperty("close-dialog")
    })

    it("omits assimilation from the menu when already on", async () => {
      const { openForNote } = useAssimilationView()
      openForNote(note.id)

      const wrapper = renderer.withProps({ note }).mount()
      await flushPromises()

      expect(
        wrapper
          .find(`button[title="${noteMoreOptionsTitles.assimilation}"]`)
          .exists()
      ).toBe(false)
    })
  })
})
