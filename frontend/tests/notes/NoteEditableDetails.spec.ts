import NoteEditableDetails from "@/components/notes/core/NoteEditableDetails.vue"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import helper from "@tests/helpers"

const mockedUpdateDetailsCall = vi.fn()

describe("NoteEditableDetails", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.spyOn(
      helper.managedApi.services,
      "updateNoteDetails"
    ).mockImplementation(mockedUpdateDetailsCall)
  })

  it("should not save previous note's details to the new note when navigating", async () => {
    // Mount component with first note
    const firstNoteId = 1
    const firstNoteDetails = "First note details"

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withStorageProps({
        noteId: firstNoteId,
        noteDetails: firstNoteDetails,
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()

    // Edit the details
    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details from first note"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    // Navigate to a different note by changing props
    const secondNoteId = 2
    const secondNoteDetails = "Second note details"

    await wrapper.setProps({
      noteId: secondNoteId,
      noteDetails: secondNoteDetails,
    })
    await flushPromises()

    // The fix: After navigation, the displayed content should update to show the new note's details
    // The old unsaved changes should be discarded
    expect(detailsEl.value).toBe("Second note details")

    // If user makes changes now, they should be associated with the second note
    detailsEl.value = "New edits on second note"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    // Now blur - this will trigger a save
    detailsEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    // Verify that any saves are for the second note with the new content
    const calls = mockedUpdateDetailsCall.mock.calls

    // Should NOT have saved the first note's edited content to the second note
    const savedOldContentToSecondNote = calls.some(
      (call) =>
        call[0].path?.note === secondNoteId &&
        call[0].body.details === "Edited details from first note"
    )
    expect(savedOldContentToSecondNote).toBe(false)

    // Should NOT have saved the first note's edited content to the first note either
    // (because navigation cancelled the pending save)
    const savedToFirstNote = calls.some(
      (call) => call[0].path?.note === firstNoteId
    )
    expect(savedToFirstNote).toBe(false)

    // Should have saved the new content to the second note
    if (calls.length > 0) {
      const savedNewContentToSecondNote = calls.some(
        (call) =>
          call[0].path?.note === secondNoteId &&
          call[0].body.details === "New edits on second note"
      )
      expect(savedNewContentToSecondNote).toBe(true)
    }
  })

  it("should update displayed details when navigating to a different note with no unsaved changes", async () => {
    // Mount component with first note
    const firstNoteId = 1
    const firstNoteDetails = "First note details"

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withStorageProps({
        noteId: firstNoteId,
        noteDetails: firstNoteDetails,
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()

    // Navigate to a different note without editing
    const secondNoteId = 2
    const secondNoteDetails = "Second note details"

    await wrapper.setProps({
      noteId: secondNoteId,
      noteDetails: secondNoteDetails,
    })
    await flushPromises()

    // Verify the displayed content is updated
    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    expect(detailsEl.value).toBe(secondNoteDetails)
  })

  it("should preserve unsaved edits if the noteDetails prop doesn't actually change", async () => {
    // This test ensures we don't break the behavior where minor prop changes
    // (like other fields of the note object) don't discard unsaved edits
    const noteId = 1
    const noteDetails = "Original details"

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withStorageProps({
        noteId: noteId,
        noteDetails: noteDetails,
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()

    // Edit the details
    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    // Change props but keep the same noteId and noteDetails
    // (simulating a re-render with updated other fields)
    await wrapper.setProps({
      noteId: noteId,
      noteDetails: noteDetails, // Same value
      readonly: false,
    })
    await flushPromises()

    // The edited content should still be there
    expect(detailsEl.value).toBe("Edited details")

    // And it should eventually save
    detailsEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    expect(mockedUpdateDetailsCall).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { details: "Edited details" },
    })
  })

  it("should save edited details to the correct note on blur before navigation", async () => {
    // Mount component with first note
    const firstNoteId = 1
    const firstNoteDetails = "First note details"

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withStorageProps({
        noteId: firstNoteId,
        noteDetails: firstNoteDetails,
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()

    // Edit and blur
    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    detailsEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    // Should have saved to the first note
    expect(mockedUpdateDetailsCall).toHaveBeenCalledWith({
      path: { note: firstNoteId },
      body: { details: "Edited details" },
    })
  })

  it("should auto-save edited details after debounce timeout without blur", async () => {
    // This test reproduces the bug: content should be saved by debounced timer
    // even without blur event
    vi.useFakeTimers()

    const noteId = 1
    const noteDetails = "Original details"

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withStorageProps({
        noteId: noteId,
        noteDetails: noteDetails,
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()

    // Edit the details
    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    // Should show dirty indicator
    expect(wrapper.find(".dirty").exists()).toBe(true)

    // Wait for debounce timeout (1000ms)
    vi.advanceTimersByTime(1000)
    await flushPromises()

    // Should have auto-saved
    expect(mockedUpdateDetailsCall).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { details: "Edited details" },
    })

    // Dirty indicator should be gone after save completes
    expect(wrapper.find(".dirty").exists()).toBe(false)

    vi.useRealTimers()
  })
})
