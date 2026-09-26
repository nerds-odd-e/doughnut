import type { UpdateNoteContentData } from "@generated/donut-backend-api"
import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import {
  blurTextarea,
  mountNoteEditableContent,
  setTextareaValue,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  textareaEl,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableContent", () => {
  let updateNoteContentSpy: ReturnType<typeof setupUpdateNoteContentMock>

  beforeEach(() => {
    vi.resetAllMocks()
    updateNoteContentSpy = setupUpdateNoteContentMock()
    setupPopupsMock(vi.fn().mockResolvedValue(null))
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  describe("switching notes", () => {
    it("should not save previous note's content to the new note when navigating", async () => {
      const firstNoteId = 1
      const secondNoteId = 2

      const wrapper = await mountNoteEditableContent(
        { noteId: firstNoteId, noteContent: "First note content" },
        { attachTo: document.body }
      )

      await setTextareaValue(wrapper, "Edited content from first note")

      await wrapper.setProps({
        noteId: secondNoteId,
        noteContent: "Second note content",
      })
      await flushPromises()

      expect(textareaEl(wrapper).value).toBe("Second note content")

      await setTextareaValue(wrapper, "New edits on second note")
      await blurTextarea(wrapper)

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
      expect(calls.some((call) => call[0].path?.note === firstNoteId)).toBe(
        false
      )
      expect(updateNoteContentSpy).toHaveBeenCalledWith({
        path: { note: secondNoteId },
        body: { content: "New edits on second note" },
      })
      wrapper.unmount()
    })

    it("updates displayed content on navigate, including clearing when content is undefined", async () => {
      const wrapper = await mountNoteEditableContent(
        { noteId: 1, noteContent: "This is the first note's content" },
        { attachTo: document.body }
      )
      expect(textareaEl(wrapper).value).toBe("This is the first note's content")

      await wrapper.setProps({ noteId: 2, noteContent: "Second note content" })
      await flushPromises()
      expect(textareaEl(wrapper).value).toBe("Second note content")

      await wrapper.setProps({ noteId: 3, noteContent: undefined })
      await flushPromises()

      expect(textareaEl(wrapper).value).not.toContain(
        "This is the first note's content"
      )
      expect(textareaEl(wrapper).value).toBe("")
      wrapper.unmount()
    })

    it("should preserve unsaved edits if the noteContent prop doesn't actually change", async () => {
      const noteId = 1
      const noteContent = "Original content"

      const wrapper = await mountNoteEditableContent({ noteId, noteContent })
      await setTextareaValue(wrapper, "Edited content")

      await wrapper.setProps({ noteId, noteContent, readonly: false })

      expect(textareaEl(wrapper).value).toBe("Edited content")
      wrapper.unmount()
    })
  })

  it("shows the rich relation type picker only when noteContent includes relation frontmatter", async () => {
    const relationTypeSelect = () =>
      document.querySelector(
        '[data-testid="rich-note-property-row"][data-property-key="relation"] button[aria-label="Relation Type"]'
      )
    const wrapper = await mountNoteEditableContent(
      {
        noteId: 99,
        noteContent: "---\nrelation: parent-of\n---\n\n# Body",
        asMarkdown: false,
      },
      { attachTo: document.body }
    )
    expect(relationTypeSelect()).not.toBeNull()

    await wrapper.setProps({
      noteContent: "---\ntopic: training\n---\n\n# Body",
    })
    await flushPromises()
    expect(relationTypeSelect()).toBeNull()

    wrapper.unmount()
  })
})
