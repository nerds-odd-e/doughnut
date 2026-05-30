import {
  AiController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
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
  checklist,
  mountNoteRefinement,
  note,
  selectFirstCheckpoint,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement delete understanding points", () => {
  describe("selection and confirmation", () => {
    it("shows checkboxes for each understanding point", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()
      expect(checklist(wrapper).findAll('input[type="checkbox"]')).toHaveLength(
        3
      )
    })

    it("disables delete button when no points are selected", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      expect(
        (
          wrapper.find('[data-test-id="delete-understanding-points"]')
            .element as HTMLButtonElement
        ).disabled
      ).toBe(true)
    })

    it("enables delete button when a point is selected", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      expect(
        (
          wrapper.find('[data-test-id="delete-understanding-points"]')
            .element as HTMLButtonElement
        ).disabled
      ).toBe(false)
    })

    it("shows confirmation dialog when delete button is clicked", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups).toHaveLength(1)
      expect(popups[0]!.type).toBe("confirm")
      expect(popups[0]!.message).toContain("delete")
    })

    it("calls API and emits contentUpdated when deletion is confirmed", async () => {
      const deletePointsSpy = mockSdkService(
        AiController,
        "removePointFromNote",
        {
          content: "Updated content",
        }
      )
      const updateDetailsSpy = mockSdkService(
        TextContentController,
        "updateNoteContent",
        makeMe.aNoteRealm.please()
      )
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()

      expect(deletePointsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { points: ["Point 1"] },
      })
      expect(updateDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { content: "Updated content" },
      })
      expect(wrapper.emitted()).toHaveProperty("contentUpdated")
      expect(wrapper.emitted("contentUpdated")).toEqual([["Updated content"]])
    })

    it("does not call API when deletion is cancelled", async () => {
      const deletePointsSpy = mockSdkService(
        AiController,
        "removePointFromNote",
        {
          content: "Updated content",
        }
      )
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(false)
      await flushPromises()

      expect(deletePointsSpy).not.toHaveBeenCalled()
      expect(wrapper.emitted()).not.toHaveProperty("contentUpdated")
    })
  })

  describe("loading modal", () => {
    it("shows LoadingModal while deleting points", async () => {
      let resolveApi: () => void
      mockSdkServiceWithImplementation(
        AiController,
        "removePointFromNote",
        async () => {
          await new Promise<void>((r) => {
            resolveApi = r
          })
          return { content: "Updated content" }
        }
      )
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      expect(document.body.textContent).toContain("AI is removing content...")
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })

    it("hides LoadingModal when delete API fails", async () => {
      let resolveApi: () => void
      mockSdkServiceWithImplementation(
        AiController,
        "removePointFromNote",
        async () => {
          await new Promise<void>((r) => {
            resolveApi = r
          })
          return wrapSdkError("API Error")
        }
      )
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstCheckpoint(wrapper)
      await wrapper
        .find('[data-test-id="delete-understanding-points"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })
  })
})
