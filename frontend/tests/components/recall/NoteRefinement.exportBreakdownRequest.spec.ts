import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import { mockSdkService } from "@tests/helpers"
import {
  exportBreakdownRequestButtonTitle,
  mountNoteRefinement,
  mountNoteRefinementWithLayout,
  note,
  openExtractionPreview,
  refinementActionButton,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

const sampleExportData = {
  model: "gpt-4",
  instructions: "Return one current-content layout for the note content",
  input: "Note content",
  text: {
    format: {
      type: "json_schema",
      schema: { type: "object" },
    },
  },
}

describe("NoteRefinement export breakdown request", () => {
  it("enables export button when layout is shown without selection", async () => {
    const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
    await flushPromises()

    expect(
      refinementActionButton(wrapper, "export-breakdown-request").disabled
    ).toBe(false)
  })

  it("opens export dialog with breakdown request JSON", async () => {
    const exportBreakdownRequestSpy = mockSdkService(
      AiController,
      "exportRefinementLayoutRequest",
      sampleExportData
    )
    const wrapper = mountNoteRefinementWithLayout([
      { id: "p1", text: "Point 1", alreadyExtracted: false, children: [] },
    ])
    await flushPromises()

    await wrapper
      .find(`button[title="${exportBreakdownRequestButtonTitle}"]`)
      .trigger("click")
    await flushPromises()

    expect(exportBreakdownRequestSpy).toHaveBeenCalledWith({
      path: { note: note.id },
    })

    const textarea = document.body.querySelector(
      '[data-testid="export-textarea"]'
    ) as HTMLTextAreaElement
    expect(textarea).toBeTruthy()
    expect(textarea.value).toContain('"model"')
    expect(textarea.value).toContain('"instructions"')
    expect(textarea.value).toContain(
      "Return one current-content layout for the note content"
    )
  })

  it("does not show export button on the extraction preview screen", async () => {
    const wrapper = mountNoteRefinement(["Point 1"])
    await flushPromises()

    await openExtractionPreview(wrapper, "p1")

    expect(
      wrapper.find('[data-test-id="export-breakdown-request"]').exists()
    ).toBe(false)
  })
})
