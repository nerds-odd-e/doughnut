import { AiController } from "@generated/doughnut-backend-api/sdk.gen"
import { noteShowLocation } from "@/routes/noteShowLocation"
import usePopups from "@/components/commons/Popups/usePopups"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import { beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import {
  clickCreateNoteFromExtractionPreview,
  clickExtractionPreviewBack,
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
  createDeferredGate,
  loadingModalMask,
} from "./noteRefinementLayoutLoadingTestSupport"
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

describe("NoteRefinement extract note", () => {
  beforeEach(() => {
    routerReplace.mockResolvedValue(undefined)
  })

  describe("dialog-level action", () => {
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

    it("shows an editable preview after extracting selected layout points", async () => {
      const preview = sampleExtractionPreview({
        newNoteTitle: "Point 2 title",
        newNoteContent: "Point 2 content",
        updatedOriginalNoteContent: "Remaining content",
      })
      const extractNotePreviewSpy = mockSdkService(
        AiController,
        "extractNotePreview",
        preview
      )
      const wrapper = await mountNoteRefinementReady([...threePointLayoutTexts])

      await openExtractionPreview(wrapper, "p2")

      expect(extractNotePreviewSpy).toHaveBeenCalledWith(
        refinementLayoutSelectionApiCall(note.id, threePointLayout(), ["p2"])
      )
      expectExtractionPreviewVisible(wrapper)
      expectPreviewFields(wrapper, {
        newTitle: "Point 2 title",
        newContent: "Point 2 content",
        originalContent: "Remaining content",
      })
      expect(wrapper.findAll("li")).toHaveLength(0)
    })

    it("creates a note from the preview and navigates to the new note", async () => {
      const preview = sampleExtractionPreview()
      const createdRealm = makeMe.aNoteRealm.please()
      mockSdkService(AiController, "extractNotePreview", preview)
      const createExtractedNoteSpy = mockSdkService(
        AiController,
        "createExtractedNote",
        createdRealm
      )
      const wrapper = await mountNoteRefinementReady([...threePointLayoutTexts])

      await openExtractionPreview(wrapper, "p2")
      await createNoteFromExtractionPreview(wrapper)

      expect(createExtractedNoteSpy).toHaveBeenCalledWith(
        extractionPreviewApiCall(note.id, preview)
      )
      expect(routerReplace).toHaveBeenCalledWith(
        noteShowLocation(createdRealm.id)
      )
    })

    it("creates a note from edited preview fields", async () => {
      const preview = sampleExtractionPreview()
      const createdRealm = makeMe.aNoteRealm.please()
      mockSdkService(AiController, "extractNotePreview", preview)
      const createExtractedNoteSpy = mockSdkService(
        AiController,
        "createExtractedNote",
        createdRealm
      )
      const wrapper = await mountNoteRefinementReady([...threePointLayoutTexts])

      await openExtractionPreview(wrapper, "p2")
      await setPreviewFields(wrapper, {
        newTitle: "Edited title",
        newContent: "Edited content",
        originalContent: "Edited original content",
      })
      await createNoteFromExtractionPreview(wrapper)

      expect(createExtractedNoteSpy).toHaveBeenCalledWith(
        extractionPreviewApiCall(note.id, {
          newNoteTitle: "Edited title",
          newNoteContent: "Edited content",
          updatedOriginalNoteContent: "Edited original content",
        })
      )
    })

    it("toggles Create note disabled state from new note title", async () => {
      mockSdkService(
        AiController,
        "extractNotePreview",
        sampleExtractionPreview({ newNoteTitle: "" })
      )
      const createExtractedNoteSpy = mockSdkService(
        AiController,
        "createExtractedNote",
        makeMe.aNoteRealm.please()
      )
      const wrapper = await mountNoteRefinementReady([...threePointLayoutTexts])

      await openExtractionPreview(wrapper, "p2")
      const createButton = wrapper.find(
        '[data-test-id="extraction-preview-create"]'
      )
      expect((createButton.element as HTMLButtonElement).disabled).toBe(true)

      await setPreviewFields(wrapper, { newTitle: "New title" })
      await nextTick()
      expect((createButton.element as HTMLButtonElement).disabled).toBe(false)

      await setPreviewFields(wrapper, { newTitle: "" })
      await nextTick()
      expect((createButton.element as HTMLButtonElement).disabled).toBe(true)
      await createButton.trigger("click")
      await flushPromises()
      expect(createExtractedNoteSpy).not.toHaveBeenCalled()
    })

    it("extracts multiple selected layout points into one preview", async () => {
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
        refinementLayoutSelectionApiCall(note.id, layout, ["p1", "p3"])
      )
      expectExtractionPreviewVisible(wrapper)
    })

    it("replaces preview fields when Ask AI to retry is clicked", async () => {
      const layout = threePointLayout()
      const firstPreview = labeledExtractionPreview("First")
      const retryPreview = labeledExtractionPreview("Retry")
      const extractNotePreviewSpy = mockExtractNotePreviewResponses(
        firstPreview,
        retryPreview
      )
      const wrapper = await mountNoteRefinementWithLayoutReady(layout)

      await openExtractionPreview(wrapper, "p2")
      await retryExtractionPreview(wrapper)

      expect(extractNotePreviewSpy).toHaveBeenCalledTimes(2)
      expect(extractNotePreviewSpy).toHaveBeenNthCalledWith(
        2,
        refinementLayoutSelectionApiCall(note.id, layout, ["p2"])
      )
      expectPreviewFields(wrapper, extractionPreviewFieldsFor("Retry"))
      expectExtractionPreviewVisible(wrapper)
    })

    it("confirms retry when preview fields were edited, keeping edits on cancel and replacing on confirm", async () => {
      const layout = threePointLayout()
      const firstPreview = labeledExtractionPreview("First")
      const retryPreview = labeledExtractionPreview("Retry")
      const extractNotePreviewSpy = mockExtractNotePreviewResponses(
        firstPreview,
        retryPreview
      )
      const wrapper = await mountNoteRefinementWithLayoutReady(layout)

      await openExtractionPreview(wrapper, "p2")
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
      expect(extractNotePreviewSpy).toHaveBeenCalledTimes(1)

      usePopups().popups.done(false)
      await flushPromises()
      expect(extractNotePreviewSpy).toHaveBeenCalledTimes(1)
      expectPreviewFields(wrapper, {
        newTitle: "Edited title",
        newContent: "Edited content",
        originalContent: "Edited original",
      })

      await clickRetryExtractionPreview(wrapper)
      usePopups().popups.done(true)
      await flushPromises()
      expect(extractNotePreviewSpy).toHaveBeenCalledTimes(2)
      expectPreviewFields(wrapper, extractionPreviewFieldsFor("Retry"))
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

  describe("loading modal", () => {
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
      const successWrapper = await mountNoteRefinementReady([
        "Test layout point",
      ])

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
      const failureWrapper = await mountNoteRefinementReady([
        "Test layout point",
      ])

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
})
