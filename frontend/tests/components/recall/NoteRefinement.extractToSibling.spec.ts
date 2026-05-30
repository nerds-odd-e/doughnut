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
import usePopups from "@/components/commons/Popups/usePopups"
import {
  extractToSiblingButtonTitle,
  mountNoteRefinement,
  note,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement extract to sibling", () => {
  describe("per-point action", () => {
    it("displays sibling extract button for each point", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      const listItems = wrapper.findAll("li")
      expect(listItems).toHaveLength(3)
      listItems.forEach((li, index) => {
        expect(li.text()).toContain(["Point 1", "Point 2", "Point 3"][index])
        const buttons = li.findAll("button")
        expect(buttons).toHaveLength(1)
        expect(buttons[0]!.attributes("title")).toBe(
          extractToSiblingButtonTitle
        )
      })
    })

    it("calls promotePointToSibling API when sibling button is clicked", async () => {
      const promotePointToSiblingSpy = mockSdkService(
        AiController,
        "promotePointToSibling",
        makeMe.aNoteRealm.please()
      )
      const wrapper = mountNoteRefinement(["Test Point"])
      await flushPromises()

      await wrapper.find("li button").trigger("click")
      await flushPromises()

      expect(promotePointToSiblingSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { points: ["Test Point"] },
      })
    })

    it("removes point from checklist after successful extraction", async () => {
      mockSdkService(
        AiController,
        "promotePointToSibling",
        makeMe.aNoteRealm.please()
      )
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      await wrapper.findAll("li")[1]!.find("button").trigger("click")
      await flushPromises()

      expect(wrapper.findAll("li")).toHaveLength(2)
      expect(wrapper.text()).toContain("Point 1")
      expect(wrapper.text()).toContain("Point 3")
      expect(wrapper.text()).not.toContain("Point 2")
    })

    it("keeps point in checklist when API fails", async () => {
      mockSdkService(
        AiController,
        "promotePointToSibling",
        undefined
      ).mockResolvedValue(wrapSdkError("API Error"))
      const wrapper = mountNoteRefinement(["Test Point"])
      await flushPromises()

      await wrapper.find("li button").trigger("click")
      await flushPromises()

      expect(wrapper.findAll("li")).toHaveLength(1)
      expect(wrapper.text()).toContain("Test Point")
    })
  })

  describe("loading modal", () => {
    it("shows LoadingModal while extracting to sibling", async () => {
      let resolveApi: () => void
      const apiPromise = new Promise<void>((r) => {
        resolveApi = r
      })
      mockSdkServiceWithImplementation(
        AiController,
        "promotePointToSibling",
        async () => {
          await apiPromise
          return makeMe.aNoteRealm.please()
        }
      )
      const wrapper = mountNoteRefinement(["Test understanding point"])
      await flushPromises()

      await wrapper
        .find(`button[title="${extractToSiblingButtonTitle}"]`)
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
      mockSdkServiceWithImplementation(
        AiController,
        "promotePointToSibling",
        async () => {
          await new Promise<void>((r) => {
            resolveApi = r
          })
          return wrapSdkError({})
        }
      )
      const wrapper = mountNoteRefinement(["Test understanding point"])
      await flushPromises()

      await wrapper
        .find(`button[title="${extractToSiblingButtonTitle}"]`)
        .trigger("click")
      await nextTick()
      await flushPromises()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      resolveApi!()
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })
  })
})
