import {
  NoteController,
  RelationController,
  SearchController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import type { Note } from "@generated/doughnut-backend-api"
import SearchForm from "@/components/links/SearchForm.vue"
import Modal from "@/components/commons/Modal.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkError } from "@tests/helpers"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import createNoteStorage from "@/store/createNoteStorage"
import {
  appendSearchKeyToHistory,
  clearSearchKeyHistoryCookie,
  readSearchKeyHistory,
} from "@/utils/searchKeyHistoryCookie"
import { searchResultItemTestId } from "@/utils/searchDialogKeyboard"
import {
  dispatchArrowKey,
  testIdSelector,
} from "@tests/helpers/searchDialogKeyboardTestSupport"
import { advanceSearchDebounce } from "@tests/helpers/searchDebounceTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { defineComponent } from "vue"
const searchResultItemSelector = testIdSelector(searchResultItemTestId)
const searchInputId = "searchTerm-searchKey"

function allSearchResultItems(): Element[] {
  return Array.from(document.querySelectorAll(searchResultItemSelector))
}

function makeNoteHit(title: string, notebookId: number) {
  return {
    hitKind: "NOTE" as const,
    noteSearchResult: MakeMe.aNoteSearchResult
      .title(title)
      .notebookId(notebookId)
      .please(),
  }
}

function makeFolderHit(folderId: number, folderName: string) {
  return {
    hitKind: "FOLDER" as const,
    folderId,
    folderName,
    notebookId: 1,
    notebookName: "Nb",
    distance: 0.9,
  }
}

function makeNotebookHit(notebookId: number, notebookName: string) {
  return {
    hitKind: "NOTEBOOK" as const,
    notebookId,
    notebookName,
    distance: 0,
  }
}

function setupSearchFormSdkMocks() {
  mockSdkService(NoteController, "getRecentNotes", [])
  mockSdkService(SearchController, "searchForRelationshipTarget", [])
  mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
  mockSdkService(SearchController, "semanticSearch", [])
  mockSdkService(SearchController, "semanticSearchWithin", [])
}

async function typeInSearch(input: HTMLElement, value: string) {
  fireEvent.update(input, value)
  await advanceSearchDebounce()
}

async function renderSearchForm(
  props: {
    note?: Note | null
    deadLinkPayload?: {
      targetToken: string
      displayText: string
    }
  },
  options?: { router?: boolean; cleanStorage?: boolean }
) {
  let chain = helper.component(SearchForm)
  if (options?.cleanStorage !== false) {
    chain = chain.withCleanStorage()
  }
  if (options?.router) {
    chain = chain.withRouter()
  }
  chain.withProps(props).render()
  await flushPromises()
  return screen.getByPlaceholderText("Search")
}

async function confirmMovePopup() {
  usePopups().popups.done(true)
  await flushPromises()
}

describe("SearchForm", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearSearchKeyHistoryCookie()
    setupSearchFormSdkMocks()
  })

  it("Search at the top level with no note", async () => {
    helper
      .component(SearchForm)
      .withCleanStorage()
      .withProps({ note: null })
      .render()
    await flushPromises()
    screen.getByPlaceholderText("Search")
    expect(screen.getByTitle("Semantic search")).toBeInTheDocument()
    expect(
      screen.getByTitle("All My Notebooks And Subscriptions")
    ).toBeDisabled()
  })

  describe("keyboard navigation", () => {
    async function renderSearchWithRecentNotes(count: number) {
      const recentNotes = Array.from({ length: count }, (_, i) =>
        MakeMe.aNoteSearchResult.title(`Recent Note ${i + 1}`).please()
      )
      mockSdkService(NoteController, "getRecentNotes", recentNotes)
      helper
        .component(SearchForm)
        .withCleanStorage()
        .withProps({ note: null })
        .render()
      await flushPromises()
      const searchInput = screen.getByPlaceholderText("Search")
      expect(allSearchResultItems().length).toBeGreaterThanOrEqual(count)
      return searchInput
    }

    it("moves focus through results and back to search input with ArrowDown and ArrowUp", async () => {
      const searchInput = await renderSearchWithRecentNotes(2)
      const [firstItem, secondItem] = allSearchResultItems()
      expect(firstItem).toBeTruthy()
      expect(secondItem).toBeTruthy()

      searchInput.focus()
      expect(document.activeElement).toBe(searchInput)

      dispatchArrowKey("ArrowDown", searchInput)
      expect(firstItem!.contains(document.activeElement)).toBe(true)

      dispatchArrowKey("ArrowDown")
      expect(secondItem!.contains(document.activeElement)).toBe(true)

      dispatchArrowKey("ArrowUp")
      expect(firstItem!.contains(document.activeElement)).toBe(true)

      dispatchArrowKey("ArrowUp")
      expect(document.activeElement).toBe(
        document.getElementById(searchInputId)
      )
    })
  })

  it("toggle search settings", async () => {
    const note = MakeMe.aNote.please()
    helper.component(SearchForm).withCleanStorage().withProps({ note }).render()
    await flushPromises()
    screen.getByTitle("All My Circles").click()
    expect(screen.getByTitle("All My Notebooks And Subscriptions")).toHaveClass(
      "text-primary"
    )
    screen.getByTitle("All My Notebooks And Subscriptions").click()
    expect(screen.getByTitle("All My Circles")).not.toHaveClass("text-primary")
  })

  describe("debounced search flows", () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })

    afterEach(() => {
      vi.runOnlyPendingTimers()
      vi.useRealTimers()
    })

    describe("Add link choice step", () => {
      async function openAddLinkChoice(
        note: Note,
        options?: { router?: boolean }
      ) {
        mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
          makeNoteHit("Target Note", note.noteTopology.id + 100),
        ])
        const searchInput = await renderSearchForm({ note }, options)
        await typeInSearch(searchInput, "Target")
        fireEvent.click(screen.getByText("Add link"))
        await flushPromises()
      }

      it("shows choice buttons when Add link is clicked on a note hit", async () => {
        const note = MakeMe.aNote.please()
        await openAddLinkChoice(note)

        expect(screen.getByText("Insert as a wiki link")).toBeInTheDocument()
        expect(
          screen.getByText("Add a new relationship note")
        ).toBeInTheDocument()
      })

      it("shows relationship form when Add a new relationship note is clicked", async () => {
        const note = MakeMe.aNote.please()
        await openAddLinkChoice(note, { router: true })

        fireEvent.click(screen.getByText("Add a new relationship note"))
        await flushPromises()

        expect(screen.getByText("Complete relationship")).toBeInTheDocument()
      })
    })

    describe("Move Under folder hit", () => {
      const targetFolderId = 42

      async function searchAndClickMoveUnder(note: Note) {
        mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
          makeFolderHit(targetFolderId, "Archive"),
        ])
        const searchInput = await renderSearchForm({ note })
        await typeInSearch(searchInput, "Arc")
        fireEvent.click(screen.getByText("Move Under"))
        await flushPromises()
      }

      it("calls moveNoteToFolder with folder id after confirm", async () => {
        const note = MakeMe.aNote.please()
        const moveNoteToFolderSpy = mockSdkService(
          RelationController,
          "moveNoteToFolder",
          []
        )

        await searchAndClickMoveUnder(note)
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

        await searchAndClickMoveUnder(note)
        await confirmMovePopup()

        const conflictPopup = usePopups().popups.peek()?.[0]
        expect(conflictPopup?.type).toBe("confirm")
        expect(conflictPopup?.message).toContain(conflictMessage)
        expect(conflictPopup?.message).toContain(
          "rename the note you are moving"
        )
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

    describe("Dead link - link to existing note", () => {
      const deadLinkPayload = {
        targetToken: "original text",
        displayText: "original text",
      }

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

      it("shows 'Link ... to this note' button when dead link payload is provided", async () => {
        const note = MakeMe.aNote.please()
        mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
          makeNoteHit("Selected Note", note.noteTopology.id + 100),
        ])

        const searchInput = await renderSearchForm({ note, deadLinkPayload })
        await typeInSearch(searchInput, "Selected")

        fireEvent.click(screen.getByText("Add link"))
        await flushPromises()

        expect(
          screen.getByText('Link "original text" to this note')
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
        fireEvent.click(screen.getByText('Link "original text" to this note'))
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

    it("records trimmed search key after debounced search completes", async () => {
      clearSearchKeyHistoryCookie()
      const note = MakeMe.aNote.please()
      mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
        makeNoteHit("Hit", note.noteTopology.id + 1),
      ])
      const searchInput = await renderSearchForm({ note })
      await typeInSearch(searchInput, "  debounced-term  ")
      expect(readSearchKeyHistory()).toEqual(["debounced-term"])
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
  })
})
