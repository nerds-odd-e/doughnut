import type { UpdateNoteContentData } from "@generated/doughnut-backend-api"
import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import {
  blurTextarea,
  mockDelayedFirstSave,
  mountMarkdownTextarea,
  mountNoteEditableContent,
  setTextareaValue,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  textareaEl,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableContent", () => {
  let updateNoteContentSpy: ReturnType<typeof setupUpdateNoteContentMock>
  // biome-ignore lint/suspicious/noExplicitAny: Mock type for testing
  let mockPopupsOptions: any

  beforeEach(() => {
    vi.resetAllMocks()
    updateNoteContentSpy = setupUpdateNoteContentMock()
    mockPopupsOptions = vi.fn().mockResolvedValue(null)
    setupPopupsMock(mockPopupsOptions)
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("should not save previous note's content to the new note when navigating", async () => {
    const firstNoteId = 1
    const secondNoteId = 2

    const wrapper = mountNoteEditableContent(
      { noteId: firstNoteId, noteContent: "First note content" },
      { attachTo: document.body }
    )
    await flushPromises()

    const contentTextarea = textareaEl(wrapper)
    await setTextareaValue(wrapper, "Edited content from first note")

    await wrapper.setProps({
      noteId: secondNoteId,
      noteContent: "Second note content",
    })
    await flushPromises()

    expect(contentTextarea.value).toBe("Second note content")

    contentTextarea.value = "New edits on second note"
    contentTextarea.dispatchEvent(new Event("input"))
    contentTextarea.dispatchEvent(new Event("blur"))
    await flushPromises()

    const calls = updateNoteContentSpy.mock.calls as Array<
      [UpdateNoteContentData]
    >
    expect(
      calls.some(
        (call) =>
          call[0].path?.note === secondNoteId &&
          call[0].body?.content === "Edited content from first note"
      )
    ).toBe(false)
    expect(calls.some((call) => call[0].path?.note === firstNoteId)).toBe(false)
    if (calls.length > 0) {
      expect(
        calls.some(
          (call) =>
            call[0].path?.note === secondNoteId &&
            call[0].body?.content === "New edits on second note"
        )
      ).toBe(true)
    }
    wrapper.unmount()
  })

  it("should update displayed content when navigating to a different note with no unsaved changes", async () => {
    const wrapper = mountNoteEditableContent(
      { noteId: 1, noteContent: "First note content" },
      { attachTo: document.body }
    )
    await flushPromises()

    await wrapper.setProps({
      noteId: 2,
      noteContent: "Second note content",
    })
    await flushPromises()

    expect(textareaEl(wrapper).value).toBe("Second note content")
    wrapper.unmount()
  })

  it("should preserve unsaved edits if the noteContent prop doesn't actually change", async () => {
    const noteId = 1
    const noteContent = "Original content"

    const wrapper = await mountMarkdownTextarea({ noteId, noteContent })
    await setTextareaValue(wrapper, "Edited content")

    await wrapper.setProps({ noteId, noteContent, readonly: false })

    expect(textareaEl(wrapper).value).toBe("Edited content")

    await blurTextarea(wrapper)

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "Edited content" },
    })
    wrapper.unmount()
  })

  it("should save edited content to the correct note on blur before navigation", async () => {
    const firstNoteId = 1

    const wrapper = mountNoteEditableContent(
      { noteId: firstNoteId, noteContent: "First note content" },
      { attachTo: document.body }
    )
    await flushPromises()

    await setTextareaValue(wrapper, "Edited content")
    await blurTextarea(wrapper)

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: firstNoteId },
      body: { content: "Edited content" },
    })
    wrapper.unmount()
  })

  it("should preserve second edit when first save response arrives after second edit", async () => {
    const noteId = 1
    const resolveFirstSave = mockDelayedFirstSave(updateNoteContentSpy, noteId)

    const wrapper = await mountMarkdownTextarea({
      noteId,
      noteContent: "Original",
    })

    await setTextareaValue(wrapper, "First edit")
    await blurTextarea(wrapper)

    await setTextareaValue(wrapper, "Second edit")
    expect(textareaEl(wrapper).value).toBe("Second edit")

    resolveFirstSave()
    await wrapper.setProps({ noteContent: "First edit" })
    await flushPromises()

    expect(textareaEl(wrapper).value).toBe("Second edit")
    wrapper.unmount()
  })

  it("should clear content when switching from a note with content to a note without content (undefined)", async () => {
    const wrapper = mountNoteEditableContent(
      {
        noteId: 1,
        noteContent: "This is the first note's content",
      },
      { attachTo: document.body }
    )
    await flushPromises()

    expect(textareaEl(wrapper).value).toBe("This is the first note's content")

    await wrapper.setProps({
      noteId: 2,
      noteContent: undefined,
    })
    await flushPromises()

    expect(textareaEl(wrapper).value).not.toContain(
      "This is the first note's content"
    )
    expect(textareaEl(wrapper).value).toBe("")
    wrapper.unmount()
  })
})
