import {
  RelationController,
  SearchController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import SearchForm from "@/components/links/SearchForm.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkError } from "@tests/helpers"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import createNoteStorage from "@/store/createNoteStorage"
import { afterEach, describe, expect, it, vi } from "vitest"
import { advanceSearchDebounce } from "@tests/helpers/searchDebounceTestSupport"
import {
  confirmMovePopup,
  deadLinkPayload,
  makeNoteHit,
  makeNotebookHit,
  openAddLinkChoice,
  renderSearchForm,
  searchAndClickMoveUnder,
  setupSearchDialogTests,
  typeInSearch,
} from "./searchDialogTestSupport"

describe("SearchForm actions", () => {
  setupSearchDialogTests()

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  describe("Add link choice step", () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })

    it("shows link choice buttons and relationship form when Add a new relationship note is clicked", async () => {
      const note = MakeMe.aNote.please()
      await openAddLinkChoice(note, { router: true })

      expect(screen.getByText("Insert as a wiki link")).toBeInTheDocument()
      expect(
        screen.getByText("Add a new relationship note")
      ).toBeInTheDocument()

      fireEvent.click(screen.getByText("Add a new relationship note"))
      await flushPromises()

      expect(screen.getByText("Complete relationship")).toBeInTheDocument()
    })
  })

  describe("Move Under folder hit", () => {
    const targetFolderId = 42

    beforeEach(() => {
      vi.useFakeTimers()
    })

    it("calls moveNoteToFolder with folder id after confirm", async () => {
      const note = MakeMe.aNote.please()
      const moveNoteToFolderSpy = mockSdkService(
        RelationController,
        "moveNoteToFolder",
        []
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
    })

    it("shows confirm when move is blocked by soft-deleted title at destination", async () => {
      const note = MakeMe.aNote.please()
      const conflictMessage =
        "A note with this title already exists here but was deleted."
      mockSdkService(
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
      await confirmMovePopup()

      const conflictPopup = usePopups().popups.peek()?.[0]
      expect(conflictPopup?.type).toBe("confirm")
      expect(conflictPopup?.message).toContain(conflictMessage)
      expect(conflictPopup?.message).toContain("rename the note you are moving")
    })
  })

  describe("Move to notebook root on NOTEBOOK hit", () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })

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

  describe("Dead link - link to existing note", () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })

    it("prefills search with dead link display text and searches automatically", async () => {
      const note = MakeMe.aNote.please()
      const searchSpy = mockSdkService(
        SearchController,
        "searchForRelationshipTargetWithin",
        [makeNoteHit("Selected Note", note.noteTopology.id + 100)]
      )

      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note, deadLinkPayload })
        .render()
      await flushPromises()

      const searchInput = screen.getByPlaceholderText("Search")
      expect(searchInput).toHaveValue("original text")
      await advanceSearchDebounce()

      expect(searchSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: { note: note.id },
          body: expect.objectContaining({ searchKey: "original text" }),
        })
      )
    })

    it("rewrites note content when linking dead link to existing note", async () => {
      const noteRealm = MakeMe.aNoteRealm
        .content("See [[original text]] for details.")
        .please()
      const note = noteRealm.note
      const targetNotebookId = note.noteTopology.id + 100
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNoteHit("Selected Note", targetNotebookId),
      ])
      const updateSpy = mockSdkService(
        TextContentController,
        "updateNoteContent",
        MakeMe.aNoteRealm.please()
      )

      const storageAccessor = useStorageAccessor()
      storageAccessor.value = createNoteStorage()
      storageAccessor.value.refreshNoteRealm(noteRealm)

      const searchInput = await renderSearchForm(
        { note, deadLinkPayload },
        { cleanStorage: false }
      )
      await typeInSearch(searchInput, "Selected")

      fireEvent.click(screen.getByText("Add link"))
      await flushPromises()

      const linkButton = screen.getByText('Link "original text" to this note')
      expect(linkButton).toBeInTheDocument()

      fireEvent.click(linkButton)
      await flushPromises()

      expect(updateSpy).toHaveBeenCalledTimes(1)
      expect(updateSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          body: expect.objectContaining({
            content: "See [[Selected Note|original text]] for details.",
          }),
        })
      )
    })
  })
})
