import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
import { TextContentController } from "@generated/doughnut-backend-api/sdk.gen"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import {
  mountMarkdownTextarea,
  setTextareaValue,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  textareaEl,
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

  it("clears dirty when save returns wrapped ordinary-note content", async () => {
    const noteId = 1
    const wrapped = "---\ntype: Note\n---\nAfter save"
    let wrapper: VueWrapper<ComponentPublicInstance>

    mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      async () => {
        await wrapper.setProps({ noteId, noteContent: wrapped })
        return makeMe.aNoteRealm.id(noteId).content(wrapped).please()
      }
    )

    wrapper = await mountMarkdownTextarea({
      noteId,
      noteContent: "Before",
    })

    await setTextareaValue(wrapper, "After save")
    await wrapper.find("textarea").trigger("blur")
    await flushPromises()

    expect(wrapper.find(".dirty").exists()).toBe(false)
    expect(textareaEl(wrapper).value).toBe(wrapped)

    wrapper.unmount()
  })
})
