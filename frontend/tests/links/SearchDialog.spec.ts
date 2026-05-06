import AddLinkDialog from "@/components/links/AddLinkDialog.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { beforeEach, describe, expect, it, vi } from "vitest"

function makeNoteHit(title: string, notebookId: number) {
  return {
    hitKind: "NOTE" as const,
    noteSearchResult: MakeMe.aNoteSearchResult
      .title(title)
      .notebookId(notebookId)
      .please(),
  }
}

describe("AddLinkDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock services used by SearchResults component
    mockSdkService("getRecentNotes", [])
    mockSdkService("searchForRelationshipTarget", [])
    mockSdkService("searchForRelationshipTargetWithin", [])
    mockSdkService("semanticSearch", [])
    mockSdkService("semanticSearchWithin", [])
  })
  it("Search at the top level with no note", async () => {
    helper
      .component(AddLinkDialog)
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
    helper
      .component(AddLinkDialog)
      .withCleanStorage()
      .withProps({ note })
      .render()
    ;(await screen.findByRole("button", { name: "All My Circles" })).click()
    expect(
      await screen.findByRole("button", {
        name: "All My Notebooks And Subscriptions",
      })
    ).toHaveClass("daisy-text-primary")
    flushPromises()
    ;(
      await screen.findByRole("button", {
        name: "All My Notebooks And Subscriptions",
      })
    ).click()
    expect(
      await screen.findByRole("button", { name: "All My Circles" })
    ).not.toHaveClass("daisy-text-primary")
  })

  describe("Add link choice step", () => {
    it("shows choice buttons when Add link is clicked on a note hit", async () => {
      const note = MakeMe.aNote.please()
      mockSdkService("searchForRelationshipTargetWithin", [
        makeNoteHit("Target Note", note.noteTopology.id + 100),
      ])
      helper
        .component(AddLinkDialog)
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
      mockSdkService("searchForRelationshipTargetWithin", [
        makeNoteHit("Target Note", note.noteTopology.id + 100),
      ])
      helper
        .component(AddLinkDialog)
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
      mockSdkService("getRecentNotes", [])
      mockSdkService("searchForRelationshipTarget", [])
      mockSdkService("semanticSearch", [])
      mockSdkService("semanticSearchWithin", [])
      const note = MakeMe.aNote.please()
      const targetFolderId = 42
      mockSdkService("searchForRelationshipTargetWithin", [
        {
          hitKind: "FOLDER",
          folderId: targetFolderId,
          folderName: "Archive",
          notebookId: 1,
          notebookName: "Nb",
          distance: 0.9,
        },
      ])
      const moveNoteToFolderSpy = mockSdkService("moveNoteToFolder", [])

      helper
        .component(AddLinkDialog)
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
})
