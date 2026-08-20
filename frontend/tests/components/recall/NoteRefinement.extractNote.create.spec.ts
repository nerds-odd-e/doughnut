import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { nextTick } from "vue"
import { beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import type { NoteExtractionResult } from "@generated/doughnut-backend-api"
import {
  createNoteFromExtractionPreview,
  expectExtractionPreviewError,
  expectExtractionPreviewVisible,
  extractionPreviewApiCall,
  openExtractionPreview,
  setPreviewFields,
} from "./noteRefinementExtractionTestSupport"
import {
  mountNoteRefinementReady,
  note,
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

async function mountCreateReady(preview: NoteExtractionResult) {
  const createdRealm = makeMe.aNoteRealm.please()
  mockSdkService(AiController, "extractNotePreview", preview)
  const createExtractedNoteSpy = mockSdkService(
    AiController,
    "createExtractedNote",
    createdRealm
  )
  const wrapper = await mountNoteRefinementReady([...threePointLayoutTexts])
  await openExtractionPreview(wrapper, "p2")
  return { wrapper, createExtractedNoteSpy, createdRealm }
}

describe("NoteRefinement extract note create", () => {
  beforeEach(() => {
    routerReplace.mockResolvedValue(undefined)
  })

  it("toggles Create note from title and creates from edited preview fields", async () => {
    const preview = sampleExtractionPreview({ newNoteTitle: "" })
    const { wrapper, createExtractedNoteSpy, createdRealm } =
      await mountCreateReady(preview)
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
      "extractNotePreview",
      sampleExtractionPreview()
    )
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
