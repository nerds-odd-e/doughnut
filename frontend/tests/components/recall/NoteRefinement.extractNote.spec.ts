import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import {
  extractNoteButtonTitle,
  mountNoteRefinement,
  note,
  refinementSuggestionsApiCall,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement extract note", () => {
  describe("per-suggestion action", () => {
    it("displays extract note button for each suggestion", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      const listItems = wrapper.findAll("li")
      expect(listItems).toHaveLength(3)
      listItems.forEach((li, index) => {
        expect(li.text()).toContain(["Point 1", "Point 2", "Point 3"][index])
        const buttons = li.findAll("button")
        expect(buttons).toHaveLength(1)
        expect(buttons[0]!.attributes("title")).toBe(extractNoteButtonTitle)
      })
    })

    it("calls extractNote API when extract button is clicked", async () => {
      const extractNoteSpy = mockSdkService(
        AiController,
        "extractNote",
        makeMe.aNoteRealm.please()
      )
      const wrapper = mountNoteRefinement(["Test Point"])
      await flushPromises()

      await wrapper.find("li button").trigger("click")
      await flushPromises()

      expect(extractNoteSpy).toHaveBeenCalledWith(
        refinementSuggestionsApiCall(note.id, ["Test Point"])
      )
    })

    it("removes suggestion from list after successful extraction", async () => {
      mockSdkService(AiController, "extractNote", makeMe.aNoteRealm.please())
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      await wrapper.findAll("li")[1]!.find("button").trigger("click")
      await flushPromises()

      expect(wrapper.findAll("li")).toHaveLength(2)
      expect(wrapper.text()).toContain("Point 1")
      expect(wrapper.text()).toContain("Point 3")
      expect(wrapper.text()).not.toContain("Point 2")
    })

    it("keeps suggestion in list when API fails", async () => {
      mockSdkService(AiController, "extractNote", undefined).mockResolvedValue(
        wrapSdkError("API Error")
      )
      const wrapper = mountNoteRefinement(["Test Point"])
      await flushPromises()

      await wrapper.find("li button").trigger("click")
      await flushPromises()

      expect(wrapper.findAll("li")).toHaveLength(1)
      expect(wrapper.text()).toContain("Test Point")
    })
  })

  describe("loading modal", () => {
    it("shows LoadingModal while extracting note", async () => {
      let resolveApi: () => void
      const apiPromise = new Promise<void>((r) => {
        resolveApi = r
      })
      mockSdkServiceWithImplementation(
        AiController,
        "extractNote",
        async () => {
          await apiPromise
          return makeMe.aNoteRealm.please()
        }
      )
      const wrapper = mountNoteRefinement(["Test refinement suggestion"])
      await flushPromises()

      await wrapper
        .find(`button[title="${extractNoteButtonTitle}"]`)
        .trigger("click")
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      expect(document.body.textContent).toContain("AI is creating note...")
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })

    it("hides LoadingModal when API fails", async () => {
      let resolveApi: () => void
      const apiGate = new Promise<void>((r) => {
        resolveApi = r
      })
      mockSdkServiceWithImplementation(
        AiController,
        "extractNote",
        async () => {
          await apiGate
          return wrapSdkError({})
        }
      )
      const wrapper = mountNoteRefinement(["Test refinement suggestion"])
      await flushPromises()

      await wrapper
        .find(`button[title="${extractNoteButtonTitle}"]`)
        .trigger("click")
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })
  })
})
