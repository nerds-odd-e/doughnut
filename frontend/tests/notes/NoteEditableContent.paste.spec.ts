import { flushPromises } from "@vue/test-utils"
import { vi, describe, it, expect, beforeEach, afterEach } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { notePropertyHref } from "@/routes/noteShowLocation"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import {
  createClipboardEvent,
  emitRichEditorPasteComplete,
  mountNoteEditableContent,
  setupPopupsMock,
  setupUpdateNoteContentMock,
  textareaEl,
} from "./noteEditableContentTestSupport"

vi.mock("@/components/commons/Popups/usePopups")

describe("NoteEditableContent paste", () => {
  // biome-ignore lint/suspicious/noExplicitAny: Mock type for testing
  let mockPopupsOptions: any

  beforeEach(() => {
    vi.resetAllMocks()
    setupUpdateNoteContentMock()
    mockPopupsOptions = vi.fn().mockResolvedValue(null)
    setupPopupsMock(mockPopupsOptions)
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("converts HTML to markdown when pasting HTML content without links", async () => {
    const wrapper = mountNoteEditableContent(
      { noteId: 1, noteContent: "existing text" },
      { attachTo: document.body }
    )
    await flushPromises()

    const textarea = textareaEl(wrapper)
    textarea.setSelectionRange(8, 8)

    await textarea.dispatchEvent(
      createClipboardEvent("<p><strong>Bold text</strong></p>")
    )
    await flushPromises()

    expect(textarea.value).toContain("Bold text")
    expect(textarea.value).toContain("existing")
    expect(mockPopupsOptions).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  describe("textarea", () => {
    it("shows options popup when pasted content contains links and removes them when chosen", async () => {
      mockPopupsOptions.mockResolvedValue("links")

      const wrapper = mountNoteEditableContent(
        {
          noteId: 1,
          noteContent: "[existing link](https://existing.com) ",
        },
        { attachTo: document.body }
      )
      await flushPromises()

      const textarea = textareaEl(wrapper)
      await textarea.dispatchEvent(
        createClipboardEvent(
          '<p><a href="https://example.com">new link</a></p>'
        )
      )
      await flushPromises()

      expect(mockPopupsOptions).toHaveBeenCalledWith(
        "The content contains 2 links.",
        expect.arrayContaining([{ label: "Remove 2 links", value: "links" }])
      )
      expect(textarea.value).toContain("existing link")
      expect(textarea.value).toContain("new link")
      expect(textarea.value).not.toContain("https://existing.com")
      expect(textarea.value).not.toContain("https://example.com")
      wrapper.unmount()
    })

    it("converts a pasted noteProperty URL to a portable property wiki", async () => {
      mockSdkService(NoteController, "authoredPortablePath", {
        portablePath: "Moon#prop:topic",
      })
      const source = makeMe.aNoteRealm
        .id(1)
        .title("Carrier")
        .inNotebook(10, "Sky")
        .please()
      const target = makeMe.aNoteRealm
        .id(99)
        .title("Moon")
        .inNotebook(10, "Sky")
        .please()
      const wrapper = mountNoteEditableContent(
        { noteId: 1, noteContent: "See " },
        { attachTo: document.body }
      )
      await flushPromises()
      useStorageAccessor().value.refreshNoteRealm(source)
      useStorageAccessor().value.refreshNoteRealm(target)

      const textarea = textareaEl(wrapper)
      textarea.setSelectionRange(4, 4)
      const href = notePropertyHref(99, "topic")
      await textarea.dispatchEvent(
        createClipboardEvent(`<p><a href="${href}">shown</a></p>`)
      )
      await flushPromises()

      expect(textarea.value).toContain("[[Moon#prop:topic|shown]]")
      expect(textarea.value).not.toContain(href)
      expect(mockPopupsOptions).not.toHaveBeenCalled()
      wrapper.unmount()
    })

    it("qualifies a pasted noteProperty URL when the target notebook differs", async () => {
      const source = makeMe.aNoteRealm
        .id(1)
        .title("Carrier")
        .inNotebook(10, "Here")
        .please()
      const target = makeMe.aNoteRealm
        .id(99)
        .title("Moon")
        .inNotebook(20, "Sky")
        .please()
      const wrapper = mountNoteEditableContent(
        { noteId: 1, noteContent: "See " },
        { attachTo: document.body }
      )
      await flushPromises()
      useStorageAccessor().value.refreshNoteRealm(source)
      useStorageAccessor().value.refreshNoteRealm(target)

      const textarea = textareaEl(wrapper)
      textarea.setSelectionRange(4, 4)
      await textarea.dispatchEvent(
        createClipboardEvent(
          `<p><a href="${notePropertyHref(99, "topic")}">shown</a></p>`
        )
      )
      await flushPromises()

      expect(textarea.value).toContain("[[Sky:Moon#prop:topic|shown]]")
      wrapper.unmount()
    })

    it("leaves a normal link and offers the paste choice when note identity cannot be resolved", async () => {
      mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
      vi.mocked(NoteController.showNote).mockResolvedValue(
        wrapSdkError("missing")
      )

      const source = makeMe.aNoteRealm
        .id(1)
        .title("Carrier")
        .inNotebook(10, "Sky")
        .please()
      const wrapper = mountNoteEditableContent(
        { noteId: 1, noteContent: "See " },
        { attachTo: document.body }
      )
      await flushPromises()
      useStorageAccessor().value.refreshNoteRealm(source)

      const textarea = textareaEl(wrapper)
      textarea.setSelectionRange(4, 4)
      const href = notePropertyHref(99, "topic")
      await textarea.dispatchEvent(
        createClipboardEvent(`<p><a href="${href}">shown</a></p>`)
      )
      await flushPromises()

      expect(textarea.value).toContain(`[shown](${href})`)
      expect(textarea.value).not.toContain("[[shown]]")
      expect(mockPopupsOptions).toHaveBeenCalledWith(
        "The content contains 1 links.",
        expect.arrayContaining([{ label: "Remove 1 links", value: "links" }])
      )
      wrapper.unmount()
    })
  })

  describe("quill editor", () => {
    function mountRichEditor(noteContent: string) {
      return mountNoteEditableContent(
        { noteId: 1, noteContent, asMarkdown: false },
        { attachTo: document.body }
      )
    }

    it("shows options popup based on content after paste, and skips when no links", async () => {
      const wrapper = mountRichEditor("plain text")
      await flushPromises()

      emitRichEditorPasteComplete(wrapper, "plain text with more plain text")
      await flushPromises()
      expect(mockPopupsOptions).not.toHaveBeenCalled()

      emitRichEditorPasteComplete(
        wrapper,
        "plain text [new link](https://example.com)"
      )
      await flushPromises()

      expect(mockPopupsOptions).toHaveBeenCalledWith(
        "The content contains 1 links.",
        expect.arrayContaining([{ label: "Remove 1 links", value: "links" }])
      )
      wrapper.unmount()
    })
  })
})
