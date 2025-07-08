import { describe, it, vi, expect } from "vitest"
import helper from "../helpers"
import makeMe from "../fixtures/makeMe"
import NoteExportDialog from "@/components/notes/core/NoteExportDialog.vue"
import { fireEvent, waitFor } from "@testing-library/vue"
import { saveAs } from "file-saver"

vi.mock("file-saver", () => ({ saveAs: vi.fn() }))

describe("NoteExportDialog", () => {
  it("fetches and displays descendants JSON when expanded", async () => {
    const note = makeMe.aNote.please()
    const descendantsData = { focusNote: { id: note.id }, relatedNotes: [] }
    helper.managedApi.restNoteController.getDescendants = vi
      .fn()
      .mockResolvedValue(descendantsData)
    const { getByText, getByTestId, queryByTestId } = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .render()
    // Initially, textarea is not visible
    expect(queryByTestId("descendants-json-textarea")).toBeNull()
    // Expand the details
    await fireEvent.click(getByText("Export Descendants (JSON)"))
    await waitFor(() => {
      const textarea = getByTestId(
        "descendants-json-textarea"
      ) as HTMLTextAreaElement
      expect(textarea).toBeTruthy()
      expect(textarea.value).toContain('"focusNote"')
    })
    // Should call API once
    expect(
      helper.managedApi.restNoteController.getDescendants
    ).toHaveBeenCalledWith(note.id)
  })

  it("downloads JSON when download button is clicked", async () => {
    const note = makeMe.aNote.please()
    const descendantsData = { focusNote: { id: note.id }, relatedNotes: [] }
    helper.managedApi.restNoteController.getDescendants = vi
      .fn()
      .mockResolvedValue(descendantsData)
    const { getByText, getByTestId } = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .render()
    await fireEvent.click(getByText("Export Descendants (JSON)"))
    await waitFor(() => getByTestId("download-json-btn"))
    await fireEvent.click(getByTestId("download-json-btn"))
    expect(saveAs).toHaveBeenCalled()
  })

  it("does not refetch JSON if already loaded when toggling open/close", async () => {
    const note = makeMe.aNote.please()
    const descendantsData = { focusNote: { id: note.id }, relatedNotes: [] }
    const getDescendantsMock = vi.fn().mockResolvedValue(descendantsData)
    helper.managedApi.restNoteController.getDescendants = getDescendantsMock
    const { getByText, getByTestId } = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .render()
    await fireEvent.click(getByText("Export Descendants (JSON)"))
    await waitFor(() => getByTestId("descendants-json-textarea"))
    expect(getDescendantsMock).toHaveBeenCalledTimes(1)
    // Close and reopen
    await fireEvent.click(getByText("Export Descendants (JSON)"))
    await fireEvent.click(getByText("Export Descendants (JSON)"))
    await waitFor(() => getByTestId("descendants-json-textarea"))
    expect(getDescendantsMock).toHaveBeenCalledTimes(1)
  })
})
