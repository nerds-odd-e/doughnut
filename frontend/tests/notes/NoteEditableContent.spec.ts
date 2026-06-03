import { TextContentController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteEditableContent from "@/components/notes/core/NoteEditableContent.vue"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { vi, describe, it, expect, beforeEach } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import type { UpdateNoteContentData } from "@generated/doughnut-backend-api"
import usePopups from "@/components/commons/Popups/usePopups"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableContent", () => {
  let updateNoteContentSpy: ReturnType<typeof mockSdkService>
  // biome-ignore lint/suspicious/noExplicitAny: Mock type for testing
  let mockPopupsOptions: any

  beforeEach(() => {
    vi.resetAllMocks()
    updateNoteContentSpy = mockSdkService(
      TextContentController,
      "updateNoteContent",
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

  const markdownTextareaDefaults = {
    readonly: false,
    asMarkdown: true,
    wikiTitles: [] as string[],
  }

  async function mountMarkdownTextarea(props: {
    noteId: number
    noteContent: string
  }) {
    const wrapper = helper
      .component(NoteEditableContent)
      .withCleanStorage()
      .withRouter()
      .withProps({ ...markdownTextareaDefaults, ...props })
      .mount()
    await flushPromises()
    return wrapper
  }

  function textareaEl(wrapper: VueWrapper<ComponentPublicInstance>) {
    return wrapper.find("textarea").element as HTMLTextAreaElement
  }

  async function setTextareaValue(
    wrapper: VueWrapper<ComponentPublicInstance>,
    value: string
  ) {
    const el = textareaEl(wrapper)
    el.value = value
    el.dispatchEvent(new Event("input"))
    await flushPromises()
    return el
  }

  it("should not save previous note's content to the new note when navigating", async () => {
    const firstNoteId = 1
    const secondNoteId = 2

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableContent)
      .withCleanStorage()
      .withRouter()
      .withProps({
        noteId: firstNoteId,
        noteContent: "First note content",
        readonly: false,
        asMarkdown: true,
        wikiTitles: [],
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const contentTextarea = wrapper.find("textarea")
      .element as HTMLTextAreaElement
    contentTextarea.value = "Edited content from first note"
    contentTextarea.dispatchEvent(new Event("input"))
    await flushPromises()

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
    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableContent)
      .withCleanStorage()
      .withRouter()
      .withProps({
        noteId: 1,
        noteContent: "First note content",
        readonly: false,
        asMarkdown: true,
        wikiTitles: [],
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    await wrapper.setProps({
      noteId: 2,
      noteContent: "Second note content",
    })
    await flushPromises()

    const contentTextarea = wrapper.find("textarea")
      .element as HTMLTextAreaElement
    expect(contentTextarea.value).toBe("Second note content")
    wrapper.unmount()
  })

  it("should preserve unsaved edits if the noteContent prop doesn't actually change", async () => {
    const noteId = 1
    const noteContent = "Original content"

    const wrapper = await mountMarkdownTextarea({ noteId, noteContent })
    await setTextareaValue(wrapper, "Edited content")

    await wrapper.setProps({ noteId, noteContent, readonly: false })

    expect(textareaEl(wrapper).value).toBe("Edited content")

    await wrapper.find("textarea").trigger("blur")
    await flushPromises()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "Edited content" },
    })
    wrapper.unmount()
  })

  it("should save edited content to the correct note on blur before navigation", async () => {
    const firstNoteId = 1

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableContent)
      .withCleanStorage()
      .withRouter()
      .withProps({
        noteId: firstNoteId,
        noteContent: "First note content",
        readonly: false,
        asMarkdown: true,
        wikiTitles: [],
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const contentTextarea = wrapper.find("textarea")
      .element as HTMLTextAreaElement
    contentTextarea.value = "Edited content"
    contentTextarea.dispatchEvent(new Event("input"))
    contentTextarea.dispatchEvent(new Event("blur"))
    await flushPromises()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: firstNoteId },
      body: { content: "Edited content" },
    })
    wrapper.unmount()
  })

  it("should auto-save edited content after debounce timeout without blur", async () => {
    vi.useFakeTimers()

    const noteId = 1
    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableContent)
      .withCleanStorage()
      .withRouter()
      .withProps({
        noteId,
        noteContent: "Original content",
        readonly: false,
        asMarkdown: true,
        wikiTitles: [],
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const contentTextarea = wrapper.find("textarea")
      .element as HTMLTextAreaElement
    contentTextarea.value = "Edited content"
    contentTextarea.dispatchEvent(new Event("input"))
    await flushPromises()

    expect(wrapper.find(".dirty").exists()).toBe(true)

    vi.advanceTimersByTime(1000)
    await flushPromises()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "Edited content" },
    })
    expect(wrapper.find(".dirty").exists()).toBe(false)

    vi.useRealTimers()
    wrapper.unmount()
  })

  it("should save content immediately when a new wiki link appears (flush debounce)", async () => {
    vi.useFakeTimers()

    const noteId = 1
    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableContent)
      .withCleanStorage()
      .withRouter()
      .withProps({
        noteId,
        noteContent: "Hello",
        readonly: false,
        asMarkdown: true,
        wikiTitles: [],
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const contentTextarea = wrapper.find("textarea")
      .element as HTMLTextAreaElement
    contentTextarea.value = "Hello [[OtherNote]]"
    contentTextarea.dispatchEvent(new Event("input"))
    await flushPromises()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "Hello [[OtherNote]]" },
    })

    vi.useRealTimers()
    wrapper.unmount()
  })

  it("should not save until debounce when edit adds no new wiki link", async () => {
    vi.useFakeTimers()

    const noteId = 1
    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableContent)
      .withCleanStorage()
      .withRouter()
      .withProps({
        noteId,
        noteContent: "Hello",
        readonly: false,
        asMarkdown: true,
        wikiTitles: [],
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const contentTextarea = wrapper.find("textarea")
      .element as HTMLTextAreaElement
    contentTextarea.value = "Hello world"
    contentTextarea.dispatchEvent(new Event("input"))
    await flushPromises()

    expect(updateNoteContentSpy).not.toHaveBeenCalled()

    vi.advanceTimersByTime(1000)
    await flushPromises()

    expect(updateNoteContentSpy).toHaveBeenCalledWith({
      path: { note: noteId },
      body: { content: "Hello world" },
    })

    vi.useRealTimers()
    wrapper.unmount()
  })

  it("should preserve second edit when first save response arrives after second edit", async () => {
    const noteId = 1
    let resolveFirstSave: (() => void) | undefined
    const firstSavePromise = new Promise<void>((resolve) => {
      resolveFirstSave = resolve
    })

    updateNoteContentSpy.mockImplementation((async (
      options: UpdateNoteContentData
    ) => {
      if (options.body?.content === "First edit") {
        await firstSavePromise
      }
      return wrapSdkResponse({
        id: noteId,
        note: {
          id: noteId,
          content: options.body?.content,
          noteTopology: { id: noteId, title: "Test Note" },
        },
      })
      // biome-ignore lint/suspicious/noExplicitAny: Vitest mock typing requires any for implementation functions
    }) as any)

    const wrapper = await mountMarkdownTextarea({
      noteId,
      noteContent: "Original",
    })

    await setTextareaValue(wrapper, "First edit")
    await wrapper.find("textarea").trigger("blur")
    await flushPromises()

    await setTextareaValue(wrapper, "Second edit")
    expect(textareaEl(wrapper).value).toBe("Second edit")

    resolveFirstSave!()
    await wrapper.setProps({ noteContent: "First edit" })
    await flushPromises()

    expect(textareaEl(wrapper).value).toBe("Second edit")
    wrapper.unmount()
  })

  it("should clear content when switching from a note with content to a note without content (undefined)", async () => {
    const firstNoteId = 1
    const secondNoteId = 2

    const wrapper: VueWrapper<ComponentPublicInstance> = helper
      .component(NoteEditableContent)
      .withCleanStorage()
      .withRouter()
      .withProps({
        noteId: firstNoteId,
        noteContent: "This is the first note's content",
        readonly: false,
        asMarkdown: true,
        wikiTitles: [],
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const contentTextarea = wrapper.find("textarea")
      .element as HTMLTextAreaElement
    expect(contentTextarea.value).toBe("This is the first note's content")

    // Switch to a note with undefined content
    await wrapper.setProps({
      noteId: secondNoteId,
      noteContent: undefined,
    })
    await flushPromises()

    // The bug: content should be cleared but it's still showing
    // This test should fail because the old content is still displayed
    expect(contentTextarea.value).not.toContain(
      "This is the first note's content"
    )
    expect(contentTextarea.value).toBe("")
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
      .component(NoteEditableContent)
      .withCleanStorage()
      .withRouter()
      .withProps({
        noteId: 1,
        noteContent: "existing text",
        readonly: false,
        asMarkdown: true,
        wikiTitles: [],
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
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId: 1,
          noteContent: "",
          readonly: false,
          asMarkdown: true,
          wikiTitles: [],
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
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId: 1,
          noteContent: "[existing link](https://existing.com) ",
          readonly: false,
          asMarkdown: true,
          wikiTitles: [],
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
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId: 1,
          noteContent: "plain text",
          readonly: false,
          asMarkdown: true,
          wikiTitles: [],
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
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId: 1,
          noteContent: "plain text",
          readonly: false,
          asMarkdown: false,
          wikiTitles: [],
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
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId: 1,
          noteContent: "plain text",
          readonly: false,
          asMarkdown: false,
          wikiTitles: [],
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

  describe("relation property row in rich mode", () => {
    it("shows RelationTypeSelectCompact when noteContent include relation frontmatter", async () => {
      const markdown = `---
relation: parent-of
---

# Body`
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId: 99,
          noteContent: markdown,
          readonly: false,
          asMarkdown: false,
          wikiTitles: [],
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const rfp = wrapper.findComponent({ name: "RichFrontmatterProperties" })
      expect(rfp.exists()).toBe(true)
      const relationSelect = rfp.findComponent({
        name: "RelationTypeSelectCompact",
      })
      expect(relationSelect.exists()).toBe(true)
      wrapper.unmount()
    })

    it("omits RelationTypeSelectCompact when noteContent omit relation property", async () => {
      const markdown = `---
topic: training
---

# Body`
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId: 99,
          noteContent: markdown,
          readonly: false,
          asMarkdown: false,
          wikiTitles: [],
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const rfp = wrapper.findComponent({ name: "RichFrontmatterProperties" })
      expect(
        rfp.findComponent({ name: "RelationTypeSelectCompact" }).exists()
      ).toBe(false)
      wrapper.unmount()
    })
  })

  describe("HTML content normalization", () => {
    it("should not save when value contains only <p><br></p> and last saved was also empty", async () => {
      vi.useFakeTimers()

      const noteId = 1
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId,
          noteContent: "",
          readonly: false,
          asMarkdown: true,
          wikiTitles: [],
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const contentTextarea = wrapper.find("textarea")
        .element as HTMLTextAreaElement
      contentTextarea.value = "<p><br></p>"
      contentTextarea.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      expect(updateNoteContentSpy).not.toHaveBeenCalled()

      vi.useRealTimers()
      wrapper.unmount()
    })

    it("should save when clearing content (from non-empty to <p><br></p>)", async () => {
      vi.useFakeTimers()

      const noteId = 1
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId,
          noteContent: "Original content",
          readonly: false,
          asMarkdown: true,
          wikiTitles: [],
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const contentTextarea = wrapper.find("textarea")
        .element as HTMLTextAreaElement
      contentTextarea.value = "<p><br></p>"
      contentTextarea.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      expect(updateNoteContentSpy).toHaveBeenCalledWith({
        path: { note: noteId },
        body: { content: "" },
      })

      vi.useRealTimers()
      wrapper.unmount()
    })

    it("should not save when only addition is empty lines and <p><br></p> at the end", async () => {
      vi.useFakeTimers()

      const noteId = 1
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId,
          noteContent: "Original content",
          readonly: false,
          asMarkdown: true,
          wikiTitles: [],
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const contentTextarea = wrapper.find("textarea")
        .element as HTMLTextAreaElement
      contentTextarea.value = "Original content\n\n<p><br></p>"
      contentTextarea.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      expect(updateNoteContentSpy).not.toHaveBeenCalled()

      vi.useRealTimers()
      wrapper.unmount()
    })

    it("should save with trailing empty lines and <p><br></p> removed when change is not only at the end", async () => {
      vi.useFakeTimers()

      const noteId = 1
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId,
          noteContent: "Original content",
          readonly: false,
          asMarkdown: true,
          wikiTitles: [],
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const contentTextarea = wrapper.find("textarea")
        .element as HTMLTextAreaElement
      contentTextarea.value = "Modified content\n\n<p><br></p>"
      contentTextarea.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      expect(updateNoteContentSpy).toHaveBeenCalledWith({
        path: { note: noteId },
        body: { content: "Modified content" },
      })

      vi.useRealTimers()
      wrapper.unmount()
    })

    it("should not save when only addition is trailing br tags", async () => {
      vi.useFakeTimers()

      const noteId = 1
      const wrapper: VueWrapper<ComponentPublicInstance> = helper
        .component(NoteEditableContent)
        .withCleanStorage()
        .withRouter()
        .withProps({
          noteId,
          noteContent: "Original content",
          readonly: false,
          asMarkdown: true,
          wikiTitles: [],
        })
        .mount({ attachTo: document.body })

      await flushPromises()

      const contentTextarea = wrapper.find("textarea")
        .element as HTMLTextAreaElement
      contentTextarea.value = "Original content\n<br>\n<br>"
      contentTextarea.dispatchEvent(new Event("input"))
      await flushPromises()

      vi.advanceTimersByTime(1000)
      await flushPromises()

      expect(updateNoteContentSpy).not.toHaveBeenCalled()

      vi.useRealTimers()
      wrapper.unmount()
    })
  })
})
