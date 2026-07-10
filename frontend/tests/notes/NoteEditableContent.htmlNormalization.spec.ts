import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import {
  mountMarkdownTextarea,
  setTextareaValue,
  setupUpdateNoteContentMock,
} from "./noteEditableContentTestSupport"

describe("NoteEditableContent HTML content normalization", () => {
  let updateNoteContentSpy: ReturnType<typeof setupUpdateNoteContentMock>

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    updateNoteContentSpy = setupUpdateNoteContentMock()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it("should not save when value contains only <p><br></p> and last saved was also empty", async () => {
    const noteId = 1
    const wrapper = await mountMarkdownTextarea({ noteId, noteContent: "" })

    await setTextareaValue(wrapper, "<p><br></p>")
    await advanceNoteContentSaveDebounce()

    expect(updateNoteContentSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("should save when clearing content (from non-empty to <p><br></p>)", async () => {
    const noteId = 1
    const wrapper = await mountMarkdownTextarea({
      noteId,
      noteContent: "Original content",
    })

    await setTextareaValue(wrapper, "<p><br></p>")
    await advanceNoteContentSaveDebounce()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "" },
    })
    wrapper.unmount()
  })

  it.each([
    {
      name: "should not save when only addition is empty lines and <p><br></p> at the end",
      editedValue: "Original content\n\n<p><br></p>",
    },
    {
      name: "should not save when only addition is trailing br tags",
      editedValue: "Original content\n<br>\n<br>",
    },
  ])("$name", async ({ editedValue }) => {
    const noteId = 1
    const wrapper = await mountMarkdownTextarea({
      noteId,
      noteContent: "Original content",
    })

    await setTextareaValue(wrapper, editedValue)
    await advanceNoteContentSaveDebounce()

    expect(updateNoteContentSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("should save with trailing empty lines and <p><br></p> removed when change is not only at the end", async () => {
    const noteId = 1
    const wrapper = await mountMarkdownTextarea({
      noteId,
      noteContent: "Original content",
    })

    await setTextareaValue(wrapper, "Modified content\n\n<p><br></p>")
    await advanceNoteContentSaveDebounce()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "Modified content" },
    })
    wrapper.unmount()
  })
})
