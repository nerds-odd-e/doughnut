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
    await waitFor(() => getByTestId("download-json-btn-descendants"))
    await fireEvent.click(getByTestId("download-json-btn-descendants"))
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

  it("fetches and displays graph JSON when expanded", async () => {
    const note = makeMe.aNote.please()
    const graphData = { focusNote: { id: note.id }, relatedNotes: [] }
    helper.managedApi.restNoteController.getGraph = vi
      .fn()
      .mockResolvedValue(graphData)
    const { getByText, getByTestId, queryByTestId } = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .render()
    // Initially, textarea is not visible
    expect(queryByTestId("graph-json-textarea")).toBeNull()
    // Expand the details
    await fireEvent.click(getByText("Export Note Graph (JSON)"))
    await waitFor(() => {
      const textarea = getByTestId("graph-json-textarea") as HTMLTextAreaElement
      expect(textarea).toBeTruthy()
      expect(textarea.value).toContain('"focusNote"')
    })
    // Should call API once
    expect(helper.managedApi.restNoteController.getGraph).toHaveBeenCalledWith(
      note.id,
      5000
    )
  })

  it("downloads graph JSON when download button is clicked", async () => {
    const note = makeMe.aNote.please()
    const graphData = { focusNote: { id: note.id }, relatedNotes: [] }
    helper.managedApi.restNoteController.getGraph = vi
      .fn()
      .mockResolvedValue(graphData)
    const { getByText, getByTestId } = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .render()
    await fireEvent.click(getByText("Export Note Graph (JSON)"))
    await waitFor(() => getByTestId("download-json-btn-graph"))
    await fireEvent.click(getByTestId("download-json-btn-graph"))
    expect(saveAs).toHaveBeenCalled()
  })

  it("does not refetch graph JSON if already loaded when toggling open/close", async () => {
    const note = makeMe.aNote.please()
    const graphData = { focusNote: { id: note.id }, relatedNotes: [] }
    const getGraphMock = vi.fn().mockResolvedValue(graphData)
    helper.managedApi.restNoteController.getGraph = getGraphMock
    const { getByText, getByTestId } = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .render()
    await fireEvent.click(getByText("Export Note Graph (JSON)"))
    await waitFor(() => getByTestId("graph-json-textarea"))
    expect(getGraphMock).toHaveBeenCalledTimes(1)
    // Close and reopen
    await fireEvent.click(getByText("Export Note Graph (JSON)"))
    await fireEvent.click(getByText("Export Note Graph (JSON)"))
    await waitFor(() => getByTestId("graph-json-textarea"))
    expect(getGraphMock).toHaveBeenCalledTimes(1)
  })

  it("allows customizing token limit and refreshes graph", async () => {
    const note = makeMe.aNote.please()
    const graphData1 = { focusNote: { id: note.id }, relatedNotes: [] }
    const graphData2 = {
      focusNote: { id: note.id, token: 1234 },
      relatedNotes: [],
    }
    const getGraphMock = vi
      .fn()
      .mockResolvedValueOnce(graphData1)
      .mockResolvedValueOnce(graphData2)
    helper.managedApi.restNoteController.getGraph = getGraphMock
    const { getByText, getByTestId } = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .render()
    await fireEvent.click(getByText("Export Note Graph (JSON)"))
    await waitFor(() => getByTestId("graph-json-textarea"))
    // Change token limit
    const input = getByTestId("token-limit-input") as HTMLInputElement
    input.value = "1234"
    await fireEvent.input(input)
    await fireEvent.click(getByTestId("refresh-graph-btn"))
    await waitFor(() => {
      expect(getGraphMock).toHaveBeenLastCalledWith(note.id, 1234)
      const textarea = getByTestId("graph-json-textarea") as HTMLTextAreaElement
      expect(textarea.value).toContain('"token": 1234')
    })
  })
})
