import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import { describe, expect, it } from "vitest"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
import { openExtractionPreview } from "./noteRefinementExtractionTestSupport"
import {
  clickExportExtractRequest,
  exportLoadingEl,
  exportTextarea,
  sampleExtractExportData,
} from "./noteRefinementExportTestSupport"
import {
  mountNoteRefinementReady,
  mountNoteRefinementWithLayoutReady,
  note,
  refinementActionButton,
  refinementLayoutItems,
  refinementLayoutSelectionApiCall,
  selectRefinementLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement export extract request", () => {
  it.each([
    { itemId: null as string | null, expectedDisabled: true },
    { itemId: "p1", expectedDisabled: false },
  ])(
    "export button disabled=$expectedDisabled when selection is $itemId",
    async ({ itemId, expectedDisabled }) => {
      const wrapper = await mountNoteRefinementReady(["Point 1", "Point 2"])
      if (itemId) {
        await selectRefinementLayoutItem(wrapper, itemId)
      }

      expect(
        refinementActionButton(wrapper, "export-extract-request").disabled
      ).toBe(expectedDisabled)
    }
  )

  it("opens export dialog with extract request JSON for the selection", async () => {
    const layout = refinementLayoutItems(["Point 1", "Point 2"])
    const exportData = sampleExtractExportData
    let resolveExport!: (value: typeof exportData) => void
    const exportPromise = new Promise<typeof exportData>((resolve) => {
      resolveExport = resolve
    })
    const exportExtractRequestSpy = mockSdkServiceWithImplementation(
      AiController,
      "exportExtractRequest",
      () => exportPromise
    )
    const wrapper = await mountNoteRefinementWithLayoutReady(layout)

    await selectRefinementLayoutItem(wrapper, "p2")
    await clickExportExtractRequest(wrapper)
    await nextTick()

    expect(exportLoadingEl()).toBeTruthy()
    expect(exportTextarea()).toBeNull()

    resolveExport(exportData)
    await flushPromises()

    expect(exportLoadingEl()).toBeNull()
    expect(exportExtractRequestSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, layout, ["p2"])
    )
    const textarea = exportTextarea()
    expect(textarea).toBeTruthy()
    expect(textarea!.value).toContain('"model"')
    expect(textarea!.value).toContain('"instructions"')
    expect(textarea!.value).toContain("Extract selected layout points")
  })

  it("does not show export button on the extraction preview screen", async () => {
    const wrapper = await mountNoteRefinementReady(["Point 1"])

    await openExtractionPreview(wrapper, "p1")

    expect(
      wrapper.find('[data-test-id="export-extract-request"]').exists()
    ).toBe(false)
  })
})
