import { describe, it, vi, expect } from "vitest"
import helper, { mockSdkService, wrapSdkResponse } from "../helpers"
import makeMe from "../fixtures/makeMe"
import NoteExportDialog from "@/components/notes/core/NoteExportDialog.vue"
import { fireEvent, waitFor } from "@testing-library/vue"
import { saveAs } from "file-saver"
import * as sdk from "@generated/backend/sdk.gen"

vi.mock("file-saver", () => ({ saveAs: vi.fn() }))

describe("NoteExportDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("fetches and displays descendants JSON when expanded", async () => {
    const note = makeMe.aNote.please()
    const descendantsData = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    mockSdkService("getDescendants", descendantsData)
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
    expect(sdk.getDescendants).toHaveBeenCalledWith({
      path: { note: note.id },
    })
  })

  it("downloads JSON when download button is clicked", async () => {
    const note = makeMe.aNote.please()
    const descendantsData = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    mockSdkService("getDescendants", descendantsData)
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
    const descendantsData = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    const getDescendantsMock = mockSdkService("getDescendants", descendantsData)
    const { getByText, getByTestId } = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .render()
    // Clear any calls from initial render
    getDescendantsMock.mockClear()
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
    const graphData = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    mockSdkService("getGraph", graphData)
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
    expect(sdk.getGraph).toHaveBeenCalledWith({
      path: { note: note.id },
      query: { tokenLimit: 5000 },
    })
  })

  it("downloads graph JSON when download button is clicked", async () => {
    const note = makeMe.aNote.please()
    const graphData = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    mockSdkService("getGraph", graphData)
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
    const graphData = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    const getGraphMock = mockSdkService("getGraph", graphData)
    const { getByText, getByTestId } = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .render()
    await fireEvent.click(getByText("Export Note Graph (JSON)"))
    await waitFor(() => getByTestId("graph-json-textarea"))
    // Clear calls from initial render
    getGraphMock.mockClear()
    // Close and reopen
    await fireEvent.click(getByText("Export Note Graph (JSON)"))
    await fireEvent.click(getByText("Export Note Graph (JSON)"))
    await waitFor(() => getByTestId("graph-json-textarea"))
    // Should not call again since data is already loaded
    expect(getGraphMock).toHaveBeenCalledTimes(0)
  })

  it("allows customizing token limit and refreshes graph", async () => {
    const note = makeMe.aNote.please()
    const graphData1 = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    const graphData2 = {
      focusNote: { id: note.id, token: 1234 },
      relatedNotes: [],
    } as never
    const getGraphMock = mockSdkService("getGraph", graphData1)
    getGraphMock
      .mockResolvedValueOnce(wrapSdkResponse(graphData1))
      .mockResolvedValueOnce(wrapSdkResponse(graphData2))
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
      expect(getGraphMock).toHaveBeenLastCalledWith({
        path: { note: note.id },
        query: { tokenLimit: 1234 },
      })
      const textarea = getByTestId("graph-json-textarea") as HTMLTextAreaElement
      expect(textarea.value).toContain('"token": 1234')
    })
  })
})
