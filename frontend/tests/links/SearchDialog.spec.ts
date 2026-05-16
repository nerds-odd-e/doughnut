import {
  NoteController,
  RelationController,
  SearchController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import SearchForm from "@/components/links/SearchForm.vue"
import Modal from "@/components/commons/Modal.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import createNoteStorage from "@/store/createNoteStorage"
import {
  appendSearchKeyToHistory,
  clearSearchKeyHistoryCookie,
  readSearchKeyHistory,
} from "@/utils/searchKeyHistoryCookie"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { defineComponent } from "vue"

function makeNoteHit(title: string, notebookId: number) {
  return {
    hitKind: "NOTE" as const,
    noteSearchResult: MakeMe.aNoteSearchResult
      .title(title)
      .notebookId(notebookId)
      .please(),
  }
}

describe("SearchForm", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearSearchKeyHistoryCookie()
    // Mock services used by SearchResults component
    mockSdkService(NoteController, "getRecentNotes", [])
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(SearchController, "semanticSearchWithin", [])
  })
  it("Search at the top level with no note", async () => {
    helper
      .component(SearchForm)
      .withCleanStorage()
      .withProps({ note: null })
      .render()
    await screen.findByPlaceholderText("Search")
    expect(
      await screen.findByRole("button", {
        name: "Semantic search",
      })
    ).toBeInTheDocument()
    expect(
      await screen.findByRole("button", {
        name: "All My Notebooks And Subscriptions",
      })
    ).toBeDisabled()
  })

  it("toggle search settings", async () => {
    const note = MakeMe.aNote.please()
    helper.component(SearchForm).withCleanStorage().withProps({ note }).render()
    ;(await screen.findByRole("button", { name: "All My Circles" })).click()
    expect(
      await screen.findByRole("button", {
        name: "All My Notebooks And Subscriptions",
      })
    ).toHaveClass("text-primary")
    flushPromises()
    ;(
      await screen.findByRole("button", {
        name: "All My Notebooks And Subscriptions",
      })
    ).click()
    expect(
      await screen.findByRole("button", { name: "All My Circles" })
    ).not.toHaveClass("text-primary")
  })

  describe("Add link choice step", () => {
    it("shows choice buttons when Add link is clicked on a note hit", async () => {
      const note = MakeMe.aNote.please()
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNoteHit("Target Note", note.noteTopology.id + 100),
      ])
      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note })
        .render()

      const searchInput = await screen.findByPlaceholderText("Search")
      fireEvent.update(searchInput, "Target")
      await new Promise((resolve) => setTimeout(resolve, 1100))
      await flushPromises()

      const addLinkBtn = await screen.findByRole("button", { name: "Add link" })
      fireEvent.click(addLinkBtn)
      await flushPromises()

      expect(
        await screen.findByRole("button", { name: "Insert as a wiki link" })
      ).toBeInTheDocument()
      expect(
        await screen.findByRole("button", {
          name: "Add a new relationship note",
        })
      ).toBeInTheDocument()
    })

    it("shows relationship form when Add a new relationship note is clicked", async () => {
      const note = MakeMe.aNote.please()
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNoteHit("Target Note", note.noteTopology.id + 100),
      ])
      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note })
        .render()

      const searchInput = await screen.findByPlaceholderText("Search")
      fireEvent.update(searchInput, "Target")
      await new Promise((resolve) => setTimeout(resolve, 1100))
      await flushPromises()

      fireEvent.click(await screen.findByRole("button", { name: "Add link" }))
      await flushPromises()
      fireEvent.click(
        await screen.findByRole("button", {
          name: "Add a new relationship note",
        })
      )
      await flushPromises()

      expect(
        await screen.findByText("Complete relationship")
      ).toBeInTheDocument()
    })
  })

  describe("Move Under folder hit", () => {
    it("calls moveNoteToFolder with folder id after confirm", async () => {
      mockSdkService(NoteController, "getRecentNotes", [])
      mockSdkService(SearchController, "searchForRelationshipTarget", [])
      mockSdkService(SearchController, "semanticSearch", [])
      mockSdkService(SearchController, "semanticSearchWithin", [])
      const note = MakeMe.aNote.please()
      const targetFolderId = 42
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        {
          hitKind: "FOLDER",
          folderId: targetFolderId,
          folderName: "Archive",
          notebookId: 1,
          notebookName: "Nb",
          distance: 0.9,
        },
      ])
      const moveNoteToFolderSpy = mockSdkService(
        RelationController,
        "moveNoteToFolder",
        []
      )

      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note })
        .render()

      await flushPromises()

      const searchInput = await screen.findByPlaceholderText("Search")
      fireEvent.update(searchInput, "Arc")

      await new Promise((resolve) => setTimeout(resolve, 1100))
      await flushPromises()

      expect(moveNoteToFolderSpy).not.toHaveBeenCalled()

      await screen.findByRole("button", { name: "Move Under" })
      fireEvent.click(screen.getByRole("button", { name: "Move Under" }))
      await flushPromises()

      usePopups().popups.done(true)
      await flushPromises()

      expect(moveNoteToFolderSpy).toHaveBeenCalledTimes(1)
      expect(moveNoteToFolderSpy).toHaveBeenCalledWith({
        path: {
          sourceNote: note.id,
          targetFolder: targetFolderId,
        },
      })
    })
  })

  describe("Move to notebook root on NOTEBOOK hit", () => {
    it("calls moveNoteToNotebookRootInNotebook with notebook id after confirm", async () => {
      mockSdkService(NoteController, "getRecentNotes", [])
      mockSdkService(SearchController, "searchForRelationshipTarget", [])
      mockSdkService(SearchController, "semanticSearch", [])
      mockSdkService(SearchController, "semanticSearchWithin", [])
      const note = MakeMe.aNote.please()
      const targetNotebookId = 99
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        {
          hitKind: "NOTEBOOK",
          notebookId: targetNotebookId,
          notebookName: "Other NB",
          distance: 0,
        },
      ])
      const spy = mockSdkService(
        RelationController,
        "moveNoteToNotebookRootInNotebook",
        []
      )

      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note })
        .render()

      await flushPromises()

      const searchInput = await screen.findByPlaceholderText("Search")
      fireEvent.update(searchInput, "Other")

      await new Promise((resolve) => setTimeout(resolve, 1100))
      await flushPromises()

      expect(spy).not.toHaveBeenCalled()

      await screen.findByRole("button", { name: "Move to notebook root" })
      fireEvent.click(
        screen.getByRole("button", { name: "Move to notebook root" })
      )
      await flushPromises()

      usePopups().popups.done(true)
      await flushPromises()

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
    it("shows 'Link ... to this note' button when dead link payload is provided", async () => {
      const note = MakeMe.aNote.please()
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNoteHit("Selected Note", note.noteTopology.id + 100),
      ])

      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({
          note,
          deadLinkPayload: {
            targetToken: "original text",
            displayText: "original text",
          },
        })
        .render()

      const searchInput = await screen.findByPlaceholderText("Search")
      fireEvent.update(searchInput, "Selected")
      await new Promise((resolve) => setTimeout(resolve, 1100))
      await flushPromises()

      fireEvent.click(await screen.findByText("Add link"))
      await flushPromises()

      expect(
        await screen.findByText('Link "original text" to this note')
      ).toBeInTheDocument()
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

      // Reset storage and pre-populate with the note realm so content is accessible
      const storageAccessor = useStorageAccessor()
      storageAccessor.value = createNoteStorage()
      storageAccessor.value.refreshNoteRealm(noteRealm)

      helper
        .component(SearchForm)
        .withProps({
          note,
          deadLinkPayload: {
            targetToken: "original text",
            displayText: "original text",
          },
        })
        .render()

      const searchInput = await screen.findByPlaceholderText("Search")
      fireEvent.update(searchInput, "Selected")
      await new Promise((resolve) => setTimeout(resolve, 1100))
      await flushPromises()

      fireEvent.click(await screen.findByText("Add link"))
      await flushPromises()

      fireEvent.click(
        await screen.findByText('Link "original text" to this note')
      )
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

  describe("search key history", () => {
    it("shows empty message when cookie has no entries", async () => {
      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note: null })
        .render()
      await screen.findByPlaceholderText("Search")
      fireEvent.click(screen.getByTestId("search-key-history-trigger"))
      await flushPromises()
      expect(screen.getByText("No search history yet")).toBeInTheDocument()
    })

    it("lists cookie keys and fills the input when one is chosen", async () => {
      appendSearchKeyToHistory("older")
      appendSearchKeyToHistory("newer")
      const note = MakeMe.aNote.please()
      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note })
        .render()
      await screen.findByPlaceholderText("Search")
      fireEvent.click(screen.getByTestId("search-key-history-trigger"))
      await flushPromises()
      fireEvent.click(screen.getByTestId("search-key-history-item-0"))
      await flushPromises()
      const input = screen.getByPlaceholderText("Search") as HTMLInputElement
      expect(input.value).toBe("newer")
    })

    it("collapses search key history when clicking outside", async () => {
      appendSearchKeyToHistory("older")
      const note = MakeMe.aNote.please()
      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note })
        .render()
      const input = await screen.findByPlaceholderText("Search")
      fireEvent.click(screen.getByTestId("search-key-history-trigger"))
      await flushPromises()

      const dropdown = screen.getByTestId(
        "search-key-history-dropdown"
      ) as HTMLDetailsElement
      expect(dropdown.open).toBe(true)

      fireEvent.click(input)
      await flushPromises()

      expect(dropdown.open).toBe(false)
    })

    it("collapses search key history when clicking a search scope toggle", async () => {
      appendSearchKeyToHistory("older")
      const note = MakeMe.aNote.please()
      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note })
        .render()
      await screen.findByPlaceholderText("Search")
      fireEvent.click(screen.getByTestId("search-key-history-trigger"))
      await flushPromises()

      const dropdown = screen.getByTestId(
        "search-key-history-dropdown"
      ) as HTMLDetailsElement
      expect(dropdown.open).toBe(true)

      fireEvent.click(screen.getByTitle("All My Circles"))
      await flushPromises()

      expect(dropdown.open).toBe(false)
    })

    it("collapses search key history inside a modal when clicking elsewhere in that modal", async () => {
      appendSearchKeyToHistory("older")
      const note = MakeMe.aNote.please()
      const SearchFormInModal = defineComponent({
        components: { Modal, SearchForm },
        props: ["note"],
        template: `
          <Modal :show-close-button="false">
            <template #body>
              <SearchForm :note="note" />
            </template>
          </Modal>
        `,
      })

      helper
        .component(SearchFormInModal)
        .withCleanStorage()
        .withRouter()
        .withProps({ note })
        .render()
      await screen.findByPlaceholderText("Search")
      fireEvent.click(screen.getByTestId("search-key-history-trigger"))
      await flushPromises()

      const dropdown = screen.getByTestId(
        "search-key-history-dropdown"
      ) as HTMLDetailsElement
      expect(dropdown.open).toBe(true)

      fireEvent.click(screen.getByTitle("All My Circles"))
      await flushPromises()

      expect(dropdown.open).toBe(false)
    })

    it("records trimmed search key after debounced search completes", async () => {
      clearSearchKeyHistoryCookie()
      const note = MakeMe.aNote.please()
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNoteHit("Hit", note.noteTopology.id + 1),
      ])
      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note })
        .render()
      const searchInput = await screen.findByPlaceholderText("Search")
      fireEvent.update(searchInput, "  debounced-term  ")
      await new Promise((resolve) => setTimeout(resolve, 1100))
      await flushPromises()
      expect(readSearchKeyHistory()).toEqual(["debounced-term"])
    })
  })
})
