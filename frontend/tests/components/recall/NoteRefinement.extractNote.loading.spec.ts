import { AiController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import {
  clickCreateNoteFromExtractionPreview,
  clickRetryExtractionPreview,
  expectExtractionPreviewError,
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
  it("shows preview LoadingModal through initial success and retry failure", async () => {
    const initialPreviewGate = createDeferredGate()
    const retryPreviewGate = createDeferredGate()
    const previewSpy = mockSdkService(
      AiController,
      "extractNotePreview",
      sampleExtractionPreview()
    )
    previewSpy
      .mockImplementationOnce(async () => {
        await initialPreviewGate.gate
        return wrapSdkResponse(sampleExtractionPreview())
      })
      .mockImplementationOnce(async () => {
        await retryPreviewGate.gate
        return wrapSdkError({ message: "Preview failed" })
      })
    const wrapper = await mountNoteRefinementReady(["Test layout point"])
    await selectRefinementLayoutItem(wrapper, "p1")
    await clickExtractRefinementLayout(wrapper)
    await nextTick()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is generating preview...")
    initialPreviewGate.resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()

    await clickRetryExtractionPreview(wrapper)
    await nextTick()

    expect(loadingModalMask()).toBeTruthy()
    retryPreviewGate.resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
    expectExtractionPreviewError(wrapper, "Preview failed")
  })

  it("shows noncancelable LoadingModal while creating note", async () => {
    const createGate = createDeferredGate()
    mockSdkServiceWithImplementation(
      AiController,
      "createExtractedNote",
      async () => {
        await createGate.gate
        return makeMe.aNoteRealm.please()
      }
    )
    const wrapper = await mountNoteRefinementReady(["Test layout point"])

    await openExtractionPreview(wrapper, "p1")
    await clickCreateNoteFromExtractionPreview(wrapper)
    await nextTick()

    expect(loadingModalMask()).toBeTruthy()
    expect(document.body.textContent).toContain("AI is creating note...")
    expect(document.body.textContent).not.toContain("Cancel")
    createGate.resolve()
    await flushPromises()
    expect(loadingModalMask()).toBeNull()
  })
})
