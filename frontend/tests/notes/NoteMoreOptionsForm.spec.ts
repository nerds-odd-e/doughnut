import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import NoteMoreOptionsForm from "@/components/notes/widgets/NoteMoreOptionsForm.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, productionRouterAt } from "@tests/helpers"
import type RenderingHelper from "@tests/helpers/RenderingHelper"
import type { Router } from "vue-router"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { voiceInputIsActive } from "@/composables/useNoteVoiceInput"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import type { ApiStatus } from "@/managedApi/ApiStatusHandler"
import { setupGlobalClient } from "@/managedApi/clientSetup"

let renderer: RenderingHelper<typeof NoteMoreOptionsForm>
let router: Router
const apiStatus: ApiStatus = { states: [] }

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
})

beforeEach(async () => {
  useAssimilationView().dismiss()
  voiceInputIsActive.value = false
  setupGlobalClient(apiStatus)
  mockSdkService(NoteController, "trashNote", undefined)
  router = await productionRouterAt({ name: "root" })
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
        wrapper
          .find(`button[title="${noteMoreOptionsTitles.voiceInput}"]`)
          .exists()
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

  describe("voice input", () => {
    const voiceInputItem = (wrapper: ReturnType<typeof renderer.mount>) =>
      wrapper.find(`button[title="${noteMoreOptionsTitles.voiceInput}"]`)

    it("starts voice input and closes the menu", async () => {
      const wrapper = renderer.withProps({ note }).mount()
      await flushPromises()

      await voiceInputItem(wrapper).trigger("click")

      expect(wrapper.emitted()).toHaveProperty("start-voice-input")
      expect(wrapper.emitted()).toHaveProperty("close-dialog")
    })

    it("omits Voice input from the menu while recording", async () => {
      voiceInputIsActive.value = true

      const wrapper = renderer.withProps({ note }).mount()
      await flushPromises()

      expect(voiceInputItem(wrapper).exists()).toBe(false)
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
    it("turns assimilation settings on without changing route and closes the menu", async () => {
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
