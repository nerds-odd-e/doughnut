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

  it("normalizes blank HTML before deciding what content to save", async () => {
    const noteId = 1
    const wrapper = await mountMarkdownTextarea({ noteId, noteContent: "" })

    await setTextareaValue(wrapper, "<p><br></p>")
    await advanceNoteContentSaveDebounce()

    expect(updateNoteContentSpy).not.toHaveBeenCalled()

    await setTextareaValue(wrapper, "Original content")
    await advanceNoteContentSaveDebounce()
    expect(updateNoteContentSpy).toHaveBeenNthCalledWith(1, {
      path: { note: noteId },
      body: { content: "Original content" },
    })

    await setTextareaValue(wrapper, "Original content\n\n<p><br></p>")
    await advanceNoteContentSaveDebounce()
    expect(updateNoteContentSpy).toHaveBeenCalledTimes(1)

    await setTextareaValue(wrapper, "Original content\n<br>\n<br>")
    await advanceNoteContentSaveDebounce()
    expect(updateNoteContentSpy).toHaveBeenCalledTimes(1)

    await setTextareaValue(wrapper, "Modified content\n\n<p><br></p>")
    await advanceNoteContentSaveDebounce()

    expect(updateNoteContentSpy).toHaveBeenNthCalledWith(2, {
      path: { note: noteId },
      body: { content: "Modified content" },
    })

    await setTextareaValue(wrapper, "<p><br></p>")
    await advanceNoteContentSaveDebounce()
    expect(updateNoteContentSpy).toHaveBeenNthCalledWith(3, {
      path: { note: noteId },
      body: { content: "" },
    })
    wrapper.unmount()
  })
})
