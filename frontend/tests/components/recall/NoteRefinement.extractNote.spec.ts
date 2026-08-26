import { AiController } from "@generated/donut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it } from "vitest"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import {
  clickExtractionPreviewBack,
  clickRetryExtractionPreview,
  expectExtractionPreviewError,
  expectExtractionPreviewVisible,
  expectPreviewFields,
  extractionPreviewFieldsFor,
  labeledExtractionPreview,
  mockExtractNotePreviewResponses,
  openExtractionPreview,
  retryExtractionPreview,
  setPreviewFields,
} from "./noteRefinementExtractionTestSupport"
import {
  clickExtractRefinementLayout,
  mountNoteRefinementReady,
  mountNoteRefinementWithLayoutReady,
  note,
  refinementLayoutSelectionApiCall,
  sampleExtractionPreview,
  selectRefinementLayoutItem,
  setupNoteRefinementTests,
  threePointLayout,
  threePointLayoutTexts,
} from "./noteRefinementTestSupport"

setupNoteRefinementTests()

describe("NoteRefinement extract note preview", () => {
  it("displays one extract button and no per-item extract buttons", async () => {
    const wrapper = await mountNoteRefinementReady([...threePointLayoutTexts])

    const listItems = wrapper.findAll("li")
    expect(listItems).toHaveLength(3)
    listItems.forEach((li) => {
      expect(li.findAll("button")).toHaveLength(0)
    })
    expect(
      wrapper.findAll('[data-test-id="extract-refinement-layout"]')
    ).toHaveLength(1)
  })

  it("shows editable preview, retries without confirm, and confirms when fields were edited", async () => {
    const layout = threePointLayout()
    const firstPreview = labeledExtractionPreview("First")
    const retryPreview = labeledExtractionPreview("Retry")
    const confirmedPreview = labeledExtractionPreview("Confirmed")
    const extractNotePreviewSpy = mockExtractNotePreviewResponses(
      firstPreview,
      retryPreview,
      confirmedPreview
    )
    const wrapper = await mountNoteRefinementWithLayoutReady(layout)

    await openExtractionPreview(wrapper, "p2")

    expect(extractNotePreviewSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, layout, ["p2"], {
        signal: true,
      })
    )
    expectExtractionPreviewVisible(wrapper)
    expectPreviewFields(wrapper, extractionPreviewFieldsFor("First"))
    expect(wrapper.findAll("li")).toHaveLength(0)

    await retryExtractionPreview(wrapper)

    expect(extractNotePreviewSpy).toHaveBeenCalledTimes(2)
    expect(extractNotePreviewSpy).toHaveBeenNthCalledWith(
      2,
      refinementLayoutSelectionApiCall(note.id, layout, ["p2"], {
        signal: true,
      })
    )
    expectPreviewFields(wrapper, extractionPreviewFieldsFor("Retry"))

    await setPreviewFields(wrapper, {
      newTitle: "Edited title",
      newContent: "Edited content",
      originalContent: "Edited original",
    })
    await clickRetryExtractionPreview(wrapper)

    const popups = usePopups().popups.peek()
    expect(popups).toHaveLength(1)
    expect(popups[0]!.type).toBe("confirm")
    expect(popups[0]!.message).toContain("discard")
    expect(extractNotePreviewSpy).toHaveBeenCalledTimes(2)

    usePopups().popups.done(false)
    await flushPromises()
    expect(extractNotePreviewSpy).toHaveBeenCalledTimes(2)
    expectPreviewFields(wrapper, {
      newTitle: "Edited title",
      newContent: "Edited content",
      originalContent: "Edited original",
    })

    await clickRetryExtractionPreview(wrapper)
    usePopups().popups.done(true)
    await flushPromises()
    expect(extractNotePreviewSpy).toHaveBeenCalledTimes(3)
    expectPreviewFields(wrapper, extractionPreviewFieldsFor("Confirmed"))
  })

  it("extracts multiple selected refinement layout items into one preview", async () => {
    const extractNotePreviewSpy = mockSdkService(
      AiController,
      "extractNotePreview",
      sampleExtractionPreview()
    )
    const layout = threePointLayout()
    const wrapper = await mountNoteRefinementWithLayoutReady(layout)

    await selectRefinementLayoutItem(wrapper, "p1")
    await selectRefinementLayoutItem(wrapper, "p3")
    await clickExtractRefinementLayout(wrapper)
    await flushPromises()

    expect(extractNotePreviewSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, layout, ["p1", "p3"], {
        signal: true,
      })
    )
    expectExtractionPreviewVisible(wrapper)
  })

  it("returns to the layout when Back is clicked", async () => {
    mockSdkService(
      AiController,
      "extractNotePreview",
      sampleExtractionPreview()
    )
    const wrapper = await mountNoteRefinementReady(["Test Point"])

    await openExtractionPreview(wrapper, "p1")
    await clickExtractionPreviewBack(wrapper)
    await flushPromises()

    expectExtractionPreviewVisible(wrapper, false)
    expect(wrapper.findAll("li")).toHaveLength(1)
    expect(wrapper.text()).toContain("Test Point")
  })

  it("shows inline error when extract preview API fails", async () => {
    mockSdkService(
      AiController,
      "extractNotePreview",
      undefined
    ).mockResolvedValue(wrapSdkError({ message: "API Error" }))
    const wrapper = await mountNoteRefinementReady(["Test Point"])

    await selectRefinementLayoutItem(wrapper, "p1")
    await clickExtractRefinementLayout(wrapper)
    await flushPromises()

    expectExtractionPreviewVisible(wrapper)
    expectExtractionPreviewError(wrapper, "API Error")
    expect(usePopups().popups.peek()).toHaveLength(0)
  })

  it("shows inline error when retry preview API fails", async () => {
    const layout = threePointLayout()
    const extractNotePreviewSpy = mockExtractNotePreviewResponses(
      sampleExtractionPreview()
    )
    extractNotePreviewSpy.mockResolvedValueOnce(
      wrapSdkError({ message: "Retry failed" })
    )
    const wrapper = await mountNoteRefinementWithLayoutReady(layout)

    await openExtractionPreview(wrapper, "p2")
    await retryExtractionPreview(wrapper)

    expectExtractionPreviewVisible(wrapper)
    expectExtractionPreviewError(wrapper, "Retry failed")
    expect(usePopups().popups.peek()).toHaveLength(0)
  })
})
