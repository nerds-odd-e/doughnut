import { describe, it, expect } from "vitest"
import helper, { mockSdkService } from "@tests/helpers"
import NoteRecallSettingForm from "@/components/recall/NoteRecallSettingForm.vue"

describe("NoteRecallSettingForm", () => {
  const defaultProps = {
    noteId: 1,
    noteRecallSetting: {
      level: 0,
      rememberSpelling: false,
      skipMemoryTracking: false,
    },
    noteDetails: "some details",
  }

  beforeEach(() => {
    mockSdkService("updateNoteRecallSetting", undefined)
  })

  it("should show remember spelling checkbox when isLinkNote is false", () => {
    const wrapper = helper
      .component(NoteRecallSettingForm)
      .withProps({ ...defaultProps, isLinkNote: false })
      .mount()

    const checkInputs = wrapper.findAllComponents({ name: "CheckInput" })
    const rememberSpellingInput = checkInputs.find(
      (c) => c.props("field") === "rememberSpelling"
    )

    expect(rememberSpellingInput).toBeDefined()
  })

  it("should hide remember spelling checkbox when isLinkNote is true", () => {
    const wrapper = helper
      .component(NoteRecallSettingForm)
      .withProps({ ...defaultProps, isLinkNote: true })
      .mount()

    const checkInputs = wrapper.findAllComponents({ name: "CheckInput" })
    const rememberSpellingInput = checkInputs.find(
      (c) => c.props("field") === "rememberSpelling"
    )

    expect(rememberSpellingInput).toBeUndefined()
  })

  describe("Remember Spelling checkbox disabled state", () => {
    it("should be disabled when noteDetails is empty", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteDetails: "" })
        .mount()

      const checkInputs = wrapper.findAllComponents({ name: "CheckInput" })
      const rememberSpellingInput = checkInputs.find(
        (c) => c.props("field") === "rememberSpelling"
      )

      expect(rememberSpellingInput?.props("disabled")).toBe(true)
    })

    it("should be disabled when noteDetails is undefined", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteDetails: undefined })
        .mount()

      const checkInputs = wrapper.findAllComponents({ name: "CheckInput" })
      const rememberSpellingInput = checkInputs.find(
        (c) => c.props("field") === "rememberSpelling"
      )

      expect(rememberSpellingInput?.props("disabled")).toBe(true)
    })

    it("should be disabled when noteDetails is whitespace only", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteDetails: "   " })
        .mount()

      const checkInputs = wrapper.findAllComponents({ name: "CheckInput" })
      const rememberSpellingInput = checkInputs.find(
        (c) => c.props("field") === "rememberSpelling"
      )

      expect(rememberSpellingInput?.props("disabled")).toBe(true)
    })

    it("should be enabled when noteDetails has content", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteDetails: "Some content" })
        .mount()

      const checkInputs = wrapper.findAllComponents({ name: "CheckInput" })
      const rememberSpellingInput = checkInputs.find(
        (c) => c.props("field") === "rememberSpelling"
      )

      expect(rememberSpellingInput?.props("disabled")).toBeFalsy()
    })

    it("should show error message when disabled", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteDetails: "" })
        .mount()

      expect(wrapper.text()).toContain(
        "Remember spelling note need to have detail"
      )
    })

    it("should not show error message when enabled", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteDetails: "Some content" })
        .mount()

      expect(wrapper.text()).not.toContain(
        "Remember spelling note need to have detail"
      )
    })

    it("should show unchecked when disabled even if rememberSpelling is true", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({
          ...defaultProps,
          noteRecallSetting: {
            ...defaultProps.noteRecallSetting,
            rememberSpelling: true,
          },
          noteDetails: "",
        })
        .mount()

      const checkInputs = wrapper.findAllComponents({ name: "CheckInput" })
      const rememberSpellingInput = checkInputs.find(
        (c) => c.props("field") === "rememberSpelling"
      )

      expect(rememberSpellingInput?.props("modelValue")).toBe(false)
    })
  })
})
