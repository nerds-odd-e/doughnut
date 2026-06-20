import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"
import { mockSdkService } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  extractNoteButtonTitle,
  layoutCheckbox,
  mountNoteRefinementWithLayout,
  note,
  refinementLayoutSelectionApiCall,
  sampleNestedLayout,
  selectRefinementLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

async function mountWithIndeterminateParentSelection() {
  const layout = sampleNestedLayout()
  const wrapper = mountNoteRefinementWithLayout(layout)
  await flushPromises()
  await selectRefinementLayoutItem(wrapper, "p1")
  await selectRefinementLayoutItem(wrapper, "p1-2", false)
  return { layout, wrapper }
}

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

  it("submits only checked descendants when parent is indeterminate (extract)", async () => {
    const extractNoteSpy = mockSdkService(
      AiController,
      "extractNote",
      makeMe.aNoteRealm.please()
    )
    const { layout, wrapper } = await mountWithIndeterminateParentSelection()
    await wrapper
      .find(`button[title="${extractNoteButtonTitle}"]`)
      .trigger("click")
    await flushPromises()

    expect(extractNoteSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, layout, ["p1-1"])
    )
  })

  it("submits only checked descendants when parent is indeterminate (remove)", async () => {
    const removeLayoutSpy = mockSdkService(
      AiController,
      "removeRefinementSuggestion",
      {
        content: "Updated content",
      }
    )
    const { layout, wrapper } = await mountWithIndeterminateParentSelection()
    await wrapper
      .find('[data-test-id="remove-refinement-layout"]')
      .trigger("click")
    usePopups().popups.done(true)
    await flushPromises()

    expect(removeLayoutSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, layout, ["p1-1"])
    )
  })

  it("includes parent id when all descendants are selected again", async () => {
    const extractNoteSpy = mockSdkService(
      AiController,
      "extractNote",
      makeMe.aNoteRealm.please()
    )
    const { layout, wrapper } = await mountWithIndeterminateParentSelection()
    await selectRefinementLayoutItem(wrapper, "p1-2", true)
    await wrapper
      .find(`button[title="${extractNoteButtonTitle}"]`)
      .trigger("click")
    await flushPromises()

    expect(extractNoteSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, layout, ["p1", "p1-1", "p1-2"])
    )
  })

  it("removes non-contiguous selected layout points", async () => {
    const removeLayoutSpy = mockSdkService(
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
      .find('[data-test-id="remove-refinement-layout"]')
      .trigger("click")
    usePopups().popups.done(true)
    await flushPromises()

    expect(removeLayoutSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, sampleNestedLayout(), [
        "p1-1",
        "p2",
      ])
    )
  })
})
