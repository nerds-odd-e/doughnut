import type { Note } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import type { ComponentPublicInstance } from "vue"
import NoteEditableTitle from "@/components/notes/core/NoteEditableTitle.vue"
import { RESERVED_README_TITLE_MESSAGE } from "@/utils/reservedReadmeTitles"
import helper from "@tests/helpers"
import {
  editTitle,
  editTitleThenBlur,
  mockedUpdateTitleCall,
  mockUpdateNoteTitle,
  mountNoteTextContent,
  titleEditorEl,
} from "./noteTextContentTestSupport"

describe("NoteTextContent title edit", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  const mountTextContentWith = (note: Note, readonly = false) => {
    wrapper = mountNoteTextContent(note, { readonly })
    return wrapper
  }

  const mountEditableTitle = (note: Note) => {
    wrapper = helper
      .component(NoteEditableTitle)
      .withCleanStorage()
      .withProps({
        noteTopology: note.noteTopology,
        noteId: note.id,
        readonly: false,
      })
      .mount({ attachTo: document.body })
    return wrapper
  }

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    mockUpdateNoteTitle()
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
    vi.useRealTimers()
  })

  it("prompts for empty content only when the note is editable", () => {
    const emptyNote = makeMe.aNote.title("Dummy Title").content("").please()

    mountTextContentWith(emptyNote)
    expect(titleEditorEl(wrapper).getAttribute("contenteditable")).toBe("true")
    expect(
      wrapper.get("[data-placeholder]").attributes("data-placeholder")
    ).toBe("Enter note content here...")
    wrapper.unmount()

    mountTextContentWith(emptyNote, true)
    expect(titleEditorEl(wrapper).getAttribute("contenteditable")).toBe("false")
    expect(wrapper.find("[data-placeholder]").exists()).toBe(false)
  })

  it("saves title change on unmount", async () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    mountEditableTitle(note)
    await editTitle(wrapper, "updated")
    wrapper.unmount()
    await flushPromises()
    expect(mockedUpdateTitleCall).toBeCalledWith({
      path: { note: note.id },
      body: { newTitle: "updated" },
    })
  })

  it("saves title change on blur", async () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    mountEditableTitle(note)
    await editTitleThenBlur(wrapper)
    await flushPromises()
    expect(mockedUpdateTitleCall).toBeCalledWith({
      path: { note: note.id },
      body: { newTitle: "updated" },
    })
  })

  it("keeps unsaved title edits when props change", async () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    mountEditableTitle(note)
    await editTitleThenBlur(wrapper, "ABC")
    await editTitle(wrapper, "ABCDEF")

    await wrapper.setProps({
      noteTopology: {
        ...note.noteTopology,
        title: "ABC",
      },
    })
    await flushPromises()

    expect(titleEditorEl(wrapper).innerText).toBe("ABCDEF")
  })

  it("updates title from props when there are no unsaved edits", async () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    mountEditableTitle(note)
    await wrapper.setProps({
      noteTopology: {
        ...note.noteTopology,
        title: "different value",
      },
    })
    expect(titleEditorEl(wrapper).innerText).toBe("different value")
  })

  it("displays title errors and clears them after a successful edit", async () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    mountEditableTitle(note)
    mockedUpdateTitleCall.mockRejectedValueOnce(
      makeMe.anApiError
        .ofBindingError({
          title: "size must be between 1 and 100",
        })
        .please()
    )
    await editTitleThenBlur(wrapper, "invalid")
    await flushPromises()

    expect(wrapper.find(".path-name-editor .text-error").text()).toBe(
      "size must be between 1 and 100"
    )

    await editTitleThenBlur(wrapper, "corrected")
    await flushPromises()
    expect(wrapper.findAll(".path-name-editor .text-error")).toHaveLength(0)
    expect(mockedUpdateTitleCall).toBeCalledTimes(2)

    mockedUpdateTitleCall.mockRejectedValueOnce({
      title: RESERVED_README_TITLE_MESSAGE,
      status: 400,
    })
    await editTitleThenBlur(wrapper, "readme")
    await flushPromises()
    expect(wrapper.find(".path-name-editor .text-error").text()).toContain(
      "reserved"
    )
  })

  it("does not save title when unchanged on unmount", async () => {
    mountEditableTitle(makeMe.aNote.title("Dummy Title").please())
    await flushPromises()
    wrapper.unmount()
    expect(mockedUpdateTitleCall).toBeCalledTimes(0)
  })

  it.each([
    { case: "empty string", value: "" },
    { case: "newlines only", value: "\n\n" },
    { case: "mixed whitespace", value: " \n \t " },
  ])("does not save when title is $case", async ({ value }) => {
    mountEditableTitle(makeMe.aNote.title("Dummy Title").please())
    await editTitleThenBlur(wrapper, value)
    await flushPromises()

    expect(mockedUpdateTitleCall).not.toBeCalled()
  })

  it("displays authorization error when save is rejected with 401", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false)
    mountEditableTitle(makeMe.aNote.title("Dummy Title").please())
    mockedUpdateTitleCall.mockRejectedValueOnce(
      makeMe.anApiError.of401().please()
    )
    await editTitleThenBlur(wrapper)
    await flushPromises()
    expect(wrapper.find(".path-name-editor .text-error").text()).toBe(
      "You are not authorized to edit this note. Perhaps you are not logged in?"
    )
    confirmSpy.mockRestore()
  })
})
