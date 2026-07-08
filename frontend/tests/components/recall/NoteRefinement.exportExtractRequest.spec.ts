import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import { mockSdkService } from "@tests/helpers"
import {
  exportExtractRequestButtonTitle,
  mountNoteRefinement,
  mountNoteRefinementWithLayout,
  note,
  openExtractionPreview,
  refinementActionButton,
  refinementLayoutItems,
  refinementLayoutSelectionApiCall,
  selectRefinementLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

const sampleExportData = {
  model: "gpt-4",
  instructions: "Extract selected layout points",
  input: "Note content",
  text: {
    format: {
      type: "json_schema",
      schema: { type: "object" },
    },
  },
}

describe("NoteRefinement export extract request", () => {
  it("disables export button when no layout points are selected", async () => {
    const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
    await flushPromises()

    expect(
      refinementActionButton(wrapper, "export-extract-request").disabled
    ).toBe(true)
  })

  it("enables export button when layout points are selected", async () => {
    const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
    await flushPromises()

    await selectRefinementLayoutItem(wrapper, "p1")

    expect(
      refinementActionButton(wrapper, "export-extract-request").disabled
    ).toBe(false)
  })

  it("opens export dialog with extract request JSON for the selection", async () => {
    const layout = refinementLayoutItems(["Point 1", "Point 2"])
    const exportExtractRequestSpy = mockSdkService(
      AiController,
      "exportExtractRequest",
      sampleExportData
    )
    const wrapper = mountNoteRefinementWithLayout(layout)
    await flushPromises()

    await selectRefinementLayoutItem(wrapper, "p2")
    await wrapper
      .find(`button[title="${exportExtractRequestButtonTitle}"]`)
      .trigger("click")
    await flushPromises()

    expect(exportExtractRequestSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, layout, ["p2"])
    )

    const textarea = document.body.querySelector(
      '[data-testid="export-textarea"]'
    ) as HTMLTextAreaElement
    expect(textarea).toBeTruthy()
    expect(textarea.value).toContain('"model"')
    expect(textarea.value).toContain('"instructions"')
    expect(textarea.value).toContain("Extract selected layout points")
  })

  it("does not show export button on the extraction preview screen", async () => {
    const wrapper = mountNoteRefinement(["Point 1"])
    await flushPromises()

    await openExtractionPreview(wrapper, "p1")

    expect(
      wrapper.find('[data-test-id="export-extract-request"]').exists()
    ).toBe(false)
  })
})
