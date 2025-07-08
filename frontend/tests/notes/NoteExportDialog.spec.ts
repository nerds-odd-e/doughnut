import { describe, it, vi, expect } from "vitest"
import helper from "../helpers"
import makeMe from "../fixtures/makeMe"
import NoteExportDialog from "@/components/notes/core/NoteExportDialog.vue"
import { fireEvent, waitFor } from "@testing-library/vue"
import { saveAs } from "file-saver"

vi.mock("file-saver", () => ({ saveAs: vi.fn() }))

describe("NoteExportDialog", () => {
  it("downloads all descendants as JSON when button is clicked", async () => {
    const note = makeMe.aNote.please()
    const descendantsData = { focusNote: { id: note.id }, relatedNotes: [] }
    helper.managedApi.restNoteController.getDescendants = vi
      .fn()
      .mockResolvedValue(descendantsData)
    const { getByText } = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .render()
    const button = getByText("Download All Descendants (JSON)")
    await fireEvent.click(button)
    await waitFor(() => {
      expect(
        helper.managedApi.restNoteController.getDescendants
      ).toHaveBeenCalledWith(note.id)
      expect(saveAs).toHaveBeenCalled()
    })
  })
})
