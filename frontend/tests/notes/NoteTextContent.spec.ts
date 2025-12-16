import NoteTextContent from "@/components/notes/core/NoteTextContent.vue"
import type { Note } from "@generated/backend"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkServiceWithImplementation } from "@tests/helpers"

const mockedUpdateTitleCall = vi.fn()

describe("in place edit on title", () => {
  const note = makeMe.aNote.titleConstructor("Dummy Title").please()
  const mountComponent = (
    n: Note,
    readonly = false
  ): VueWrapper<ComponentPublicInstance> => {
    return helper
      .component(NoteTextContent)
      .withCleanStorage()
      .withProps({
        readonly,
        note: n,
      })
      .mount()
  }

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    mockSdkServiceWithImplementation("updateNoteTitle", async (options) => {
      return await mockedUpdateTitleCall(options)
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it("should display text field when one single click on title", async () => {
    const wrapper = mountComponent(note)
    await flushPromises()
    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    expect(titleEl.getAttribute("contenteditable")).toBe("true")
  })

  it("should not save change when not unmount", async () => {
    const wrapper = mountComponent(note)
    await flushPromises()
    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    titleEl.innerText = "updated"
    titleEl.dispatchEvent(new Event("input"))
    wrapper.unmount()
  })

  it("is not editable when readonly", async () => {
    const wrapper = mountComponent(note, true)
    await flushPromises()
    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    expect(titleEl.getAttribute("contenteditable")).toBe("false")
  })

  const getPlaceholder = (wrapper: VueWrapper<ComponentPublicInstance>) => {
    return wrapper.get("[data-placeholder]").attributes("data-placeholder")
  }

  it("should prompt people to add details", async () => {
    note.details = ""
    const wrapper = mountComponent(note)
    const placeholder = getPlaceholder(wrapper)
    expect(placeholder).toBe("Enter note details here...")
  })

  it("should not prompt people to add details if readonly", async () => {
    note.details = ""
    const wrapper = mountComponent(note, true)
    try {
      getPlaceholder(wrapper)
      expect(true, "there should not be placehodler for readonly").toBe(false)
    } catch (e: unknown) {
      if (e instanceof Error) {
        expect(e.message).toContain("Unable to get")
      } else {
        throw new Error("Unexpected error type")
      }
    }
  })

  it("should save change when unmount", async () => {
    const wrapper = mountComponent(note)
    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    titleEl.innerText = "updated"
    titleEl.dispatchEvent(new Event("input"))
    wrapper.unmount()
    expect(mockedUpdateTitleCall).toBeCalledWith({
      path: { note: note.id },
      body: { newTitle: "updated" },
    })
  })

  const editTitle = async (
    wrapper: VueWrapper<ComponentPublicInstance>,
    newValue: string
  ) => {
    await flushPromises()
    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    titleEl.innerText = newValue
    titleEl.dispatchEvent(new Event("input"))
  }

  const editTitleThenBlur = async (
    wrapper: VueWrapper<ComponentPublicInstance>
  ) => {
    await editTitle(wrapper, "updated")
    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    titleEl.dispatchEvent(new Event("blur"))
  }

  it("should save content when blur text field title", async () => {
    const wrapper = mountComponent(note)
    await flushPromises()
    await editTitle(wrapper, "updated")
    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    titleEl.dispatchEvent(new Event("blur"))
    expect(mockedUpdateTitleCall).toBeCalledWith({
      path: { note: note.id },
      body: { newTitle: "updated" },
    })
  })

  it("should not change content if there's unsaved changed", async () => {
    const wrapper = mountComponent(note)
    await editTitle(wrapper, "updated")

    await wrapper.setProps({
      note: { ...note, opicConstructor: "different value" },
    })
    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    expect(titleEl.innerText).toBe("updated")

    expect(mockedUpdateTitleCall).not.toBeCalled()
  })

  it("should keep unsaved changes when API returns with older value during typing", async () => {
    const wrapper = mountComponent(note)
    await flushPromises()

    const titleEl = wrapper.find('[role="title"]').element as HTMLElement

    titleEl.innerText = "ABC"
    titleEl.dispatchEvent(new Event("input"))
    await flushPromises()

    await vi.advanceTimersByTimeAsync(1000)
    await flushPromises()

    expect(mockedUpdateTitleCall).toHaveBeenCalledWith({
      path: { note: note.id },
      body: { newTitle: "ABC" },
    })

    titleEl.innerText = "ABCDEF"
    titleEl.dispatchEvent(new Event("input"))
    await flushPromises()

    await wrapper.setProps({
      note: {
        ...note,
        noteTopology: {
          ...note.noteTopology,
          titleOrPredicate: "ABC",
        },
      },
    })
    await flushPromises()

    expect(titleEl.innerText).toBe("ABCDEF")
  })

  it("should change content if there's no unsaved changed but change from prop", async () => {
    const wrapper = mountComponent(note)
    await wrapper.setProps({
      note: {
        ...note,
        noteTopology: {
          ...note.noteTopology,
          titleOrPredicate: "different value",
        },
      },
    })
    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    expect(titleEl.innerText).toBe("different value")
  })

  describe("saved and having error", () => {
    let wrapper: VueWrapper<ComponentPublicInstance>
    beforeEach(async () => {
      wrapper = mountComponent(note)
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

    it("should display error when saving failed", async () => {
      expect(wrapper.find(".error-message").text()).toBe(
        "size must be between 1 and 100"
      )
    })

    it("should clean up errors when editing", async () => {
      await editTitleThenBlur(wrapper)
      await flushPromises()
      expect(wrapper.findAll(".error-message")).toHaveLength(0)
      expect(mockedUpdateTitleCall).toBeCalledTimes(2)
    })
  })

  it("should not trigger changes for initial details content", async () => {
    note.details = "initial\n\ndescription"
    const wrapper = mountComponent(note)
    await flushPromises()
    wrapper.unmount()
    expect(mockedUpdateTitleCall).toBeCalledTimes(0)
  })

  describe("blank title handling", () => {
    it.each([
      { case: "empty string", value: "" },
      { case: "spaces only", value: "   " },
      { case: "newlines only", value: "\n\n" },
      { case: "mixed whitespace", value: " \n \t " },
    ])("should not save when title is $case", async ({ value }) => {
      const wrapper = mountComponent(note)
      await flushPromises()

      const titleEl = wrapper.find('[role="title"]').element as HTMLElement
      titleEl.innerText = value
      titleEl.dispatchEvent(new Event("input"))
      titleEl.dispatchEvent(new Event("blur"))
      await flushPromises()

      expect(mockedUpdateTitleCall).not.toBeCalled()
    })
  })

  describe("with mocked window.confirm", () => {
    // eslint-disable-next-line no-alert
    const jsdomConfirm = window.confirm
    beforeEach(() => {
      // eslint-disable-next-line no-alert
      window.confirm = () => false
    })

    afterEach(() => {
      // eslint-disable-next-line no-alert
      window.confirm = jsdomConfirm
    })

    it("should display error when no authorization to save", async () => {
      const wrapper = mountComponent(note)
      mockedUpdateTitleCall.mockRejectedValueOnce(
        makeMe.anApiError.of401().please()
      )
      await editTitleThenBlur(wrapper)
      await flushPromises()
      expect(wrapper.find(".error-message").text()).toBe(
        "You are not authorized to edit this note. Perhaps you are not logged in?"
      )
    })
  })
  describe("for a linking note", () => {
    const target = makeMe.aNote.underNote(note).please()
    const relationNote = makeMe.aRelationship.to(target).please()

    it("should dispay target", async () => {
      const wrapper = mountComponent(relationNote)
      expect(wrapper.text()).toContain(
        relationNote.noteTopology.targetNoteTopology?.titleOrPredicate
      )
    })

    it("should dispay breadcrumbs", async () => {
      const wrapper = mountComponent(relationNote)
      expect(wrapper.text()).toContain(note.noteTopology.titleOrPredicate)
    })
  })
})
