import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import { mockSdkServiceWithImplementation, wrapSdkError } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  mountNoteRefinement,
  selectFirstLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement remove layout loading modal", () => {
  function deferApiCompletion() {
    let resolveApi!: () => void
    const apiGate = new Promise<void>((resolve) => {
      resolveApi = resolve
    })
    return { apiGate, finishApi: () => resolveApi() }
  }

  async function startRemovingWithPendingApi(apiResult: { content: string }) {
    const { apiGate, finishApi } = deferApiCompletion()
    mockSdkServiceWithImplementation(
      AiController,
      "removeRefinementSuggestion",
      async () => {
        await apiGate
        return apiResult
      }
    )
    const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
    await flushPromises()
    await selectFirstLayoutItem(wrapper)
    await wrapper
      .find('[data-test-id="remove-refinement-layout"]')
      .trigger("click")
    await flushPromises()
    usePopups().popups.done(true)
    await flushPromises()
    return { finishApi }
  }

  it("shows LoadingModal while removing layout points", async () => {
    const { finishApi } = await startRemovingWithPendingApi({
      content: "Updated content",
    })

    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    expect(document.body.textContent).toContain("AI is removing content...")
    finishApi()
    await flushPromises()
    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })

  it("hides LoadingModal when remove API fails", async () => {
    const { apiGate, finishApi } = deferApiCompletion()
    mockSdkServiceWithImplementation(
      AiController,
      "removeRefinementSuggestion",
      async () => {
        await apiGate
        return wrapSdkError("API Error")
      }
    )
    const wrapper = mountNoteRefinement(["Point 1", "Point 2"])
    await flushPromises()
    await selectFirstLayoutItem(wrapper)
    await wrapper
      .find('[data-test-id="remove-refinement-layout"]')
      .trigger("click")
    await flushPromises()
    usePopups().popups.done(true)
    await flushPromises()

    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    finishApi()
    await flushPromises()
    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })
})
