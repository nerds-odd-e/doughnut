import { AiController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { screen } from "@testing-library/vue"
import { describe, expect, it } from "vitest"
import { mockSdkServiceWithImplementation, wrapSdkError } from "@tests/helpers"
import {
  createDeferredGate,
  loadingModalMask,
} from "./noteRefinementLayoutLoadingTestSupport"
import { clickRemoveRefinementLayout } from "./noteRefinementRemoveTestSupport"
import {
  mountNoteRefinementWithFirstItemSelected,
  refinementLayoutItems,
  selectFirstLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement remove layout loading modal", () => {
  it("shows LoadingModal while removing refinement layout items and hides on success or failure", async () => {
    const wrapper = await mountNoteRefinementWithFirstItemSelected(["Point 1"])

    const successGate = createDeferredGate()
    mockSdkServiceWithImplementation(
      AiController,
      "removeRefinementSuggestion",
      async () => {
        await successGate.gate
        return { content: "Updated content" }
      }
    )
    await clickRemoveRefinementLayout(wrapper)

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is removing content...")
    successGate.resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()

    await selectFirstLayoutItem(wrapper)
    const failureGate = createDeferredGate()
    mockSdkServiceWithImplementation(
      AiController,
      "removeRefinementSuggestion",
      async () => {
        await failureGate.gate
        return wrapSdkError("API Error")
      }
    )
    await clickRemoveRefinementLayout(wrapper)

    expect(loadingModalMask()).toBeTruthy()
    failureGate.resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
  })

  it("keeps remove continuous blocker noncancelable while nested layout regenerates", async () => {
    const layoutGate = createDeferredGate()
    const wrapper = await mountNoteRefinementWithFirstItemSelected(["Point 1"])
    mockSdkServiceWithImplementation(
      AiController,
      "generateRefinementSuggestions",
      async () => {
        await layoutGate.gate
        return { items: refinementLayoutItems(["Point 1"]) }
      }
    )
    await clickRemoveRefinementLayout(wrapper)

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is removing content...")
    expect(screen.queryByText("Cancel")).toBeNull()
    expect(document.body.textContent).not.toContain(
      "AI is generating refinement layout..."
    )

    layoutGate.resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
    expect(wrapper.find('[data-test-id="refinement-layout"]').exists()).toBe(
      true
    )
  })
})
