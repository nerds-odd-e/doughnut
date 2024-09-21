import NoteTextContent from "@/components/notes/core/NoteTextContent.vue"
import type { Note } from "@/generated/backend"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

const mockedUpdateTopicCall = vi.fn()

describe("in place edit on title", () => {
  const note = makeMe.aNote.topicConstructor("Dummy Title").please()
  const mountComponent = (
    n: Note,
    readonly = false
  ): VueWrapper<ComponentPublicInstance> => {
    return helper
      .component(NoteTextContent)
      .withStorageProps({
        readonly,
        note: n,
      })
      .mount()
  }

  beforeEach(() => {
    vi.resetAllMocks()
    helper.managedApi.restTextContentController.updateNoteTopicConstructor =
      mockedUpdateTopicCall
  })

  it("should display text field when one single click on title", async () => {
    const wrapper = mountComponent(note)
    expect(wrapper.findAll('[role="topic"] input')).toHaveLength(0)
    await wrapper.find('[role="topic"] h2').trigger("click")

    await flushPromises()

    expect(wrapper.findAll('[role="topic"] input')).toHaveLength(1)
    expect(wrapper.findAll('[role="topic"] h2')).toHaveLength(0)
  })

  it("should not save change when not unmount", async () => {
    const wrapper = mountComponent(note)
    await wrapper.find('[role="topic"]').trigger("click")
    await wrapper.find('[role="topic"] input').setValue("updated")
    wrapper.unmount()
  })

  it("is not editable when readonly", async () => {
    const wrapper = mountComponent(note, true)
    await wrapper.find('[role="topic"]').trigger("click")
    expect(wrapper.findAll("[role='topic'] input")).toHaveLength(0)
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
    await wrapper.find('[role="topic"]').trigger("click")
    await wrapper.find('[role="topic"] input').setValue("updated")
    wrapper.unmount()
    expect(mockedUpdateTopicCall).toBeCalledWith(note.id, {
      topicConstructor: "updated",
    })
  })

  const editTitle = async (
    wrapper: VueWrapper<ComponentPublicInstance>,
    newValue: string
  ) => {
    await wrapper.find('[role="topic"]').trigger("click")
    await wrapper.find('[role="topic"] input').setValue(newValue)
  }

  const editTitleThenBlur = async (
    wrapper: VueWrapper<ComponentPublicInstance>
  ) => {
    await editTitle(wrapper, "updated")
    await wrapper.find('[role="topic"] input').trigger("blur")
  }

  it("should save content when blur text field title", async () => {
    const wrapper = mountComponent(note)
    await editTitle(wrapper, "updated")
    await wrapper.find('[role="topic"] input').trigger("blur")
    expect(mockedUpdateTopicCall).toBeCalledWith(note.id, {
      topicConstructor: "updated",
    })
  })

  it("should not change content if there's unsaved changed", async () => {
    const wrapper = mountComponent(note)
    await editTitle(wrapper, "updated")

    await wrapper.setProps({
      note: { ...note, topicConstructor: "different value" },
    })
    expect(
      wrapper.find<HTMLInputElement>('[role="topic"] input').element.value
    ).toBe("updated")

    expect(mockedUpdateTopicCall).not.toBeCalled()
  })

  it("should change content if there's no unsaved changed but change from prop", async () => {
    const wrapper = mountComponent(note)
    await wrapper.setProps({
      note: {
        ...note,
        noteTopic: { ...note.noteTopic, topicConstructor: "different value" },
      },
    })
    await wrapper.find('[role="topic"]').trigger("click")
    expect(
      wrapper.find<HTMLInputElement>('[role="topic"] input').element.value
    ).toBe("different value")
  })

  describe("saved and having error", () => {
    let wrapper: VueWrapper<ComponentPublicInstance>
    beforeEach(async () => {
      wrapper = mountComponent(note)
      mockedUpdateTopicCall.mockRejectedValueOnce(
        makeMe.anApiError
          .ofBindingError({
            topic: "size must be between 1 and 100",
          })
          .please()
      )
      await editTitleThenBlur(wrapper)
      await flushPromises()
    })

    it("should display error when saving failed", async () => {
      expect(wrapper.find(".error-msg").text()).toBe(
        "size must be between 1 and 100"
      )
    })

    it("should clean up errors when editing", async () => {
      await editTitleThenBlur(wrapper)
      await flushPromises()
      expect(wrapper.findAll(".error-msg")).toHaveLength(0)
      expect(mockedUpdateTopicCall).toBeCalledTimes(2)
    })
  })

  it("should not trigger changes for initial details content", async () => {
    note.details = "initial\n\ndescription"
    const wrapper = mountComponent(note)
    await flushPromises()
    wrapper.unmount()
    expect(mockedUpdateTopicCall).toBeCalledTimes(0)
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
      mockedUpdateTopicCall.mockRejectedValueOnce(
        makeMe.anApiError.of401().please()
      )
      await editTitleThenBlur(wrapper)
      await flushPromises()
      expect(wrapper.find(".error-msg").text()).toBe(
        "You are not authorized to edit this note. Perhaps you are not logged in?"
      )
    })
  })
  describe("for a linking note", () => {
    const target = makeMe.aNote.underNote(note).please()
    const linkingNote = makeMe.aLink.to(target).please()

    it("should dispay target", async () => {
      const wrapper = mountComponent(linkingNote)
      expect(wrapper.text()).toContain(
        linkingNote.noteTopic.targetNoteTopic?.topicConstructor
      )
    })

    it("should dispay breadcrumbs", async () => {
      const wrapper = mountComponent(linkingNote)
      expect(wrapper.text()).toContain(note.noteTopic.topicConstructor)
    })
  })
})
