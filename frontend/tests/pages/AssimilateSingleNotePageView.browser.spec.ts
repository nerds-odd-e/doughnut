import AssimilateSingleNotePageView from "@/pages/AssimilateSingleNotePageView.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"

let renderer: RenderingHelper<typeof AssimilateSingleNotePageView>

beforeEach(() => {
  const noteRealm = makeMe.aNoteRealm.please()
  mockSdkService("getNoteInfo", {
    note: noteRealm,
    recallSetting: {
      level: 0,
      rememberSpelling: false,
      skipMemoryTracking: false,
    },
    memoryTrackers: [],
    createdAt: "",
    noteType: undefined,
  })
  mockSdkService("showNoteAccessory", {})
  mockSdkService("showNote", noteRealm)
  mockSdkService("generateUnderstandingChecklist", { points: [] })
  // Suppress Vue warnings about emitted events (false positives)
  vi.spyOn(console, "warn").mockImplementation(() => {
    // Intentionally empty to suppress warnings
  })
  renderer = helper
    .component(AssimilateSingleNotePageView)
    .withRouter()
    .withCleanStorage()
})

describe("AssimilateSingleNotePageView", () => {
  const note = makeMe.aNote.please()

  describe("rendering", () => {
    it("displays ContentLoader when note is not provided", async () => {
      const wrapper = renderer.withProps({ note: undefined }).mount()

      await flushPromises()

      const loader = wrapper.findComponent({ name: "ContentLoader" })
      expect(loader.exists()).toBe(true)
    })

    it("displays Assimilation component when note is provided", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilation = wrapper.findComponent({ name: "Assimilation" })
      expect(assimilation.exists()).toBe(true)
    })

    it("displays GlobalBar with title", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const globalBar = wrapper.findComponent({ name: "GlobalBar" })
      expect(globalBar.exists()).toBe(true)
      expect(wrapper.text()).toContain("Assimilate Note")
    })
  })

  describe("events", () => {
    it("emits initial-review-done when Assimilation emits the event", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilation = wrapper.findComponent({ name: "Assimilation" })
      assimilation.vm.$emit("initial-review-done")

      await flushPromises()

      expect(wrapper.emitted()).toHaveProperty("initial-review-done")
    })

    it("emits reload-needed when Assimilation emits the event", async () => {
      const wrapper = renderer.withProps({ note }).mount()

      await flushPromises()

      const assimilation = wrapper.findComponent({ name: "Assimilation" })
      assimilation.vm.$emit("reload-needed")

      await flushPromises()

      expect(wrapper.emitted()).toHaveProperty("reload-needed")
    })
  })
})
