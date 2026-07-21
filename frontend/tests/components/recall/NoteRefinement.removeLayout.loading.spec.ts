import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import { mockSdkServiceWithImplementation, wrapSdkError } from "@tests/helpers"
import {
  createDeferredGate,
  loadingModalMask,
} from "./noteRefinementLayoutLoadingTestSupport"
import { clickRemoveRefinementLayout } from "./noteRefinementRemoveTestSupport"
import {
  mountNoteRefinementReady,
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
})
