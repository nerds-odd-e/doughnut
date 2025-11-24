import NoteEditableDetails from "@/components/notes/core/NoteEditableDetails.vue"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import helper from "@tests/helpers"
import * as sdk from "@generated/backend/sdk.gen"

const mockedUpdateDetailsCall = vi.fn()

describe("NoteEditableDetails", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.spyOn(sdk, "updateNoteDetails").mockImplementation(async (options) => {
      const result = await mockedUpdateDetailsCall(options)
      return {
        data: result,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }
    })
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

  it("should preserve cursor position when API response updates the value", async () => {
    vi.useFakeTimers()

    const noteId = 1
    const originalDetails = "Original note details text"

    // Mock the API to return the same value (simulating API response)
    mockedUpdateDetailsCall.mockResolvedValue({
      id: noteId,
      details: originalDetails,
    } as never)

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withStorageProps({
        noteId: noteId,
        noteDetails: originalDetails,
        readonly: false,
        asMarkdown: true,
      })
      .mount()

    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement

    // Set cursor position in the middle of the text
    detailsEl.focus()
    detailsEl.setSelectionRange(15, 15) // Position cursor after "Original note"
    expect(detailsEl.selectionStart).toBe(15)
    expect(detailsEl.selectionEnd).toBe(15)

    // Edit the details (this triggers a debounced save)
    detailsEl.value = originalDetails
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    // Wait for debounce timeout and API response
    vi.advanceTimersByTime(1000)
    await flushPromises()

    // Simulate the API response updating the value prop
    // This happens when refreshNoteRealm is called after the API response
    await wrapper.setProps({
      noteId: noteId,
      noteDetails: originalDetails, // Same value from API response
    })
    await flushPromises()

    // The cursor position should be preserved
    // If the bug exists, the cursor would jump to position 0
    expect(detailsEl.selectionStart).toBe(15)
    expect(detailsEl.selectionEnd).toBe(15)

    vi.useRealTimers()
  })
})
