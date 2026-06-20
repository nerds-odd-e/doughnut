import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import { beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import {
  extractNoteButtonTitle,
  mountNoteRefinement,
  mountNoteRefinementWithLayout,
  note,
  refinementLayoutSelectionApiCall,
  refinementLayoutItems,
  selectRefinementLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

const routerReplace = vi.fn()

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      replace: routerReplace,
    }),
  }
})

setupNoteRefinementTests()

describe("NoteRefinement extract note", () => {
  beforeEach(() => {
    routerReplace.mockResolvedValue(undefined)
  })

  describe("dialog-level action", () => {
    it("displays one extract button and no per-item extract buttons", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      const listItems = wrapper.findAll("li")
      expect(listItems).toHaveLength(3)
      listItems.forEach((li) => {
        expect(li.findAll("button")).toHaveLength(0)
      })
      const extractButtons = wrapper.findAll(
        `button[title="${extractNoteButtonTitle}"]`
      )
      expect(extractButtons).toHaveLength(1)
    })

    it("extracts the selected suggestion and navigates to the new note", async () => {
      const createdRealm = makeMe.aNoteRealm.please()
      const extractNoteSpy = mockSdkService(
        AiController,
        "extractNote",
        createdRealm
      )
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      await selectRefinementLayoutItem(wrapper, "p2")
      await wrapper
        .find(`button[title="${extractNoteButtonTitle}"]`)
        .trigger("click")
      await flushPromises()

      expect(extractNoteSpy).toHaveBeenCalledWith(
        refinementLayoutSelectionApiCall(
          note.id,
          refinementLayoutItems(["Point 1", "Point 2", "Point 3"]),
          ["p2"]
        )
      )
      expect(wrapper.findAll("li")).toHaveLength(3)
      expect(routerReplace).toHaveBeenCalledWith(
        noteShowLocation(createdRealm.id)
      )
    })

    it("extracts multiple selected layout points into one note", async () => {
      const createdRealm = makeMe.aNoteRealm.please()
      const extractNoteSpy = mockSdkService(
        AiController,
        "extractNote",
        createdRealm
      )
      const layout = refinementLayoutItems(["Point 1", "Point 2", "Point 3"])
      const wrapper = mountNoteRefinementWithLayout(layout)
      await flushPromises()

      await selectRefinementLayoutItem(wrapper, "p1")
      await selectRefinementLayoutItem(wrapper, "p3")
      await wrapper
        .find(`button[title="${extractNoteButtonTitle}"]`)
        .trigger("click")
      await flushPromises()

      expect(extractNoteSpy).toHaveBeenCalledWith(
        refinementLayoutSelectionApiCall(note.id, layout, ["p1", "p3"])
      )
      expect(routerReplace).toHaveBeenCalledWith(
        noteShowLocation(createdRealm.id)
      )
    })

    it("keeps suggestion in list when API fails", async () => {
      mockSdkService(AiController, "extractNote", undefined).mockResolvedValue(
        wrapSdkError("API Error")
      )
      const wrapper = mountNoteRefinement(["Test Point"])
      await flushPromises()

      await selectRefinementLayoutItem(wrapper, "p1")
      await wrapper
        .find(`button[title="${extractNoteButtonTitle}"]`)
        .trigger("click")
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

      await selectRefinementLayoutItem(wrapper, "p1")
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

      await selectRefinementLayoutItem(wrapper, "p1")
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
