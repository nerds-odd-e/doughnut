import { describe, it, vi, expect, beforeEach } from "vitest"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import NoteExportDialog from "@/components/notes/core/NoteExportDialog.vue"
import { saveAs } from "file-saver"
import { page } from "vitest/browser"

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
    const getDescendantsSpy = mockSdkService("getDescendants", descendantsData)
    helper.component(NoteExportDialog).withProps({ note }).render()

    // Initially, textarea is not visible
    await expect
      .element(page.getByTestId("descendants-json-textarea"))
      .not.toBeInTheDocument()

    // Expand the details
    await page.getByText("Export Descendants (JSON)").click()

    const textarea = page.getByTestId("descendants-json-textarea")
    await expect.element(textarea).toBeInTheDocument()
    // Value assertion logic: element text content vs value property
    // For textarea, it's usually value.
    // Vitest browser expect.element(locator).toHaveValue(...)
    await expect
      .element(textarea)
      .toHaveValue(expect.stringContaining('"focusNote"'))

    // Should call API once
    expect(getDescendantsSpy).toHaveBeenCalledWith({
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
    helper.component(NoteExportDialog).withProps({ note }).render()

    await page.getByText("Export Descendants (JSON)").click()
    const downloadBtn = page.getByTestId("download-json-btn-descendants")
    await expect.element(downloadBtn).toBeVisible()
    await downloadBtn.click()
    expect(saveAs).toHaveBeenCalled()
  })

  it("does not refetch JSON if already loaded when toggling open/close", async () => {
    const note = makeMe.aNote.please()
    const descendantsData = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    const getDescendantsMock = mockSdkService("getDescendants", descendantsData)
    helper.component(NoteExportDialog).withProps({ note }).render()

    // Clear any calls from initial render
    getDescendantsMock.mockClear()
    const toggleBtn = page.getByText("Export Descendants (JSON)")
    await toggleBtn.click()

    await expect
      .element(page.getByTestId("descendants-json-textarea"))
      .toBeVisible()
    expect(getDescendantsMock).toHaveBeenCalledTimes(1)

    // Close and reopen
    await toggleBtn.click()
    await toggleBtn.click()

    await expect
      .element(page.getByTestId("descendants-json-textarea"))
      .toBeVisible()
    expect(getDescendantsMock).toHaveBeenCalledTimes(1)
  })

  it("fetches and displays graph JSON when expanded", async () => {
    const note = makeMe.aNote.please()
    const graphData = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    const getGraphSpy = mockSdkService("getGraph", graphData)
    helper.component(NoteExportDialog).withProps({ note }).render()

    // Initially, textarea is not visible
    await expect
      .element(page.getByTestId("graph-json-textarea"))
      .not.toBeInTheDocument()

    // Expand the details
    await page.getByText("Export Note Graph (JSON)").click()

    const textarea = page.getByTestId("graph-json-textarea")
    await expect.element(textarea).toBeInTheDocument()
    await expect
      .element(textarea)
      .toHaveValue(expect.stringContaining('"focusNote"'))

    // Should call API once
    expect(getGraphSpy).toHaveBeenCalledWith({
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
    helper.component(NoteExportDialog).withProps({ note }).render()

    await page.getByText("Export Note Graph (JSON)").click()
    const downloadBtn = page.getByTestId("download-json-btn-graph")
    await expect.element(downloadBtn).toBeVisible()
    await downloadBtn.click()
    expect(saveAs).toHaveBeenCalled()
  })

  it("does not refetch graph JSON if already loaded when toggling open/close", async () => {
    const note = makeMe.aNote.please()
    const graphData = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    const getGraphMock = mockSdkService("getGraph", graphData)
    helper.component(NoteExportDialog).withProps({ note }).render()

    const toggleBtn = page.getByText("Export Note Graph (JSON)")
    await toggleBtn.click()
    await expect.element(page.getByTestId("graph-json-textarea")).toBeVisible()

    // Clear calls from initial render
    getGraphMock.mockClear()

    // Close and reopen
    await toggleBtn.click()
    await toggleBtn.click()

    await expect.element(page.getByTestId("graph-json-textarea")).toBeVisible()
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

    helper.component(NoteExportDialog).withProps({ note }).render()

    await page.getByText("Export Note Graph (JSON)").click()
    await expect.element(page.getByTestId("graph-json-textarea")).toBeVisible()

    // Change token limit
    const input = page.getByTestId("token-limit-input")
    await input.fill("1234")

    await page.getByTestId("refresh-graph-btn").click()

    const textarea = page.getByTestId("graph-json-textarea")
    await expect
      .element(textarea)
      .toHaveValue(expect.stringContaining('"token": 1234'))

    expect(getGraphMock).toHaveBeenLastCalledWith({
      path: { note: note.id },
      query: { tokenLimit: 1234 },
    })
  })
})
