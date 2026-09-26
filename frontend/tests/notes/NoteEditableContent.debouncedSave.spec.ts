import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
  MemoryTrackerController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import {
  mountNoteEditableContent,
  setTextareaValue,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  textareaEl,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableContent debounced save", () => {
  const noteId = 1
  let updateNoteContentSpy: ReturnType<typeof setupUpdateNoteContentMock>
  let confirmMock: ReturnType<typeof vi.fn<(msg: string) => Promise<boolean>>>

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    updateNoteContentSpy = setupUpdateNoteContentMock()
    confirmMock = vi.fn<(msg: string) => Promise<boolean>>()
    setupPopupsMock(vi.fn().mockResolvedValue(null), { confirm: confirmMock })
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  function expectSaved(content: string) {
    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content },
    })
  }

  it("should auto-save edited content after debounce timeout without blur", async () => {
    const wrapper = await mountNoteEditableContent({
      noteId,
      noteContent: "Original content",
    })

    await setTextareaValue(wrapper, "Edited content")
    expect(wrapper.find(".dirty").exists()).toBe(true)
    expect(updateNoteContentSpy).not.toHaveBeenCalled()

    await advanceNoteContentSaveDebounce()

    expectSaved("Edited content")
    expect(wrapper.find(".dirty").exists()).toBe(false)
    wrapper.unmount()
  })

  it("does not send obsolete edit when draft is restored before debounce fires", async () => {
    const wrapper = await mountNoteEditableContent({ noteId, noteContent: "A" })

    await setTextareaValue(wrapper, "AB")
    expect(updateNoteContentSpy).not.toHaveBeenCalled()
    await setTextareaValue(wrapper, "A")
    await advanceNoteContentSaveDebounce()

    expect(updateNoteContentSpy).not.toHaveBeenCalled()
    expect(textareaEl(wrapper).value).toBe("A")
    wrapper.unmount()
  })

  it("should save content immediately when a new wiki link appears (flush debounce)", async () => {
    const wrapper = await mountNoteEditableContent({
      noteId,
      noteContent: "Hello",
    })

    await setTextareaValue(wrapper, "Hello [[OtherNote]]")

    expectSaved("Hello [[OtherNote]]")
    wrapper.unmount()
  })

  it("does not save when navigating to a note in rich mode", async () => {
    const noteContent = String.raw`---
topic: Japanese
---

使い分けのコツ（1行ルール） \\\* 「細かいことはいいから今すぐ！」→ とにかく \\\* 「Aはいったん置いといて、まずB」→ ともかく`

    const wrapper = await mountNoteEditableContent({
      noteId: 2,
      noteContent: "Other note",
      asMarkdown: false,
    })
    await wrapper.setProps({ noteId, noteContent })
    await flushPromises()
    await advanceNoteContentSaveDebounce()

    expect(updateNoteContentSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("normalizes blank HTML before deciding what content to save", async () => {
    const wrapper = await mountNoteEditableContent({ noteId, noteContent: "" })
    const editAndWait = async (value: string) => {
      await setTextareaValue(wrapper, value)
      await advanceNoteContentSaveDebounce()
    }

    await editAndWait("<p><br></p>")
    expect(updateNoteContentSpy).not.toHaveBeenCalled()

    await editAndWait("Original content")
    expectSaved("Original content")

    await editAndWait("Original content\n\n<p><br></p>")
    await editAndWait("Original content\n<br>\n<br>")
    expect(updateNoteContentSpy).toHaveBeenCalledTimes(1)

    await editAndWait("Modified content\n\n<p><br></p>")
    expect(updateNoteContentSpy).toHaveBeenNthCalledWith(2, {
      path: { note: noteId },
      body: { content: "Modified content" },
    })

    await editAndWait("<p><br></p>")
    expect(updateNoteContentSpy).toHaveBeenNthCalledWith(3, {
      path: { note: noteId },
      body: { content: "" },
    })
    wrapper.unmount()
  })

  describe("guarding a property's memory tracker", () => {
    const tracker = makeMe.aMemoryTracker
      .id(99)
      .withPropertyKey("topic")
      .please()
    let deleteSpy: ReturnType<typeof mockSdkService>
    let updatePropertyKeySpy: ReturnType<typeof mockSdkService>

    beforeEach(() => {
      mockSdkService(
        NoteController,
        "getNoteInfo",
        makeMe.aNoteRecallInfo.memoryTrackers([tracker]).please()
      )
      deleteSpy = mockSdkService(MemoryTrackerController, "delete", undefined)
      updatePropertyKeySpy = mockSdkService(
        MemoryTrackerController,
        "updatePropertyKey",
        undefined
      )
      confirmMock.mockResolvedValueOnce(true)
    })

    async function editTrackedProperty(edited: string) {
      const wrapper = await mountNoteEditableContent({
        noteId,
        noteContent: "---\ntopic: training\n---\n\nWorkshop body.",
      })
      await setTextareaValue(wrapper, edited)
      await advanceNoteContentSaveDebounce()
      expectSaved(edited)
      wrapper.unmount()
    }

    it("hard-deletes the tracker and saves when the user confirms removing a tracked property", async () => {
      await editTrackedProperty("---\n---\n\nWorkshop body.")

      expect(confirmMock).toHaveBeenCalledWith(
        'Property "topic" has a memory tracker. Deleting it will also delete that tracker. Continue?'
      )
      expect(deleteSpy).toHaveBeenCalledWith({
        path: { memoryTracker: tracker.id },
      })
    })

    it("updates the tracker property key and saves when the user confirms renaming", async () => {
      await editTrackedProperty("---\nsubject: training\n---\n\nWorkshop body.")

      expect(confirmMock).toHaveBeenCalledWith(
        'Property "topic" has a memory tracker. Renaming it to "subject" will update the tracker. Continue?'
      )
      expect(updatePropertyKeySpy).toHaveBeenCalledWith({
        path: { memoryTracker: tracker.id },
        body: { propertyKey: "subject" },
      })
    })
  })
})
