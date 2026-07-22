import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
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
  mountNoteRefinementReady,
  refinementLayoutItems,
  selectFirstLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement remove layout loading modal", () => {
  async function mountReadyForRemove() {
    const wrapper = await mountNoteRefinementReady(["Point 1", "Point 2"])
    await selectFirstLayoutItem(wrapper)
    return wrapper
  }

  it("shows LoadingModal while removing layout points and hides on success or failure", async () => {
    const successGate = createDeferredGate()
    mockSdkServiceWithImplementation(
      AiController,
      "removeRefinementSuggestion",
      async () => {
        await successGate.gate
        return { content: "Updated content" }
      }
    )
    const successWrapper = await mountReadyForRemove()
    await clickRemoveRefinementLayout(successWrapper)

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is removing content...")
    successGate.resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()

    const failureGate = createDeferredGate()
    mockSdkServiceWithImplementation(
      AiController,
      "removeRefinementSuggestion",
      async () => {
        await failureGate.gate
        return wrapSdkError("API Error")
      }
    )
    const failureWrapper = await mountReadyForRemove()
    await clickRemoveRefinementLayout(failureWrapper)

    expect(loadingModalMask()).toBeTruthy()
    failureGate.resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
  })

  it("keeps remove continuous blocker noncancelable while nested layout regenerates", async () => {
    const layoutGate = createDeferredGate()
    const wrapper = await mountReadyForRemove()
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
      "AI is generating layout..."
    )

    layoutGate.resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
    expect(wrapper.find('[data-test-id="refinement-layout"]').exists()).toBe(
      true
    )
  })
})
