import {
  AiController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  clickRemoveRefinementLayout,
  expectRemoveConfirmPopup,
  openRemoveRefinementConfirmDialog,
} from "./noteRefinementRemoveTestSupport"
import {
  layoutCheckbox,
  mountNoteRefinementReady,
  mountNoteRefinementWithFirstItemSelected,
  refinementActionButton,
  refinementLayoutItems,
  refinementLayoutPanel,
  refinementLayoutSelectionApiCall,
  renderer,
  selectFirstLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement remove refinement layout items", () => {
  describe("selection and confirmation", () => {
    it("shows checkboxes and toggles remove/extract button disabled state with selection", async () => {
      const wrapper = await mountNoteRefinementReady([
        "Point 1",
        "Point 2",
        "Point 3",
      ])
      expect(
        refinementLayoutPanel(wrapper).findAll('input[type="checkbox"]')
      ).toHaveLength(3)
      expect(
        refinementActionButton(wrapper, "remove-refinement-layout").disabled
      ).toBe(true)
      expect(
        refinementActionButton(wrapper, "extract-refinement-layout").disabled
      ).toBe(true)

      await selectFirstLayoutItem(wrapper)
      expect(
        refinementActionButton(wrapper, "remove-refinement-layout").disabled
      ).toBe(false)
      expect(
        refinementActionButton(wrapper, "extract-refinement-layout").disabled
      ).toBe(false)
    })

    it("shows confirmation dialog and does not call API when removal is cancelled", async () => {
      const removeLayoutSpy = mockSdkService(
        AiController,
        "removeRefinementSuggestion",
        {
          content: "Updated content",
        }
      )
      const wrapper = await mountNoteRefinementWithFirstItemSelected()
      await openRemoveRefinementConfirmDialog(wrapper)
      expectRemoveConfirmPopup()
      usePopups().popups.done(false)

      expect(removeLayoutSpy).not.toHaveBeenCalled()
      expect(wrapper.emitted()).not.toHaveProperty("contentUpdated")
    })

    it("skips unchanged removal content, then saves and reloads changed content", async () => {
      const noteWithContent = makeMe.aNote.content("Original content").please()
      const initialLayout = refinementLayoutItems(["Point 1", "Point 2"])
      const postRemovalLayout = refinementLayoutItems(["Point 1"])
      const generateLayoutSpy = mockSdkServiceWithImplementation(
        AiController,
        "generateRefinementSuggestions",
        vi
          .fn()
          .mockResolvedValueOnce({ items: initialLayout })
          .mockResolvedValueOnce({ items: postRemovalLayout })
      )
      const removeLayoutSpy = mockSdkServiceWithImplementation(
        AiController,
        "removeRefinementSuggestion",
        vi
          .fn()
          .mockResolvedValueOnce({ content: "Original content" })
          .mockResolvedValueOnce({ content: "Updated content" })
      )
      const updateDetailsSpy = mockSdkService(
        TextContentController,
        "updateNoteContent",
        makeMe.aNoteRealm.please()
      )
      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note: noteWithContent })
        .mount()
      await flushPromises()
      expect(generateLayoutSpy).toHaveBeenCalledTimes(1)

      await selectFirstLayoutItem(wrapper)
      await clickRemoveRefinementLayout(wrapper)

      expect(updateDetailsSpy).not.toHaveBeenCalled()
      expect(wrapper.emitted()).not.toHaveProperty("contentUpdated")
      expect(generateLayoutSpy).toHaveBeenCalledTimes(1)

      await clickRemoveRefinementLayout(wrapper)

      expect(removeLayoutSpy).toHaveBeenCalledWith(
        refinementLayoutSelectionApiCall(noteWithContent.id, initialLayout, [
          "p1",
        ])
      )
      expect(removeLayoutSpy).toHaveBeenCalledTimes(2)
      expect(updateDetailsSpy).toHaveBeenCalledWith({
        path: { note: noteWithContent.id },
        body: { content: "Updated content" },
      })
      expect(wrapper.emitted()).toHaveProperty("contentUpdated")
      expect(wrapper.emitted("contentUpdated")).toEqual([["Updated content"]])
      expect(generateLayoutSpy).toHaveBeenCalledTimes(2)
      expect(layoutCheckbox(wrapper, "p1").checked).toBe(false)
      expect(
        refinementActionButton(wrapper, "remove-refinement-layout").disabled
      ).toBe(true)
      expect(
        refinementLayoutPanel(wrapper).findAll('input[type="checkbox"]')
      ).toHaveLength(1)
    })
  })
})
