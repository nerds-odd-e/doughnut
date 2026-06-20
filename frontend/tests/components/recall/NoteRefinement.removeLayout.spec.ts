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
  layoutCheckbox,
  mountNoteRefinement,
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
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()
      expect(
        refinementLayoutPanel(wrapper).findAll('input[type="checkbox"]')
      ).toHaveLength(3)
    })

    it("disables remove button when no layout points are selected", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      expect(
        refinementActionButton(wrapper, "remove-refinement-layout").disabled
      ).toBe(true)
    })

    it("disables extract button when no layout points are selected", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      expect(
        refinementActionButton(wrapper, "extract-refinement-layout").disabled
      ).toBe(true)
    })

    it("enables remove button when a layout point is selected", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstLayoutItem(wrapper)
      expect(
        refinementActionButton(wrapper, "remove-refinement-layout").disabled
      ).toBe(false)
    })

    it("enables extract button when a layout point is selected", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstLayoutItem(wrapper)
      expect(
        refinementActionButton(wrapper, "extract-refinement-layout").disabled
      ).toBe(false)
    })

    it("shows confirmation dialog when remove button is clicked", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstLayoutItem(wrapper)
      await wrapper
        .find('[data-test-id="remove-refinement-layout"]')
        .trigger("click")
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups).toHaveLength(1)
      expect(popups[0]!.type).toBe("confirm")
      expect(popups[0]!.message).toContain("remove")
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
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstLayoutItem(wrapper)
      await wrapper
        .find('[data-test-id="remove-refinement-layout"]')
        .trigger("click")
      usePopups().popups.done(true)
      await flushPromises()

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
      await wrapper
        .find('[data-test-id="remove-refinement-layout"]')
        .trigger("click")
      usePopups().popups.done(true)
      await flushPromises()

      expect(generateLayoutSpy).toHaveBeenCalledTimes(2)
      expect(layoutCheckbox(wrapper, "p1").checked).toBe(false)
      expect(
        refinementActionButton(wrapper, "remove-refinement-layout").disabled
      ).toBe(true)
      expect(
        refinementLayoutPanel(wrapper).findAll('input[type="checkbox"]')
      ).toHaveLength(1)
    })

    it("does not call API when removal is cancelled", async () => {
      const removeLayoutSpy = mockSdkService(
        AiController,
        "removeRefinementSuggestion",
        {
          content: "Updated content",
        }
      )
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstLayoutItem(wrapper)
      await wrapper
        .find('[data-test-id="remove-refinement-layout"]')
        .trigger("click")
      usePopups().popups.done(false)

      expect(removeLayoutSpy).not.toHaveBeenCalled()
      expect(wrapper.emitted()).not.toHaveProperty("contentUpdated")
    })
  })
})
