import {
  RelationController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import { describe, expect, it } from "vitest"
import {
  confirmMovePopup,
  makeNotebookHit,
  renderSearchForm,
  searchAndClickMoveUnder,
  setupSearchDialogFakeTimers,
  setupSearchDialogTests,
  typeInSearch,
} from "./searchDialogTestSupport"

describe("SearchForm actions", () => {
  setupSearchDialogTests()
  setupSearchDialogFakeTimers()

  describe("Move Under folder hit", () => {
    const targetFolderId = 42

    it("calls moveNoteToFolder with folder id; soft-deleted title conflict shows rename confirm", async () => {
      const note = MakeMe.aNote.please()
      const conflictMessage =
        "A note with this title already exists here but was deleted."
      const moveNoteToFolderSpy = mockSdkService(
        RelationController,
        "moveNoteToFolder",
        []
      ).mockResolvedValue(
        wrapSdkError({
          status: 409,
          errorType: "SOFT_DELETED_TITLE_CONFLICT",
          message: conflictMessage,
        })
      )

      await searchAndClickMoveUnder(note, targetFolderId)
      expect(moveNoteToFolderSpy).not.toHaveBeenCalled()

      await confirmMovePopup()

      expect(moveNoteToFolderSpy).toHaveBeenCalledTimes(1)
      expect(moveNoteToFolderSpy).toHaveBeenCalledWith({
        path: {
          sourceNote: note.id,
          targetFolder: targetFolderId,
        },
      })

      const conflictPopup = usePopups().popups.peek()?.[0]
      expect(conflictPopup?.type).toBe("confirm")
      expect(conflictPopup?.message).toContain(conflictMessage)
      expect(conflictPopup?.message).toContain("rename the note you are moving")
    })
  })

  describe("Move to notebook root on NOTEBOOK hit", () => {
    it("calls moveNoteToNotebookRootInNotebook with notebook id after confirm", async () => {
      const note = MakeMe.aNote.please()
      const targetNotebookId = 99
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNotebookHit(targetNotebookId, "Other NB"),
      ])
      const spy = mockSdkService(
        RelationController,
        "moveNoteToNotebookRootInNotebook",
        []
      )

      const searchInput = await renderSearchForm({ note })
      await typeInSearch(searchInput, "Other")

      expect(spy).not.toHaveBeenCalled()

      fireEvent.click(screen.getByText("Move to notebook root"))
      await flushPromises()
      await confirmMovePopup()

      expect(spy).toHaveBeenCalledTimes(1)
      expect(spy).toHaveBeenCalledWith({
        path: {
          sourceNote: note.id,
          targetNotebook: targetNotebookId,
        },
      })
    })
  })
})
