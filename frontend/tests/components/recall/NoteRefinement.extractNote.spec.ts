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
  wrapSdkResponse,
} from "@tests/helpers"
import {
  createNoteFromExtractionPreview,
  extractNoteButtonTitle,
  extractionPreviewApiCall,
  mountNoteRefinement,
  mountNoteRefinementWithLayout,
  note,
  openExtractionPreview,
  refinementLayoutSelectionApiCall,
  refinementLayoutItems,
  retryExtractionPreview,
  sampleExtractionPreview,
  selectRefinementLayoutItem,
  setupNoteRefinementTests,
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
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      const listItems = wrapper.findAll("li")
      expect(listItems).toHaveLength(3)
      listItems.forEach((li) => {
        expect(li.findAll("button")).toHaveLength(0)
      })
      const extractButtons = wrapper.findAll(
        `button[title="${extractNoteButtonTitle}"]`
      )
      expect(extractButtons).toHaveLength(1)
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
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      await openExtractionPreview(wrapper, "p2")

      expect(extractNotePreviewSpy).toHaveBeenCalledWith(
        refinementLayoutSelectionApiCall(
          note.id,
          refinementLayoutItems(["Point 1", "Point 2", "Point 3"]),
          ["p2"]
        )
      )
      expect(wrapper.find('[data-test-id="extraction-preview"]').exists()).toBe(
        true
      )
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-new-title"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Point 2 title")
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-new-content"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Point 2 content")
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-original-content"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Remaining content")
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
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

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
      const wrapper = mountNoteRefinement(["Point 1", "Point 2", "Point 3"])
      await flushPromises()

      await openExtractionPreview(wrapper, "p2")
      await wrapper
        .find('[data-test-id="extraction-preview-new-title"]')
        .setValue("Edited title")
      await wrapper
        .find('[data-test-id="extraction-preview-new-content"]')
        .setValue("Edited content")
      await wrapper
        .find('[data-test-id="extraction-preview-original-content"]')
        .setValue("Edited original content")
      await createNoteFromExtractionPreview(wrapper)

      expect(createExtractedNoteSpy).toHaveBeenCalledWith(
        extractionPreviewApiCall(note.id, {
          newNoteTitle: "Edited title",
          newNoteContent: "Edited content",
          updatedOriginalNoteContent: "Edited original content",
        })
      )
    })

    it("extracts multiple selected layout points into one preview", async () => {
      const extractNotePreviewSpy = mockSdkService(
        AiController,
        "extractNotePreview",
        sampleExtractionPreview()
      )
      const layout = refinementLayoutItems(["Point 1", "Point 2", "Point 3"])
      const wrapper = mountNoteRefinementWithLayout(layout)
      await flushPromises()

      await selectRefinementLayoutItem(wrapper, "p1")
      await selectRefinementLayoutItem(wrapper, "p3")
      await wrapper
        .find(`button[title="${extractNoteButtonTitle}"]`)
        .trigger("click")
      await flushPromises()

      expect(extractNotePreviewSpy).toHaveBeenCalledWith(
        refinementLayoutSelectionApiCall(note.id, layout, ["p1", "p3"])
      )
      expect(wrapper.find('[data-test-id="extraction-preview"]').exists()).toBe(
        true
      )
    })

    it("replaces preview fields when Ask AI to retry is clicked", async () => {
      const layout = refinementLayoutItems(["Point 1", "Point 2", "Point 3"])
      const firstPreview = sampleExtractionPreview({
        newNoteTitle: "First title",
        newNoteContent: "First content",
        updatedOriginalNoteContent: "First original",
      })
      const retryPreview = sampleExtractionPreview({
        newNoteTitle: "Retry title",
        newNoteContent: "Retry content",
        updatedOriginalNoteContent: "Retry original",
      })
      const extractNotePreviewSpy = mockSdkService(
        AiController,
        "extractNotePreview",
        firstPreview
      )
      extractNotePreviewSpy.mockResolvedValueOnce(wrapSdkResponse(firstPreview))
      extractNotePreviewSpy.mockResolvedValueOnce(wrapSdkResponse(retryPreview))
      const wrapper = mountNoteRefinementWithLayout(layout)
      await flushPromises()

      await openExtractionPreview(wrapper, "p2")
      await retryExtractionPreview(wrapper)

      expect(extractNotePreviewSpy).toHaveBeenCalledTimes(2)
      expect(extractNotePreviewSpy).toHaveBeenNthCalledWith(
        2,
        refinementLayoutSelectionApiCall(note.id, layout, ["p2"])
      )
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-new-title"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Retry title")
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-new-content"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Retry content")
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-original-content"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Retry original")
      expect(wrapper.find('[data-test-id="extraction-preview"]').exists()).toBe(
        true
      )
    })

    it("shows confirmation before retry when preview fields were edited", async () => {
      const layout = refinementLayoutItems(["Point 1", "Point 2", "Point 3"])
      const firstPreview = sampleExtractionPreview({
        newNoteTitle: "First title",
        newNoteContent: "First content",
        updatedOriginalNoteContent: "First original",
      })
      const extractNotePreviewSpy = mockSdkService(
        AiController,
        "extractNotePreview",
        firstPreview
      )
      const wrapper = mountNoteRefinementWithLayout(layout)
      await flushPromises()

      await openExtractionPreview(wrapper, "p2")
      await wrapper
        .find('[data-test-id="extraction-preview-new-title"]')
        .setValue("Edited title")
      await retryExtractionPreview(wrapper)

      const popups = usePopups().popups.peek()
      expect(popups).toHaveLength(1)
      expect(popups[0]!.type).toBe("confirm")
      expect(popups[0]!.message).toContain("discard")
      expect(extractNotePreviewSpy).toHaveBeenCalledTimes(1)
    })

    it("discards edited preview fields when retry is confirmed", async () => {
      const layout = refinementLayoutItems(["Point 1", "Point 2", "Point 3"])
      const firstPreview = sampleExtractionPreview({
        newNoteTitle: "First title",
        newNoteContent: "First content",
        updatedOriginalNoteContent: "First original",
      })
      const retryPreview = sampleExtractionPreview({
        newNoteTitle: "Retry title",
        newNoteContent: "Retry content",
        updatedOriginalNoteContent: "Retry original",
      })
      const extractNotePreviewSpy = mockSdkService(
        AiController,
        "extractNotePreview",
        firstPreview
      )
      extractNotePreviewSpy.mockResolvedValueOnce(wrapSdkResponse(firstPreview))
      extractNotePreviewSpy.mockResolvedValueOnce(wrapSdkResponse(retryPreview))
      const wrapper = mountNoteRefinementWithLayout(layout)
      await flushPromises()

      await openExtractionPreview(wrapper, "p2")
      await wrapper
        .find('[data-test-id="extraction-preview-new-title"]')
        .setValue("Edited title")
      await wrapper
        .find('[data-test-id="retry-extraction-preview"]')
        .trigger("click")
      usePopups().popups.done(true)
      await flushPromises()

      expect(extractNotePreviewSpy).toHaveBeenCalledTimes(2)
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-new-title"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Retry title")
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-new-content"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Retry content")
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-original-content"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Retry original")
    })

    it("keeps edited preview fields when retry is cancelled", async () => {
      const layout = refinementLayoutItems(["Point 1", "Point 2", "Point 3"])
      const firstPreview = sampleExtractionPreview({
        newNoteTitle: "First title",
        newNoteContent: "First content",
        updatedOriginalNoteContent: "First original",
      })
      const extractNotePreviewSpy = mockSdkService(
        AiController,
        "extractNotePreview",
        firstPreview
      )
      const wrapper = mountNoteRefinementWithLayout(layout)
      await flushPromises()

      await openExtractionPreview(wrapper, "p2")
      await wrapper
        .find('[data-test-id="extraction-preview-new-title"]')
        .setValue("Edited title")
      await wrapper
        .find('[data-test-id="extraction-preview-new-content"]')
        .setValue("Edited content")
      await wrapper
        .find('[data-test-id="extraction-preview-original-content"]')
        .setValue("Edited original")
      await wrapper
        .find('[data-test-id="retry-extraction-preview"]')
        .trigger("click")
      usePopups().popups.done(false)
      await flushPromises()

      expect(extractNotePreviewSpy).toHaveBeenCalledTimes(1)
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-new-title"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Edited title")
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-new-content"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Edited content")
      expect(
        (
          wrapper.find('[data-test-id="extraction-preview-original-content"]')
            .element as HTMLTextAreaElement
        ).value
      ).toBe("Edited original")
    })

    it("returns to the layout when Back is clicked", async () => {
      mockSdkService(
        AiController,
        "extractNotePreview",
        sampleExtractionPreview()
      )
      const wrapper = mountNoteRefinement(["Test Point"])
      await flushPromises()

      await openExtractionPreview(wrapper, "p1")
      await wrapper
        .find('[data-test-id="extraction-preview-back"]')
        .trigger("click")
      await flushPromises()

      expect(wrapper.find('[data-test-id="extraction-preview"]').exists()).toBe(
        false
      )
      expect(wrapper.findAll("li")).toHaveLength(1)
      expect(wrapper.text()).toContain("Test Point")
    })

    it("keeps layout visible when preview API fails", async () => {
      mockSdkService(
        AiController,
        "extractNotePreview",
        undefined
      ).mockResolvedValue(wrapSdkError("API Error"))
      const wrapper = mountNoteRefinement(["Test Point"])
      await flushPromises()

      await selectRefinementLayoutItem(wrapper, "p1")
      await wrapper
        .find(`button[title="${extractNoteButtonTitle}"]`)
        .trigger("click")
      await flushPromises()

      expect(wrapper.find('[data-test-id="extraction-preview"]').exists()).toBe(
        false
      )
      expect(wrapper.findAll("li")).toHaveLength(1)
      expect(wrapper.text()).toContain("Test Point")
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
      const wrapper = mountNoteRefinement(["Test Point"])
      await flushPromises()

      await openExtractionPreview(wrapper, "p1")
      await createNoteFromExtractionPreview(wrapper)

      expect(wrapper.find('[data-test-id="extraction-preview"]').exists()).toBe(
        true
      )
      expect(
        wrapper.find('[data-test-id="extraction-preview-error"]').text()
      ).toBe("Title is reserved")
      expect(routerReplace).not.toHaveBeenCalled()
    })
  })

  describe("loading modal", () => {
    it("shows LoadingModal while generating extract preview", async () => {
      let resolveApi: () => void
      const apiPromise = new Promise<void>((r) => {
        resolveApi = r
      })
      mockSdkServiceWithImplementation(
        AiController,
        "extractNotePreview",
        async () => {
          await apiPromise
          return sampleExtractionPreview()
        }
      )
      const wrapper = mountNoteRefinement(["Test layout point"])
      await flushPromises()

      await selectRefinementLayoutItem(wrapper, "p1")
      await wrapper
        .find(`button[title="${extractNoteButtonTitle}"]`)
        .trigger("click")
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      expect(document.body.textContent).toContain("AI is generating preview...")
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })

    it("shows LoadingModal while creating note from preview", async () => {
      let resolveApi: () => void
      const apiPromise = new Promise<void>((r) => {
        resolveApi = r
      })
      mockSdkService(
        AiController,
        "extractNotePreview",
        sampleExtractionPreview()
      )
      mockSdkServiceWithImplementation(
        AiController,
        "createExtractedNote",
        async () => {
          await apiPromise
          return makeMe.aNoteRealm.please()
        }
      )
      const wrapper = mountNoteRefinement(["Test layout point"])
      await flushPromises()

      await openExtractionPreview(wrapper, "p1")
      await wrapper
        .find('[data-test-id="extraction-preview-create"]')
        .trigger("click")
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      expect(document.body.textContent).toContain("AI is creating note...")
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })

    it("hides LoadingModal when preview API fails", async () => {
      let resolveApi: () => void
      const apiGate = new Promise<void>((r) => {
        resolveApi = r
      })
      mockSdkServiceWithImplementation(
        AiController,
        "extractNotePreview",
        async () => {
          await apiGate
          return wrapSdkError({})
        }
      )
      const wrapper = mountNoteRefinement(["Test layout point"])
      await flushPromises()

      await selectRefinementLayoutItem(wrapper, "p1")
      await wrapper
        .find(`button[title="${extractNoteButtonTitle}"]`)
        .trigger("click")
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })

    it("shows LoadingModal while retrying extract preview", async () => {
      let resolveApi: () => void
      const apiPromise = new Promise<void>((r) => {
        resolveApi = r
      })
      let callCount = 0
      mockSdkServiceWithImplementation(
        AiController,
        "extractNotePreview",
        async () => {
          callCount++
          if (callCount === 1) {
            return sampleExtractionPreview()
          }
          await apiPromise
          return sampleExtractionPreview({
            newNoteTitle: "Retry title",
            newNoteContent: "Retry content",
            updatedOriginalNoteContent: "Retry original",
          })
        }
      )
      const wrapper = mountNoteRefinement(["Test layout point"])
      await flushPromises()

      await openExtractionPreview(wrapper, "p1")
      await wrapper
        .find('[data-test-id="retry-extraction-preview"]')
        .trigger("click")
      await nextTick()

      expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
      expect(document.body.textContent).toContain("AI is generating preview...")
      resolveApi!()
      await flushPromises()
      expect(document.querySelector(".loading-modal-mask")).toBeNull()
    })
  })
})
