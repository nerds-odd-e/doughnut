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

  it("normalizes trailing blank HTML before deciding whether to save", async () => {
    const noteId = 1
    const wrapper = await mountMarkdownTextarea({
      noteId,
      noteContent: "Original content",
    })

    await setTextareaValue(wrapper, "Original content\n\n<p><br></p>")
    await advanceNoteContentSaveDebounce()
    expect(updateNoteContentSpy).not.toHaveBeenCalled()

    await setTextareaValue(wrapper, "Original content\n<br>\n<br>")
    await advanceNoteContentSaveDebounce()
    expect(updateNoteContentSpy).not.toHaveBeenCalled()

    await setTextareaValue(wrapper, "Modified content\n\n<p><br></p>")
    await advanceNoteContentSaveDebounce()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "Modified content" },
    })
    wrapper.unmount()
  })
})
