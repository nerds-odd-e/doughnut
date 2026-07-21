import {
  AiController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
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
  note,
  refinementActionButton,
  refinementLayoutItems,
  refinementLayoutPanel,
  refinementLayoutSelectionApiCall,
  renderer,
  selectFirstLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement remove layout points", () => {
  describe("selection and confirmation", () => {
    it("shows checkboxes for each layout point", async () => {
      const wrapper = await mountNoteRefinementReady([
        "Point 1",
        "Point 2",
        "Point 3",
      ])
      expect(
        refinementLayoutPanel(wrapper).findAll('input[type="checkbox"]')
      ).toHaveLength(3)
    })

    it.each([
      { testId: "remove-refinement-layout" as const, action: "remove" },
      { testId: "extract-refinement-layout" as const, action: "extract" },
    ])(
      "disables $action button when no layout points are selected",
      async ({ testId }) => {
        const wrapper = await mountNoteRefinementReady(["Point 1", "Point 2"])
        expect(refinementActionButton(wrapper, testId).disabled).toBe(true)
      }
    )

    it.each([
      { testId: "remove-refinement-layout" as const, action: "remove" },
      { testId: "extract-refinement-layout" as const, action: "extract" },
    ])(
      "enables $action button when a layout point is selected",
      async ({ testId }) => {
        const wrapper = await mountNoteRefinementReady(["Point 1", "Point 2"])
        await selectFirstLayoutItem(wrapper)
        expect(refinementActionButton(wrapper, testId).disabled).toBe(false)
      }
    )

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

    it("calls API and emits contentUpdated when removal is confirmed", async () => {
      const removeLayoutSpy = mockSdkService(
        AiController,
        "removeRefinementSuggestion",
        {
          content: "Updated content",
        }
      )
      const updateDetailsSpy = mockSdkService(
        TextContentController,
        "updateNoteContent",
        makeMe.aNoteRealm.please()
      )
      const wrapper = await mountNoteRefinementWithFirstItemSelected()
      await clickRemoveRefinementLayout(wrapper)

      expect(removeLayoutSpy).toHaveBeenCalledWith(
        refinementLayoutSelectionApiCall(
          note.id,
          refinementLayoutItems(["Point 1", "Point 2"]),
          ["p1"]
        )
      )
      expect(updateDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: { content: "Updated content" },
      })
      expect(wrapper.emitted()).toHaveProperty("contentUpdated")
      expect(wrapper.emitted("contentUpdated")).toEqual([["Updated content"]])
    })

    it("clears selection and reloads layout after confirmed removal", async () => {
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
      mockSdkService(AiController, "removeRefinementSuggestion", {
        content: "Updated content",
      })
      mockSdkService(
        TextContentController,
        "updateNoteContent",
        makeMe.aNoteRealm.please()
      )
      const wrapper = renderer.withCleanStorage().withProps({ note }).mount()
      await flushPromises()
      expect(generateLayoutSpy).toHaveBeenCalledTimes(1)

      await selectFirstLayoutItem(wrapper)
      await clickRemoveRefinementLayout(wrapper)

      expect(generateLayoutSpy).toHaveBeenCalledTimes(2)
      expect(layoutCheckbox(wrapper, "p1").checked).toBe(false)
      expect(
        refinementActionButton(wrapper, "remove-refinement-layout").disabled
      ).toBe(true)
      expect(
        refinementLayoutPanel(wrapper).findAll('input[type="checkbox"]')
      ).toHaveLength(1)
    })

    it("does not save or emit contentUpdated when removal returns unchanged content", async () => {
      const noteWithContent = makeMe.aNote.content("Original content").please()
      const removeLayoutSpy = mockSdkService(
        AiController,
        "removeRefinementSuggestion",
        {
          content: "Original content",
        }
      )
      const updateDetailsSpy = mockSdkService(
        TextContentController,
        "updateNoteContent",
        makeMe.aNoteRealm.please()
      )
      const wrapper = await mountNoteRefinementWithFirstItemSelected(
        ["Point 1", "Point 2"],
        { note: noteWithContent }
      )
      await clickRemoveRefinementLayout(wrapper)

      expect(removeLayoutSpy).toHaveBeenCalled()
      expect(updateDetailsSpy).not.toHaveBeenCalled()
      expect(wrapper.emitted()).not.toHaveProperty("contentUpdated")
    })
  })
})
