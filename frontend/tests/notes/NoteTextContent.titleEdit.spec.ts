import type { Note } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import type { ComponentPublicInstance } from "vue"
import { RESERVED_README_TITLE_MESSAGE } from "@/utils/reservedReadmeTitles"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
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

  const mountWith = (note: Note, readonly = false) => {
    wrapper = mountNoteTextContent(note, { readonly })
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

  it("displays an editable title by default", async () => {
    mountWith(makeMe.aNote.title("Dummy Title").please())
    await flushPromises()
    expect(titleEditorEl(wrapper).getAttribute("contenteditable")).toBe("true")
  })

  it("is not editable when readonly", async () => {
    mountWith(makeMe.aNote.title("Dummy Title").please(), true)
    await flushPromises()
    expect(titleEditorEl(wrapper).getAttribute("contenteditable")).toBe("false")
  })

  it("prompts for content when empty", async () => {
    mountWith(makeMe.aNote.title("Dummy Title").content("").please())
    expect(
      wrapper.get("[data-placeholder]").attributes("data-placeholder")
    ).toBe("Enter note content here...")
  })

  it("does not prompt for content when readonly", async () => {
    mountWith(makeMe.aNote.title("Dummy Title").content("").please(), true)
    expect(wrapper.find("[data-placeholder]").exists()).toBe(false)
  })

  it("saves title change on unmount", async () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    mountWith(note)
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
    mountWith(note)
    await editTitleThenBlur(wrapper)
    await flushPromises()
    expect(mockedUpdateTitleCall).toBeCalledWith({
      path: { note: note.id },
      body: { newTitle: "updated" },
    })
  })

  it("keeps unsaved title edits when props change", async () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    mountWith(note)
    await editTitle(wrapper, "updated")

    await wrapper.setProps({
      note: { ...note, content: "different value" },
    })
    expect(titleEditorEl(wrapper).innerText).toBe("updated")
    expect(mockedUpdateTitleCall).not.toBeCalled()
  })

  it("keeps newer local edits when API returns an older title", async () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    mountWith(note)
    await editTitle(wrapper, "ABC")
    await advanceNoteContentSaveDebounce()

    expect(mockedUpdateTitleCall).toHaveBeenCalledWith({
      path: { note: note.id },
      body: { newTitle: "ABC" },
    })

    await editTitle(wrapper, "ABCDEF")

    await wrapper.setProps({
      note: {
        ...note,
        noteTopology: {
          ...note.noteTopology,
          title: "ABC",
        },
      },
    })
    await flushPromises()

    expect(titleEditorEl(wrapper).innerText).toBe("ABCDEF")
  })

  it("updates title from props when there are no unsaved edits", async () => {
    const note = makeMe.aNote.title("Dummy Title").please()
    mountWith(note)
    await wrapper.setProps({
      note: {
        ...note,
        noteTopology: {
          ...note.noteTopology,
          title: "different value",
        },
      },
    })
    expect(titleEditorEl(wrapper).innerText).toBe("different value")
  })

  describe("when save fails with a binding error", () => {
    beforeEach(async () => {
      const note = makeMe.aNote.title("Dummy Title").please()
      mountWith(note)
      mockedUpdateTitleCall.mockRejectedValueOnce(
        makeMe.anApiError
          .ofBindingError({
            title: "size must be between 1 and 100",
          })
          .please()
      )
      await editTitleThenBlur(wrapper)
      await flushPromises()
    })

    it("displays the binding error", async () => {
      expect(wrapper.find(".path-name-editor .text-error").text()).toBe(
        "size must be between 1 and 100"
      )
    })

    it("clears the error after a successful edit", async () => {
      await editTitleThenBlur(wrapper)
      await flushPromises()
      expect(wrapper.findAll(".path-name-editor .text-error")).toHaveLength(0)
      expect(mockedUpdateTitleCall).toBeCalledTimes(2)
    })
  })

  it("displays reserved title error in the title field", async () => {
    mountWith(makeMe.aNote.title("Dummy Title").please())
    mockedUpdateTitleCall.mockRejectedValueOnce({
      title: RESERVED_README_TITLE_MESSAGE,
      status: 400,
    })
    await editTitleThenBlur(wrapper)
    await flushPromises()
    expect(wrapper.find(".path-name-editor .text-error").text()).toContain(
      "reserved"
    )
  })

  it("does not save title when unchanged on unmount", async () => {
    mountWith(makeMe.aNote.title("Dummy Title").please())
    await flushPromises()
    wrapper.unmount()
    expect(mockedUpdateTitleCall).toBeCalledTimes(0)
  })

  it.each([
    { case: "empty string", value: "" },
    { case: "spaces only", value: "   " },
    { case: "newlines only", value: "\n\n" },
    { case: "mixed whitespace", value: " \n \t " },
  ])("does not save when title is $case", async ({ value }) => {
    mountWith(makeMe.aNote.title("Dummy Title").please())
    await editTitleThenBlur(wrapper, value)
    await flushPromises()

    expect(mockedUpdateTitleCall).not.toBeCalled()
  })

  it("displays authorization error when save is rejected with 401", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false)
    mountWith(makeMe.aNote.title("Dummy Title").please())
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
