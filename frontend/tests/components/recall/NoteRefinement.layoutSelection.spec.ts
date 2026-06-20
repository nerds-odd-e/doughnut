import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import { mockSdkService } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  layoutCheckbox,
  mountNoteRefinementWithLayout,
  note,
  refinementSuggestionsApiCall,
  sampleNestedLayout,
  selectRefinementLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement layout selection", () => {
  it("checks children when a parent is checked", async () => {
    const wrapper = mountNoteRefinementWithLayout(sampleNestedLayout())
    await flushPromises()

    await selectRefinementLayoutItem(wrapper, "p1")

    expect(layoutCheckbox(wrapper, "p1").checked).toBe(true)
    expect(layoutCheckbox(wrapper, "p1-1").checked).toBe(true)
    expect(layoutCheckbox(wrapper, "p1-2").checked).toBe(true)
  })

  it("makes parent indeterminate when a selected child is unchecked", async () => {
    const wrapper = mountNoteRefinementWithLayout(sampleNestedLayout())
    await flushPromises()

    await selectRefinementLayoutItem(wrapper, "p1")
    await selectRefinementLayoutItem(wrapper, "p1-2", false)

    expect(layoutCheckbox(wrapper, "p1").checked).toBe(false)
    expect(layoutCheckbox(wrapper, "p1").indeterminate).toBe(true)
    expect(layoutCheckbox(wrapper, "p1-1").checked).toBe(true)
    expect(layoutCheckbox(wrapper, "p1-2").checked).toBe(false)
  })

  it("marks already extracted layout points clearly without disabling selection", async () => {
    const wrapper = mountNoteRefinementWithLayout(sampleNestedLayout())
    await flushPromises()

    const alreadyExtractedItem = wrapper.find(
      '[data-test-id="refinement-layout-item-p1-2"]'
    )

    expect(alreadyExtractedItem.text()).toContain("Already extracted")
    expect(layoutCheckbox(wrapper, "p1-2").disabled).toBe(false)
  })

  it("removes non-contiguous selected layout points", async () => {
    const removeSuggestionsSpy = mockSdkService(
      AiController,
      "removeRefinementSuggestion",
      {
        content: "Updated content",
      }
    )
    const wrapper = mountNoteRefinementWithLayout(sampleNestedLayout())
    await flushPromises()
    await selectRefinementLayoutItem(wrapper, "p1-1")
    await selectRefinementLayoutItem(wrapper, "p2")
    await wrapper
      .find('[data-test-id="remove-refinement-suggestions"]')
      .trigger("click")
    usePopups().popups.done(true)
    await flushPromises()

    expect(removeSuggestionsSpy).toHaveBeenCalledWith(
      refinementSuggestionsApiCall(note.id, ["Child point A", "Separate point"])
    )
  })
})
