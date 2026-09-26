import { AiController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import { describe, expect, it } from "vitest"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { openExtractionPreview } from "./noteRefinementExtractionTestSupport"
import {
  mountNoteRefinement,
  mountNoteRefinementReady,
  mountNoteRefinementWithLayout,
  mountNoteRefinementWithLayoutReady,
  note,
  refinementActionButton,
  refinementLayoutItems,
  refinementLayoutSelectionApiCall,
  selectRefinementLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

const exportData = (instructions: string) => ({
  model: "gpt-4",
  instructions,
  input: "Note content",
  text: {
    format: {
      type: "json_schema",
      schema: { type: "object" },
    },
  },
})

const exportLoadingEl = () =>
  document.body.querySelector('[data-testid="export-loading"]')

const exportTextarea = () =>
  document.body.querySelector(
    '[data-testid="export-textarea"]'
  ) as HTMLTextAreaElement | null

function expectExportTextareaContaining(instructions: string) {
  const textarea = exportTextarea()
  expect(textarea).toBeTruthy()
  expect(textarea!.value).toContain('"model"')
  expect(textarea!.value).toContain('"instructions"')
  expect(textarea!.value).toContain(instructions)
}

describe("NoteRefinement export requests", () => {
  it("enables export breakdown button when layout is shown without selection", async () => {
    const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
    await flushPromises()

    expect(
      refinementActionButton(wrapper, "export-breakdown-request").disabled
    ).toBe(false)
  })

  it("opens export dialog with breakdown request JSON", async () => {
    const instructions =
      "Return one current-content refinement layout for the note content"
    const exportBreakdownRequestSpy = mockSdkService(
      AiController,
      "exportRefinementLayoutRequest",
      exportData(instructions)
    )
    const wrapper = mountNoteRefinementWithLayout(
      refinementLayoutItems(["Point 1"])
    )
    await flushPromises()

    await wrapper
      .find('[data-test-id="export-breakdown-request"]')
      .trigger("click")
    await flushPromises()

    expect(exportBreakdownRequestSpy).toHaveBeenCalledWith({
      path: { note: note.id },
    })
    expectExportTextareaContaining(instructions)
  })

  it("toggles export extract button with selection and opens dialog with extract request JSON", async () => {
    const layout = refinementLayoutItems(["Point 1", "Point 2"])
    const instructions = "Extract selected refinement layout items"
    let resolveExport!: (value: ReturnType<typeof exportData>) => void
    const exportExtractRequestSpy = mockSdkServiceWithImplementation(
      AiController,
      "exportExtractRequest",
      () =>
        new Promise<ReturnType<typeof exportData>>((resolve) => {
          resolveExport = resolve
        })
    )
    const wrapper = await mountNoteRefinementWithLayoutReady(layout)

    expect(
      refinementActionButton(wrapper, "export-extract-request").disabled
    ).toBe(true)

    await selectRefinementLayoutItem(wrapper, "p2")
    expect(
      refinementActionButton(wrapper, "export-extract-request").disabled
    ).toBe(false)

    await wrapper
      .find('[data-test-id="export-extract-request"]')
      .trigger("click")
    await nextTick()

    expect(exportLoadingEl()).toBeTruthy()
    expect(exportTextarea()).toBeNull()

    resolveExport(exportData(instructions))
    await flushPromises()

    expect(exportLoadingEl()).toBeNull()
    expect(exportExtractRequestSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, layout, ["p2"])
    )
    expectExportTextareaContaining(instructions)
  })

  it("does not show export buttons on the extraction preview screen", async () => {
    const wrapper = await mountNoteRefinementReady(["Point 1"])

    await openExtractionPreview(wrapper, "p1")

    expect(
      wrapper.find('[data-test-id="export-breakdown-request"]').exists()
    ).toBe(false)
    expect(
      wrapper.find('[data-test-id="export-extract-request"]').exists()
    ).toBe(false)
  })
})
