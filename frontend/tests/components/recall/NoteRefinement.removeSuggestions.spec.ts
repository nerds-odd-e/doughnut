import {
  AiController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  mountNoteRefinement,
  note,
  refinementActionButton,
  refinementLayoutItems,
  refinementLayoutSelectionApiCall,
  refinementSuggestionsPanel,
  selectFirstSuggestion,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement remove refinement suggestions", () => {
  describe("selection and confirmation", () => {
    it("shows checkboxes for each refinement suggestion", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()
      expect(
        refinementSuggestionsPanel(wrapper).findAll('input[type="checkbox"]')
      ).toHaveLength(3)
    })

    it("disables remove button when no suggestions are selected", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      expect(
        refinementActionButton(wrapper, "remove-refinement-suggestions")
          .disabled
      ).toBe(true)
    })

    it("disables extract button when no suggestions are selected", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      expect(
        refinementActionButton(wrapper, "extract-refinement-suggestions")
          .disabled
      ).toBe(true)
    })

    it("enables remove button when a suggestion is selected", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstSuggestion(wrapper)
      expect(
        refinementActionButton(wrapper, "remove-refinement-suggestions")
          .disabled
      ).toBe(false)
    })

    it("enables extract button when a suggestion is selected", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstSuggestion(wrapper)
      expect(
        refinementActionButton(wrapper, "extract-refinement-suggestions")
          .disabled
      ).toBe(false)
    })

    it("shows confirmation dialog when remove button is clicked", async () => {
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstSuggestion(wrapper)
      await wrapper
        .find('[data-test-id="remove-refinement-suggestions"]')
        .trigger("click")
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups).toHaveLength(1)
      expect(popups[0]!.type).toBe("confirm")
      expect(popups[0]!.message).toContain("remove")
    })

    it("calls API and emits contentUpdated when removal is confirmed", async () => {
      const removeSuggestionsSpy = mockSdkService(
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
      await selectFirstSuggestion(wrapper)
      await wrapper
        .find('[data-test-id="remove-refinement-suggestions"]')
        .trigger("click")
      usePopups().popups.done(true)
      await flushPromises()

      expect(removeSuggestionsSpy).toHaveBeenCalledWith(
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

    it("does not call API when removal is cancelled", async () => {
      const removeSuggestionsSpy = mockSdkService(
        AiController,
        "removeRefinementSuggestion",
        {
          content: "Updated content",
        }
      )
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstSuggestion(wrapper)
      await wrapper
        .find('[data-test-id="remove-refinement-suggestions"]')
        .trigger("click")
      usePopups().popups.done(false)

      expect(removeSuggestionsSpy).not.toHaveBeenCalled()
      expect(wrapper.emitted()).not.toHaveProperty("contentUpdated")
    })
  })

  describe("loading modal", () => {
    function deferApiCompletion() {
      let resolveApi!: () => void
      const apiGate = new Promise<void>((resolve) => {
        resolveApi = resolve
      })
      return { apiGate, finishApi: () => resolveApi() }
    }

    async function startRemovingWithPendingApi(apiResult: { content: string }) {
      const { apiGate, finishApi } = deferApiCompletion()
      mockSdkServiceWithImplementation(
        AiController,
        "removeRefinementSuggestion",
        async () => {
          await apiGate
          return apiResult
        }
      )
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstSuggestion(wrapper)
      await wrapper
        .find('[data-test-id="remove-refinement-suggestions"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()
      return { wrapper, finishApi }
    }

    it("shows LoadingModal while removing suggestions", async () => {
      const { finishApi } = await startRemovingWithPendingApi({
        content: "Updated content",
      })

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      expect(document.body.textContent).toContain("AI is removing content...")
      finishApi()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })

    it("hides LoadingModal when remove API fails", async () => {
      const { apiGate, finishApi } = deferApiCompletion()
      mockSdkServiceWithImplementation(
        AiController,
        "removeRefinementSuggestion",
        async () => {
          await apiGate
          return wrapSdkError("API Error")
        }
      )
      const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
      await flushPromises()
      await selectFirstSuggestion(wrapper)
      await wrapper
        .find('[data-test-id="remove-refinement-suggestions"]')
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      finishApi()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })
  })
})
