import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import {
  mountMarkdownTextarea,
  setTextareaValue,
  setupPopupsMock,
  setupUpdateNoteContentMock,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableContent debounced save", () => {
  let updateNoteContentSpy: ReturnType<typeof setupUpdateNoteContentMock>

  beforeEach(() => {
    vi.resetAllMocks()
    updateNoteContentSpy = setupUpdateNoteContentMock()
    setupPopupsMock(vi.fn().mockResolvedValue(null))
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.useRealTimers()
  })

  it("should auto-save edited content after debounce timeout without blur", async () => {
    vi.useFakeTimers()
    const noteId = 1
    const wrapper = await mountMarkdownTextarea({
      noteId,
      noteContent: "Original content",
    })

    await setTextareaValue(wrapper, "Edited content")
    expect(wrapper.find(".dirty").exists()).toBe(true)

    await advanceNoteContentSaveDebounce()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "Edited content" },
    })
    expect(wrapper.find(".dirty").exists()).toBe(false)

    wrapper.unmount()
  })

  it("should save content immediately when a new wiki link appears (flush debounce)", async () => {
    vi.useFakeTimers()
    const noteId = 1
    const wrapper = await mountMarkdownTextarea({
      noteId,
      noteContent: "Hello",
    })

    await setTextareaValue(wrapper, "Hello [[OtherNote]]")

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "Hello [[OtherNote]]" },
    })

    wrapper.unmount()
  })

  it("should not save until debounce when edit adds no new wiki link", async () => {
    vi.useFakeTimers()
    const noteId = 1
    const wrapper = await mountMarkdownTextarea({
      noteId,
      noteContent: "Hello",
    })

    await setTextareaValue(wrapper, "Hello world")
    expect(updateNoteContentSpy).not.toHaveBeenCalled()

    await advanceNoteContentSaveDebounce()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "Hello world" },
    })

    wrapper.unmount()
  })
})
