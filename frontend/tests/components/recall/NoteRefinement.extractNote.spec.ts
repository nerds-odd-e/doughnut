import { AiController } from "@generated/donut-backend-api/sdk.gen"
import usePopups from "@/components/commons/Popups/usePopups"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import { beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import {
  clickRetryExtractionPreview,
  createNoteFromExtractionPreview,
  expectExtractionPreviewError,
  expectExtractionPreviewVisible,
  expectPreviewFields,
  extractionPreviewApiCall,
  extractionPreviewFieldsFor,
  labeledExtractionPreview,
  mockExtractNotePreviewResponses,
  openExtractionPreview,
  retryExtractionPreview,
  setPreviewFields,
} from "./noteRefinementExtractionTestSupport"
import {
  mountNoteRefinementReady,
  mountNoteRefinementWithLayoutReady,
  note,
  refinementLayoutItems,
  refinementLayoutSelectionApiCall,
  sampleExtractionPreview,
  setupNoteRefinementTests,
  threePointLayoutTexts,
} from "./noteRefinementTestSupport"

const routerReplace = vi.fn()

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      replace: routerReplace,
    }),
  }
})

setupNoteRefinementTests()

describe("NoteRefinement extract note preview", () => {
  beforeEach(() => {
    routerReplace.mockResolvedValue(undefined)
  })

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

  it("retries an editable preview and shows retry errors after confirming edited fields", async () => {
    const layout = refinementLayoutItems(["Point"])
    const firstPreview = labeledExtractionPreview("First")
    const retryPreview = labeledExtractionPreview("Retry")
    const extractNotePreviewSpy = mockExtractNotePreviewResponses(
      firstPreview,
      retryPreview
    )
    extractNotePreviewSpy.mockResolvedValueOnce(
      wrapSdkError({ message: "Retry failed" })
    )
    const wrapper = await mountNoteRefinementWithLayoutReady(layout)

    await openExtractionPreview(wrapper, "p1")

    expect(extractNotePreviewSpy).toHaveBeenCalledWith(
      refinementLayoutSelectionApiCall(note.id, layout, ["p1"], {
        signal: true,
      })
    )
    expectExtractionPreviewVisible(wrapper)
    expectPreviewFields(wrapper, extractionPreviewFieldsFor("First"))

    await retryExtractionPreview(wrapper)

    expect(extractNotePreviewSpy).toHaveBeenCalledTimes(2)
    expectPreviewFields(wrapper, extractionPreviewFieldsFor("Retry"))

    await setPreviewFields(wrapper, {
      newTitle: "Edited title",
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
    expectPreviewFields(wrapper, { newTitle: "Edited title" })

    await clickRetryExtractionPreview(wrapper)
    usePopups().popups.done(true)
    await flushPromises()
    expect(extractNotePreviewSpy).toHaveBeenCalledTimes(3)
    expectExtractionPreviewError(wrapper, "Retry failed")
    expect(usePopups().popups.peek()).toHaveLength(0)
  })

  it("shows inline error when extract preview API fails", async () => {
    mockSdkService(
      AiController,
      "extractNotePreview",
      undefined
    ).mockResolvedValue(wrapSdkError({ message: "API Error" }))
    const wrapper = await mountNoteRefinementReady(["Test Point"])

    await openExtractionPreview(wrapper, "p1")

    expectExtractionPreviewError(wrapper, "API Error")
    expect(usePopups().popups.peek()).toHaveLength(0)
  })

  it("toggles Create note from title and creates from edited preview fields", async () => {
    const createdRealm = makeMe.aNoteRealm.please()
    mockSdkService(
      AiController,
      "extractNotePreview",
      sampleExtractionPreview({ newNoteTitle: "" })
    )
    const createExtractedNoteSpy = mockSdkService(
      AiController,
      "createExtractedNote",
      createdRealm
    )
    const wrapper = await mountNoteRefinementReady([...threePointLayoutTexts])
    await openExtractionPreview(wrapper, "p2")
    const createButton = wrapper.find(
      '[data-test-id="extraction-preview-create"]'
    )
    expect((createButton.element as HTMLButtonElement).disabled).toBe(true)

    await setPreviewFields(wrapper, {
      newTitle: "Edited title",
      newContent: "Edited content",
      originalContent: "Edited original content",
    })
    await nextTick()
    expect((createButton.element as HTMLButtonElement).disabled).toBe(false)

    await createNoteFromExtractionPreview(wrapper)

    expect(createExtractedNoteSpy).toHaveBeenCalledWith(
      extractionPreviewApiCall(note.id, {
        newNoteTitle: "Edited title",
        newNoteContent: "Edited content",
        updatedOriginalNoteContent: "Edited original content",
      })
    )
    expect(routerReplace).toHaveBeenCalledWith(
      noteShowLocation(createdRealm.id)
    )
  })

  it("shows create errors in the preview", async () => {
    mockSdkService(
      AiController,
      "createExtractedNote",
      undefined
    ).mockResolvedValue(wrapSdkError({ message: "Title is reserved" }))
    const wrapper = await mountNoteRefinementReady(["Test Point"])

    await openExtractionPreview(wrapper, "p1")
    await createNoteFromExtractionPreview(wrapper)

    expectExtractionPreviewVisible(wrapper)
    expectExtractionPreviewError(wrapper, "Title is reserved")
    expect(routerReplace).not.toHaveBeenCalled()
  })
})
