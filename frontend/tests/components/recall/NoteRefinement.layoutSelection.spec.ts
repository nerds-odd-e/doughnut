import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { describe, expect, it } from "vitest"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import {
  clickRemoveRefinementLayout,
  mountNestedLayoutWithIndeterminateParentSelection,
  sampleNestedLayout,
} from "./noteRefinementRemoveTestSupport"
import {
  clickExtractRefinementLayout,
  layoutCheckbox,
  mountNoteRefinementWithLayoutReady,
  note,
  refinementLayoutItems,
  refinementLayoutSelectionApiCall,
  sampleExtractionPreview,
  selectRefinementLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement layout selection", () => {
  it("cascades parent selection and indeterminate state to children", async () => {
    const wrapper = await mountNoteRefinementWithLayoutReady(
      sampleNestedLayout()
    )

    await selectRefinementLayoutItem(wrapper, "p1")

    expect(layoutCheckbox(wrapper, "p1").checked).toBe(true)
    expect(layoutCheckbox(wrapper, "p1-1").checked).toBe(true)
    expect(layoutCheckbox(wrapper, "p1-2").checked).toBe(true)

    await selectRefinementLayoutItem(wrapper, "p1-2", false)

    expect(layoutCheckbox(wrapper, "p1").checked).toBe(false)
    expect(layoutCheckbox(wrapper, "p1").indeterminate).toBe(true)
    expect(layoutCheckbox(wrapper, "p1-1").checked).toBe(true)
    expect(layoutCheckbox(wrapper, "p1-2").checked).toBe(false)
  })

  it("marks already extracted refinement layout items clearly without disabling selection", async () => {
    const wrapper = await mountNoteRefinementWithLayoutReady(
      sampleNestedLayout()
    )

    const alreadyExtractedItem = wrapper.find(
      '[data-test-id="refinement-layout-item-p1-2"]'
    )

    expect(alreadyExtractedItem.text()).toContain("Already extracted")
    expect(layoutCheckbox(wrapper, "p1-2").disabled).toBe(false)
  })

  it.each([
    {
      action: "extract",
      method: "extractNotePreview" as const,
      response: sampleExtractionPreview(),
      trigger: clickExtractRefinementLayout,
    },
    {
      action: "remove",
      method: "removeRefinementSuggestion" as const,
      response: { content: "Updated content" },
      trigger: clickRemoveRefinementLayout,
    },
  ])(
    "submits only checked descendants when parent is indeterminate ($action)",
    async ({ method, response, trigger }) => {
      const spy = mockSdkService(AiController, method, response)
      const { layout, wrapper } =
        await mountNestedLayoutWithIndeterminateParentSelection()
      await trigger(wrapper)
      if (method === "extractNotePreview") {
        await flushPromises()
      }

      expect(spy).toHaveBeenCalledWith(
        refinementLayoutSelectionApiCall(note.id, layout, ["p1-1"], {
          signal: method === "extractNotePreview",
        })
      )
    }
  )

  it("includes parent id when all descendants are selected again", async () => {
    const extractNotePreviewSpy = mockSdkService(
      AiController,
      "extractNotePreview",
      sampleExtractionPreview()
    )
    const { layout, wrapper } =
      await mountNestedLayoutWithIndeterminateParentSelection()
    await selectRefinementLayoutItem(wrapper, "p1-2", true)
    await clickExtractRefinementLayout(wrapper)
    await flushPromises()

    expect(extractNotePreviewSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(
        note.id,
        layout,
        ["p1", "p1-1", "p1-2"],
        {
          signal: true,
        }
      )
    )
  })

  it("removes non-contiguous selected refinement layout items", async () => {
    const layout = sampleNestedLayout()
    const removeLayoutSpy = mockSdkService(
      AiController,
      "removeRefinementSuggestion",
      {
        content: "Updated content",
      }
    )
    const wrapper = await mountNoteRefinementWithLayoutReady(layout)
    await selectRefinementLayoutItem(wrapper, "p1-1")
    await selectRefinementLayoutItem(wrapper, "p2")
    await clickRemoveRefinementLayout(wrapper)

    expect(removeLayoutSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, layout, ["p1-1", "p2"])
    )
  })

  it("preselects ledToQuestion items when question context is provided", async () => {
    const layout = refinementLayoutItems(
      ["Question-led point", "Other point"],
      { ledToQuestion: [true, false] }
    )
    const wrapper = await mountNoteRefinementWithLayoutReady(layout, {
      questionContext: {
        stem: "What is the capital?",
        choices: ["Paris", "London"],
        correctAnswerIndex: 0,
      },
    })

    expect(layoutCheckbox(wrapper, "p1").checked).toBe(true)
    expect(layoutCheckbox(wrapper, "p2").checked).toBe(false)
  })

  it("starts with empty selection when no question context", async () => {
    const layout = refinementLayoutItems(["Flagged without context"], {
      ledToQuestion: [true],
    })
    const wrapper = await mountNoteRefinementWithLayoutReady(layout)

    expect(layoutCheckbox(wrapper, "p1").checked).toBe(false)
  })
})
