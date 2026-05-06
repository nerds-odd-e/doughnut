import { describe, it, vi, expect, beforeEach, afterEach } from "vitest"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import NoteExportDialog from "@/components/notes/core/NoteExportDialog.vue"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { saveAs } from "file-saver"
import { page } from "vitest/browser"
import { flushPromises, type VueWrapper } from "@vue/test-utils"

vi.mock("file-saver", () => ({ saveAs: vi.fn() }))

const aiMarkdownStub = { markdown: "# AI context\n\nHello **world**." }

describe("NoteExportDialog", () => {
  let wrapper: VueWrapper
  beforeEach(() => {
    vi.clearAllMocks()
    mockSdkService("getAiContextMarkdown", aiMarkdownStub)
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("fetches and displays AI markdown on open", async () => {
    const note = makeMe.aNote.please()
    wrapper = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .mount({ attachTo: document.body })
    await flushPromises()
    expect(NoteController.getAiContextMarkdown).toHaveBeenCalledWith({
      path: { note: note.id },
    })
    const ta = page.getByTestId("ai-context-markdown-textarea")
    await expect.element(ta).toBeVisible()
    await expect.element(ta).toHaveValue(expect.stringContaining("AI context"))
  })

  it("fetches and displays graph JSON when expanded", async () => {
    const note = makeMe.aNote.please()
    const graphData = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    const getGraphSpy = mockSdkService("getGraph", graphData)
    wrapper = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .mount({ attachTo: document.body })

    // Initially, textarea is not visible
    await expect
      .element(page.getByTestId("graph-json-textarea"))
      .not.toBeInTheDocument()

    // Expand the details
    await page.getByText("Export Note Graph (JSON)").click()
    await flushPromises()

    const textarea = page.getByTestId("graph-json-textarea")
    await expect.element(textarea).toBeInTheDocument()
    await expect
      .element(textarea)
      .toHaveValue(expect.stringContaining('"focusNote"'))

    // Should call API once
    expect(getGraphSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      query: { tokenLimit: 2000 },
    })
  })

  it("downloads AI markdown when primary download is clicked", async () => {
    const note = makeMe.aNote.please()
    wrapper = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .mount({ attachTo: document.body })
    await flushPromises()
    await page.getByTestId("download-ai-context-md-btn").click()
    expect(saveAs).toHaveBeenCalled()
    const blobArg = vi.mocked(saveAs).mock.calls[0][0] as Blob
    expect(blobArg.type).toContain("markdown")
  })

  it("downloads graph JSON when download button is clicked", async () => {
    const note = makeMe.aNote.please()
    const graphData = {
      focusNote: { id: note.id },
      relatedNotes: [],
    } as never
    mockSdkService("getGraph", graphData)
    wrapper = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .mount({ attachTo: document.body })

    await page.getByText("Export Note Graph (JSON)").click()
    await flushPromises()
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
    wrapper = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .mount({ attachTo: document.body })

    const toggleBtn = page.getByText("Export Note Graph (JSON)")
    await toggleBtn.click()
    await flushPromises()
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

    wrapper = helper
      .component(NoteExportDialog)
      .withProps({ note })
      .mount({ attachTo: document.body })

    await page.getByText("Export Note Graph (JSON)").click()
    await flushPromises()
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
