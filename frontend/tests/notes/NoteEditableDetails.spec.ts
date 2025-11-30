import NoteEditableDetails from "@/components/notes/core/NoteEditableDetails.vue"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import type { UpdateNoteDetailsData } from "@generated/backend"

describe("NoteEditableDetails", () => {
  let updateNoteDetailsSpy: ReturnType<
    typeof mockSdkService<"updateNoteDetails">
  >

  beforeEach(() => {
    vi.resetAllMocks()
    updateNoteDetailsSpy = mockSdkService(
      "updateNoteDetails",
      makeMe.aNoteRealm.please()
    )
  })

  it("should not save previous note's details to the new note when navigating", async () => {
    const firstNoteId = 1
    const secondNoteId = 2

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId: firstNoteId,
        noteDetails: "First note details",
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details from first note"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    await wrapper.setProps({
      noteId: secondNoteId,
      noteDetails: "Second note details",
    })
    await flushPromises()

    expect(detailsEl.value).toBe("Second note details")

    detailsEl.value = "New edits on second note"
    detailsEl.dispatchEvent(new Event("input"))
    detailsEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    const calls = updateNoteDetailsSpy.mock.calls as Array<
      [UpdateNoteDetailsData]
    >
    expect(
      calls.some(
        (call) =>
          call[0].path?.note === secondNoteId &&
          call[0].body?.details === "Edited details from first note"
      )
    ).toBe(false)
    expect(calls.some((call) => call[0].path?.note === firstNoteId)).toBe(false)
    if (calls.length > 0) {
      expect(
        calls.some(
          (call) =>
            call[0].path?.note === secondNoteId &&
            call[0].body?.details === "New edits on second note"
        )
      ).toBe(true)
    }
  })

  it("should update displayed details when navigating to a different note with no unsaved changes", async () => {
    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId: 1,
        noteDetails: "First note details",
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()

    await wrapper.setProps({
      noteId: 2,
      noteDetails: "Second note details",
    })
    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    expect(detailsEl.value).toBe("Second note details")
  })

  it("should preserve unsaved edits if the noteDetails prop doesn't actually change", async () => {
    const noteId = 1
    const noteDetails = "Original details"

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId,
        noteDetails,
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    await wrapper.setProps({
      noteId,
      noteDetails,
      readonly: false,
    })
    await flushPromises()

    expect(detailsEl.value).toBe("Edited details")

    detailsEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { details: "Edited details" },
    })
  })

  it("should save edited details to the correct note on blur before navigation", async () => {
    const firstNoteId = 1

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId: firstNoteId,
        noteDetails: "First note details",
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details"
    detailsEl.dispatchEvent(new Event("input"))
    detailsEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
      path: { note: firstNoteId },
      body: { details: "Edited details" },
    })
  })

  it("should auto-save edited details after debounce timeout without blur", async () => {
    vi.useFakeTimers()

    const noteId = 1
    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId,
        noteDetails: "Original details",
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    expect(wrapper.find(".dirty").exists()).toBe(true)

    vi.advanceTimersByTime(1000)
    await flushPromises()

    expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { details: "Edited details" },
    })
    expect(wrapper.find(".dirty").exists()).toBe(false)

    vi.useRealTimers()
  })

  it("should preserve second edit when first save response arrives after second edit", async () => {
    const noteId = 1
    let resolveFirstSave: (() => void) | undefined
    const firstSavePromise = new Promise<void>((resolve) => {
      resolveFirstSave = resolve
    })

    updateNoteDetailsSpy.mockImplementation((async (
      options: UpdateNoteDetailsData
    ) => {
      if (options.body?.details === "First edit") {
        await firstSavePromise
      }
      return wrapSdkResponse({
        id: noteId,
        note: {
          id: noteId,
          details: options.body?.details,
          noteTopology: { id: noteId, titleOrPredicate: "Test Note" },
        },
      })
      // biome-ignore lint/suspicious/noExplicitAny: Vitest mock typing requires any for implementation functions
    }) as any)

    const wrapper = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId,
        noteDetails: "Original",
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()
    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement

    detailsEl.value = "First edit"
    detailsEl.dispatchEvent(new Event("input"))
    detailsEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    detailsEl.value = "Second edit"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()
    expect(detailsEl.value).toBe("Second edit")

    resolveFirstSave!()
    await wrapper.setProps({ noteDetails: "First edit" })
    await flushPromises()

    expect(detailsEl.value).toBe("Second edit")
  })
})
