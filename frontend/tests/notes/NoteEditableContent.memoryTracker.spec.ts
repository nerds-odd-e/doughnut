import usePopups from "@/components/commons/Popups/usePopups"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import {
  mountMarkdownTextarea,
  mockNoteInfoWithPropertyTracker,
  setTextareaValue,
  setupMemoryTrackerSdkMocks,
  setupUpdateNoteContentMock,
  trackedPropertyMarkdown,
  trackedPropertyNoteId,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableContent property memory tracker guard on markdown", () => {
  let updateNoteContentSpy: ReturnType<typeof setupUpdateNoteContentMock>
  let getNoteInfoSpy: ReturnType<
    typeof setupMemoryTrackerSdkMocks
  >["getNoteInfoSpy"]
  let softDeleteSpy: ReturnType<
    typeof setupMemoryTrackerSdkMocks
  >["softDeleteSpy"]
  let updatePropertyKeySpy: ReturnType<
    typeof setupMemoryTrackerSdkMocks
  >["updatePropertyKeySpy"]
  let confirmMock: ReturnType<typeof vi.fn<(msg: string) => Promise<boolean>>>
  // biome-ignore lint/suspicious/noExplicitAny: Mock type for testing
  let mockPopupsOptions: any

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    updateNoteContentSpy = setupUpdateNoteContentMock()
    ;({ getNoteInfoSpy, softDeleteSpy, updatePropertyKeySpy } =
      setupMemoryTrackerSdkMocks())
    mockPopupsOptions = vi.fn().mockResolvedValue(null)
    confirmMock = vi.fn<(msg: string) => Promise<boolean>>()
    vi.mocked(usePopups).mockReturnValue({
      popups: {
        options: mockPopupsOptions,
        alert: vi.fn(),
        confirm: confirmMock,
        done: vi.fn(),
        register: vi.fn(),
        peek: vi.fn(),
      },
    })
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  it("soft-deletes the tracker and saves when the user confirms removing a tracked property", async () => {
    const tracker = mockNoteInfoWithPropertyTracker(getNoteInfoSpy, "topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(true))

    const withoutTopic = `---
---

Workshop body.`
    const wrapper = await mountMarkdownTextarea({
      noteId: trackedPropertyNoteId,
      noteContent: trackedPropertyMarkdown,
    })

    await setTextareaValue(wrapper, withoutTopic)
    await advanceNoteContentSaveDebounce()

    expect(confirmMock).toHaveBeenCalledWith(
      'Property "topic" has a memory tracker. Deleting it will also delete that tracker. Continue?'
    )
    expect(softDeleteSpy).toHaveBeenCalledWith({
      path: { memoryTracker: tracker.id },
    })
    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: trackedPropertyNoteId },
      body: { content: withoutTopic },
    })
    wrapper.unmount()
  })

  it("updates the tracker property key and saves when the user confirms renaming", async () => {
    const tracker = mockNoteInfoWithPropertyTracker(getNoteInfoSpy, "topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(true))

    const renamedMarkdown = `---
subject: training
---

Workshop body.`
    const wrapper = await mountMarkdownTextarea({
      noteId: trackedPropertyNoteId,
      noteContent: trackedPropertyMarkdown,
    })

    await setTextareaValue(wrapper, renamedMarkdown)
    await advanceNoteContentSaveDebounce()

    expect(confirmMock).toHaveBeenCalledWith(
      'Property "topic" has a memory tracker. Renaming it to "subject" will update the tracker. Continue?'
    )
    expect(updatePropertyKeySpy).toHaveBeenCalledWith({
      path: { memoryTracker: tracker.id },
      body: { propertyKey: "subject" },
    })
    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: trackedPropertyNoteId },
      body: { content: renamedMarkdown },
    })
    wrapper.unmount()
  })

  it("does not save when the user cancels", async () => {
    mockNoteInfoWithPropertyTracker(getNoteInfoSpy, "topic", 99)
    confirmMock.mockImplementationOnce(() => Promise.resolve(false))

    const wrapper = await mountMarkdownTextarea({
      noteId: trackedPropertyNoteId,
      noteContent: trackedPropertyMarkdown,
    })

    await setTextareaValue(
      wrapper,
      `---
---

Workshop body.`
    )
    await advanceNoteContentSaveDebounce()

    expect(confirmMock).toHaveBeenCalledOnce()
    expect(softDeleteSpy).not.toHaveBeenCalled()
    expect(updateNoteContentSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})
