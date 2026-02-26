import AssimilateSingleNotePage from "@/pages/AssimilateSingleNotePage.vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

let renderer: RenderingHelper<typeof AssimilateSingleNotePage>
let router: ReturnType<typeof createRouter>
let showNoteSpy: ReturnType<typeof mockSdkService<"showNote">>

beforeEach(() => {
  const noteRealm = makeMe.aNoteRealm.please()
  showNoteSpy = mockSdkService("showNote", noteRealm)
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
  mockSdkService("generateUnderstandingChecklist", { points: [] })
  router = createRouter({
    history: createWebHistory(),
    routes,
  })
  renderer = helper
    .component(AssimilateSingleNotePage)
    .withRouter(router)
    .withCleanStorage()
})

describe("AssimilateSingleNotePage", () => {
  const noteId = 123

  describe("initialization", () => {
    it("fetches note on mount", async () => {
      renderer.withProps({ noteId }).mount()

      await flushPromises()

      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: noteId },
      })
    })

    it("displays the note view after loading", async () => {
      const wrapper = renderer.withProps({ noteId }).mount()

      await flushPromises()

      const pageView = wrapper.findComponent({
        name: "AssimilateSingleNotePageView",
      })
      expect(pageView.exists()).toBe(true)
    })
  })

  describe("navigation", () => {
    it("navigates to note show page after assimilation is done", async () => {
      const wrapper = renderer.withProps({ noteId }).mount()

      await flushPromises()

      const pageView = wrapper.findComponent({
        name: "AssimilateSingleNotePageView",
      })
      pageView.vm.$emit("assimilation-done")

      await flushPromises()

      expect(router.currentRoute.value.name).toBe("noteShow")
      expect(router.currentRoute.value.params.noteId).toBe(String(noteId))
    })
  })

  describe("reload", () => {
    it("reloads note when reload is needed", async () => {
      const wrapper = renderer.withProps({ noteId }).mount()

      await flushPromises()
      showNoteSpy.mockClear()

      const pageView = wrapper.findComponent({
        name: "AssimilateSingleNotePageView",
      })
      pageView.vm.$emit("reload-needed")

      await flushPromises()

      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: noteId },
      })
    })
  })
})
