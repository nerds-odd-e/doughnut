import AssimilateSingleNotePageView from "@/pages/AssimilateSingleNotePageView.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"

let renderer: RenderingHelper<typeof AssimilateSingleNotePageView>

beforeEach(() => {
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
