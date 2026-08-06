import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import { describe, expect, it } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import {
  clickCreateNoteFromExtractionPreview,
  clickRetryExtractionPreview,
  expectExtractionPreviewError,
  labeledExtractionPreview,
  openExtractionPreview,
} from "./noteRefinementExtractionTestSupport"
import {
  createDeferredGate,
  loadingModalMask,
} from "./noteRefinementLayoutLoadingTestSupport"
import {
  clickExtractRefinementLayout,
  mountNoteRefinementReady,
  sampleExtractionPreview,
  selectRefinementLayoutItem,
  setupNoteRefinementTests,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement extract note loading", () => {
  it("shows LoadingModal during extract preview and hides on success or failure", async () => {
    const successGate = createDeferredGate()
    const successSpy = mockSdkServiceWithImplementation(
      AiController,
      "extractNotePreview",
      async () => {
        await successGate.gate
        return sampleExtractionPreview()
      }
    )
    const successWrapper = await mountNoteRefinementReady(["Test layout point"])

    await selectRefinementLayoutItem(successWrapper, "p1")
    await clickExtractRefinementLayout(successWrapper)
    await nextTick()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is generating preview...")
    successGate.resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
    expect(successSpy).toHaveBeenCalled()

    const failureGate = createDeferredGate()
    const failureSpy = mockSdkService(
      AiController,
      "extractNotePreview",
      sampleExtractionPreview()
    )
    failureSpy.mockImplementation(async () => {
      await failureGate.gate
      return wrapSdkError({ message: "Preview failed" })
    })
    const failureWrapper = await mountNoteRefinementReady(["Test layout point"])

    await selectRefinementLayoutItem(failureWrapper, "p1")
    await clickExtractRefinementLayout(failureWrapper)
    await nextTick()

    expect(loadingModalMask()).toBeTruthy()
    failureGate.resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
    expectExtractionPreviewError(failureWrapper, "Preview failed")
  })

  it("shows LoadingModal while creating note from preview", async () => {
    const { gate, resolve } = createDeferredGate()
    mockSdkService(
      AiController,
      "extractNotePreview",
      sampleExtractionPreview()
    )
    mockSdkServiceWithImplementation(
      AiController,
      "createExtractedNote",
      async () => {
        await gate
        return makeMe.aNoteRealm.please()
      }
    )
    const wrapper = await mountNoteRefinementReady(["Test layout point"])

    await openExtractionPreview(wrapper, "p1")
    await clickCreateNoteFromExtractionPreview(wrapper)
    await nextTick()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is creating note...")
    resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
  })

  it("shows LoadingModal while retrying extract preview", async () => {
    const { gate, resolve } = createDeferredGate()
    let callCount = 0
    mockSdkServiceWithImplementation(
      AiController,
      "extractNotePreview",
      async () => {
        callCount++
        if (callCount === 1) {
          return sampleExtractionPreview()
        }
        await gate
        return labeledExtractionPreview("Retry")
      }
    )
    const wrapper = await mountNoteRefinementReady(["Test layout point"])

    await openExtractionPreview(wrapper, "p1")
    await clickRetryExtractionPreview(wrapper)
    await nextTick()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is generating preview...")
    resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
  })
})
