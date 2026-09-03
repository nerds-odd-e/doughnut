import { AiController } from "@generated/donut-backend-api/sdk.gen"
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
  it("cascades parent selection and marks already extracted items without disabling", async () => {
    const wrapper = await mountNoteRefinementWithLayoutReady(
      sampleNestedLayout()
    )

    expect(
      wrapper.find('[data-test-id="refinement-layout-item-p1-2"]').text()
    ).toContain("Already extracted")
    expect(layoutCheckbox(wrapper, "p1-2").disabled).toBe(false)

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

  it("submits only checked descendants when parent is indeterminate for remove", async () => {
    const spy = mockSdkService(AiController, "removeRefinementSuggestion", {
      content: "Updated content",
    })
    const { layout, wrapper } =
      await mountNestedLayoutWithIndeterminateParentSelection()
    await clickRemoveRefinementLayout(wrapper)

    expect(spy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, layout, ["p1-1"])
    )
  })

  it("includes parent id when all descendants are selected", async () => {
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

  it("preselects ledToQuestion items only when question context is provided", async () => {
    const withContext = await mountNoteRefinementWithLayoutReady(
      refinementLayoutItems(["Question-led point", "Other point"], {
        ledToQuestion: [true, false],
      }),
      {
        questionContext: {
          stem: "What is the capital?",
          choices: ["Paris", "London"],
          correctAnswerIndex: 0,
        },
      }
    )
    expect(layoutCheckbox(withContext, "p1").checked).toBe(true)
    expect(layoutCheckbox(withContext, "p2").checked).toBe(false)

    const withoutContext = await mountNoteRefinementWithLayoutReady(
      refinementLayoutItems(["Flagged without context"], {
        ledToQuestion: [true],
      })
    )
    expect(layoutCheckbox(withoutContext, "p1").checked).toBe(false)
  })
})
