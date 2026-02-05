import NoteEditableDetails from "@/components/notes/core/NoteEditableDetails.vue"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { vi, describe, it, expect, beforeEach } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import type { UpdateNoteDetailsData } from "@generated/backend"
import usePopups from "@/components/commons/Popups/usePopups"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableDetails", () => {
  let updateNoteDetailsSpy: ReturnType<
    typeof mockSdkService<"updateNoteDetails">
  >
  // biome-ignore lint/suspicious/noExplicitAny: Mock type for testing
  let mockPopupsOptions: any

  beforeEach(() => {
    vi.resetAllMocks()
    updateNoteDetailsSpy = mockSdkService(
      "updateNoteDetails",
      makeMe.aNoteRealm.please()
    )
    mockPopupsOptions = vi.fn().mockResolvedValue(null)
    vi.mocked(usePopups).mockReturnValue({
      popups: {
        options: mockPopupsOptions,
        alert: vi.fn(),
        confirm: vi.fn(),
        done: vi.fn(),
        register: vi.fn(),
        peek: vi.fn(),
      },
    })
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("should not save previous note's details to the new note when navigating", async () => {
    const firstNoteId = 1
    const secondNoteId = 2

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId: firstNoteId,
        noteDetails: "First note details",
        readonly: false,
        asMarkdown: true,
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details from first note"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    await wrapper.setProps({
      noteId: secondNoteId,
      noteDetails: "Second note details",
    })
    await flushPromises()

    expect(detailsEl.value).toBe("Second note details")

    detailsEl.value = "New edits on second note"
    detailsEl.dispatchEvent(new Event("input"))
    detailsEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    const calls = updateNoteDetailsSpy.mock.calls as Array<
      [UpdateNoteDetailsData]
    >
    expect(
      calls.some(
        (call) =>
          call[0].path?.note === secondNoteId &&
          call[0].body?.details === "Edited details from first note"
      )
    ).toBe(false)
    expect(calls.some((call) => call[0].path?.note === firstNoteId)).toBe(false)
    if (calls.length > 0) {
      expect(
        calls.some(
          (call) =>
            call[0].path?.note === secondNoteId &&
            call[0].body?.details === "New edits on second note"
        )
      ).toBe(true)
    }
    wrapper.unmount()
  })

  it("should update displayed details when navigating to a different note with no unsaved changes", async () => {
    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId: 1,
        noteDetails: "First note details",
        readonly: false,
        asMarkdown: true,
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    await wrapper.setProps({
      noteId: 2,
      noteDetails: "Second note details",
    })
    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    expect(detailsEl.value).toBe("Second note details")
    wrapper.unmount()
  })

  it("should preserve unsaved edits if the noteDetails prop doesn't actually change", async () => {
    const noteId = 1
    const noteDetails = "Original details"

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId,
        noteDetails,
        readonly: false,
        asMarkdown: true,
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    await wrapper.setProps({
      noteId,
      noteDetails,
      readonly: false,
    })
    await flushPromises()

    expect(detailsEl.value).toBe("Edited details")

    detailsEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { details: "Edited details" },
    })
    wrapper.unmount()
  })

  it("should save edited details to the correct note on blur before navigation", async () => {
    const firstNoteId = 1

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId: firstNoteId,
        noteDetails: "First note details",
        readonly: false,
        asMarkdown: true,
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details"
    detailsEl.dispatchEvent(new Event("input"))
    detailsEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
      path: { note: firstNoteId },
      body: { details: "Edited details" },
    })
    wrapper.unmount()
  })

  it("should auto-save edited details after debounce timeout without blur", async () => {
    vi.useFakeTimers()

    const noteId = 1
    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId,
        noteDetails: "Original details",
        readonly: false,
        asMarkdown: true,
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    detailsEl.value = "Edited details"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()

    expect(wrapper.find(".dirty").exists()).toBe(true)

    vi.advanceTimersByTime(1000)
    await flushPromises()

    expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { details: "Edited details" },
    })
    expect(wrapper.find(".dirty").exists()).toBe(false)

    vi.useRealTimers()
    wrapper.unmount()
  })

  it("should preserve second edit when first save response arrives after second edit", async () => {
    const noteId = 1
    let resolveFirstSave: (() => void) | undefined
    const firstSavePromise = new Promise<void>((resolve) => {
      resolveFirstSave = resolve
    })

    updateNoteDetailsSpy.mockImplementation((async (
      options: UpdateNoteDetailsData
    ) => {
      if (options.body?.details === "First edit") {
        await firstSavePromise
      }
      return wrapSdkResponse({
        id: noteId,
        note: {
          id: noteId,
          details: options.body?.details,
          noteTopology: { id: noteId, title: "Test Note" },
        },
      })
      // biome-ignore lint/suspicious/noExplicitAny: Vitest mock typing requires any for implementation functions
    }) as any)

    const wrapper = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId,
        noteDetails: "Original",
        readonly: false,
        asMarkdown: true,
      })
      .mount({ attachTo: document.body })

    await flushPromises()
    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement

    detailsEl.value = "First edit"
    detailsEl.dispatchEvent(new Event("input"))
    detailsEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    detailsEl.value = "Second edit"
    detailsEl.dispatchEvent(new Event("input"))
    await flushPromises()
    expect(detailsEl.value).toBe("Second edit")

    resolveFirstSave!()
    await wrapper.setProps({ noteDetails: "First edit" })
    await flushPromises()

    expect(detailsEl.value).toBe("Second edit")
    wrapper.unmount()
  })

  it("should clear details when switching from a note with details to a note without details (undefined)", async () => {
    const firstNoteId = 1
    const secondNoteId = 2

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId: firstNoteId,
        noteDetails: "This is the first note's details",
        readonly: false,
        asMarkdown: true,
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
    expect(detailsEl.value).toBe("This is the first note's details")

    // Switch to a note with undefined details
    await wrapper.setProps({
      noteId: secondNoteId,
      noteDetails: undefined,
    })
    await flushPromises()

    // The bug: details should be cleared but they're still showing
    // This test should fail because the old details are still displayed
    expect(detailsEl.value).not.toContain("This is the first note's details")
    expect(detailsEl.value).toBe("")
    wrapper.unmount()
  })

  function createClipboardEvent(html: string): ClipboardEvent {
    const event = new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData: new DataTransfer(), // Browser Mode: Use DataTransfer
    })
    event.clipboardData?.setData("text/html", html) // Browser Mode: Set data
    return event
  }

  it("converts HTML to markdown when pasting HTML content", async () => {
    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableDetails)
      .withCleanStorage()
      .withProps({
        noteId: 1,
        noteDetails: "existing text",
        readonly: false,
        asMarkdown: true,
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const textarea = wrapper.find("textarea").element as HTMLTextAreaElement
    textarea.setSelectionRange(8, 8) // Position cursor after "existing"

    const pasteEvent = createClipboardEvent("<p><strong>Bold text</strong></p>")

    await textarea.dispatchEvent(pasteEvent)
    await flushPromises()

    // The HTML should be converted to markdown and the value should be updated
    // The update should have been called through TextContentWrapper
    expect(textarea.value).toContain("Bold text")
    expect(textarea.value).toContain("existing")
    wrapper.unmount()
  })

  describe("paste with links and images in textarea", () => {
    it("shows options popup when content contains links after paste", async () => {
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableDetails)
        .withCleanStorage()
        .withProps({
          noteId: 1,
          noteDetails: "",
          readonly: false,
          asMarkdown: true,
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const textarea = wrapper.find("textarea").element as HTMLTextAreaElement
      const pasteEvent = createClipboardEvent(
        '<p>Check <a href="https://example.com">this link</a></p>'
      )

      await textarea.dispatchEvent(pasteEvent)
      await flushPromises()

      expect(mockPopupsOptions).toHaveBeenCalledWith(
        "The content contains 1 links.",
        expect.arrayContaining([{ label: "Remove 1 links", value: "links" }])
      )
      wrapper.unmount()
    })

    it("removes all links from entire content when user selects remove links", async () => {
      mockPopupsOptions.mockResolvedValue("links")

      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableDetails)
        .withCleanStorage()
        .withProps({
          noteId: 1,
          noteDetails: "[existing link](https://existing.com) ",
          readonly: false,
          asMarkdown: true,
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const textarea = wrapper.find("textarea").element as HTMLTextAreaElement
      const pasteEvent = createClipboardEvent(
        '<p><a href="https://example.com">new link</a></p>'
      )

      await textarea.dispatchEvent(pasteEvent)
      await flushPromises()

      // Both existing and new links should be removed
      expect(textarea.value).toContain("existing link")
      expect(textarea.value).toContain("new link")
      expect(textarea.value).not.toContain("https://existing.com")
      expect(textarea.value).not.toContain("https://example.com")
      wrapper.unmount()
    })

    it("does not show popup when entire content has no links or images", async () => {
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableDetails)
        .withCleanStorage()
        .withProps({
          noteId: 1,
          noteDetails: "plain text",
          readonly: false,
          asMarkdown: true,
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const textarea = wrapper.find("textarea").element as HTMLTextAreaElement
      const pasteEvent = createClipboardEvent(
        "<p><strong>Bold text</strong></p>"
      )

      await textarea.dispatchEvent(pasteEvent)
      await flushPromises()

      expect(mockPopupsOptions).not.toHaveBeenCalled()
      wrapper.unmount()
    })
  })

  describe("paste with links and images in quill editor", () => {
    it("shows options popup based on content AFTER paste, not before", async () => {
      // Start with plain text (no links)
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableDetails)
        .withCleanStorage()
        .withProps({
          noteId: 1,
          noteDetails: "plain text",
          readonly: false,
          asMarkdown: false,
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const richEditor = wrapper.findComponent({ name: "RichMarkdownEditor" })
      const newContent = "plain text [new link](https://example.com)"

      // Simulate quill editor pasting content with a link:
      // RichMarkdownEditor emits update:modelValue then pasteComplete with the new content
      richEditor.vm.$emit("update:modelValue", newContent)
      richEditor.vm.$emit("pasteComplete", newContent)
      await flushPromises()

      // Should detect the link in the NEW content (after paste)
      expect(mockPopupsOptions).toHaveBeenCalledWith(
        "The content contains 1 links.",
        expect.arrayContaining([{ label: "Remove 1 links", value: "links" }])
      )
      wrapper.unmount()
    })

    it("does not show popup when quill content has no links or images", async () => {
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableDetails)
        .withCleanStorage()
        .withProps({
          noteId: 1,
          noteDetails: "plain text",
          readonly: false,
          asMarkdown: false,
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const richEditor = wrapper.findComponent({ name: "RichMarkdownEditor" })
      const newContent = "plain text with more plain text"
      richEditor.vm.$emit("update:modelValue", newContent)
      richEditor.vm.$emit("pasteComplete", newContent)
      await flushPromises()

      expect(mockPopupsOptions).not.toHaveBeenCalled()
      wrapper.unmount()
    })
  })

  describe("HTML content normalization", () => {
    it("should not save when value contains only <p><br></p> and last saved was also empty", async () => {
      vi.useFakeTimers()

      const noteId = 1
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableDetails)
        .withCleanStorage()
        .withProps({
          noteId,
          noteDetails: "",
          readonly: false,
          asMarkdown: true,
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
      detailsEl.value = "<p><br></p>"
      detailsEl.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()

      vi.useRealTimers()
      wrapper.unmount()
    })

    it("should save when clearing content (from non-empty to <p><br></p>)", async () => {
      vi.useFakeTimers()

      const noteId = 1
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableDetails)
        .withCleanStorage()
        .withProps({
          noteId,
          noteDetails: "Original details",
          readonly: false,
          asMarkdown: true,
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
      detailsEl.value = "<p><br></p>"
      detailsEl.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: noteId },
        body: { details: "" },
      })

      vi.useRealTimers()
      wrapper.unmount()
    })

    it("should not save when only addition is empty lines and <p><br></p> at the end", async () => {
      vi.useFakeTimers()

      const noteId = 1
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableDetails)
        .withCleanStorage()
        .withProps({
          noteId,
          noteDetails: "Original details",
          readonly: false,
          asMarkdown: true,
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
      detailsEl.value = "Original details\n\n<p><br></p>"
      detailsEl.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()

      vi.useRealTimers()
      wrapper.unmount()
    })

    it("should save with trailing empty lines and <p><br></p> removed when change is not only at the end", async () => {
      vi.useFakeTimers()

      const noteId = 1
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableDetails)
        .withCleanStorage()
        .withProps({
          noteId,
          noteDetails: "Original details",
          readonly: false,
          asMarkdown: true,
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement
      detailsEl.value = "Modified details\n\n<p><br></p>"
      detailsEl.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: noteId },
        body: { details: "Modified details" },
      })

      vi.useRealTimers()
      wrapper.unmount()
    })

    it("should remove trailing consecutive empty lines, <br>, and <p><br></p>, and only save when there's content change", async () => {
      vi.useFakeTimers()

      const noteId = 1
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableDetails)
        .withCleanStorage()
        .withProps({
          noteId,
          noteDetails: "Original details",
          readonly: false,
          asMarkdown: true,
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const detailsEl = wrapper.find("textarea").element as HTMLTextAreaElement

      // Test with trailing <br> tags
      detailsEl.value = "Original details\n<br>\n<br>"
      detailsEl.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      // Should not save because normalized value is the same
      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()

      // Test with trailing empty lines and <p><br></p>
      detailsEl.value = "Original details\n\n\n<p><br></p>"
      detailsEl.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      // Should not save because normalized value is the same
      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()

      // Test with actual content change
      detailsEl.value = "Modified content\n\n<br>\n<p><br></p>"
      detailsEl.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      // Should save with normalized value (trailing content removed)
      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: noteId },
        body: { details: "Modified content" },
      })

      vi.useRealTimers()
      wrapper.unmount()
    })
  })
})
