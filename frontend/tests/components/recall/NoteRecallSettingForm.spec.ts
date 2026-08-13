import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { describe, it, expect } from "vitest"
import type { VueWrapper } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import NoteRecallSettingForm from "@/components/recall/NoteRecallSettingForm.vue"

function checkInputByField(wrapper: VueWrapper, field: string) {
  return wrapper
    .findAllComponents({ name: "CheckInput" })
    .find((c) => c.props("field") === field)
}

describe("NoteRecallSettingForm", () => {
  const defaultProps = {
    noteId: 1,
    noteRecallSetting: {
      level: 0,
      rememberSpelling: false,
    },
    noteContent: "some body text",
    isLinkNote: false,
  }

  beforeEach(() => {
    mockSdkService(NoteController, "updateNoteRecallSetting", undefined)
  })

  it("should not show skip memory tracking checkbox", () => {
    const wrapper = helper
      .component(NoteRecallSettingForm)
      .withProps(defaultProps)
      .mount()

    expect(checkInputByField(wrapper, "skipMemoryTracking")).toBeUndefined()
  })

  it("should show remember spelling checkbox when isLinkNote is false", () => {
    const wrapper = helper
      .component(NoteRecallSettingForm)
      .withProps({ ...defaultProps, isLinkNote: false })
      .mount()

    expect(checkInputByField(wrapper, "rememberSpelling")).toBeDefined()
  })

  it("should hide remember spelling checkbox when isLinkNote is true", () => {
    const wrapper = helper
      .component(NoteRecallSettingForm)
      .withProps({ ...defaultProps, isLinkNote: true })
      .mount()

    expect(checkInputByField(wrapper, "rememberSpelling")).toBeUndefined()
  })

  describe("Remember Spelling checkbox disabled state", () => {
    it("should be disabled when noteContent is empty", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteContent: "" })
        .mount()

      expect(
        checkInputByField(wrapper, "rememberSpelling")?.props("disabled")
      ).toBe(true)
    })

    it("should be disabled when noteContent is undefined", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteContent: undefined })
        .mount()

      expect(
        checkInputByField(wrapper, "rememberSpelling")?.props("disabled")
      ).toBe(true)
    })

    it("should be disabled when noteContent is whitespace only", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteContent: "   " })
        .mount()

      expect(
        checkInputByField(wrapper, "rememberSpelling")?.props("disabled")
      ).toBe(true)
    })

    it("should be enabled when noteContent has text", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteContent: "Some content" })
        .mount()

      expect(
        checkInputByField(wrapper, "rememberSpelling")?.props("disabled")
      ).toBeFalsy()
    })

    it("should show error message when disabled", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteContent: "" })
        .mount()

      expect(wrapper.text()).toContain(
        "Remember spelling note need to have content"
      )
    })

    it("should not show error message when enabled", () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteContent: "Some content" })
        .mount()

      expect(wrapper.text()).not.toContain(
        "Remember spelling note need to have content"
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
          noteContent: "",
        })
        .mount()

      expect(
        checkInputByField(wrapper, "rememberSpelling")?.props("modelValue")
      ).toBe(false)
    })

    it("should enable checkbox when noteContent changes from empty to having content", async () => {
      const wrapper = helper
        .component(NoteRecallSettingForm)
        .withProps({ ...defaultProps, noteContent: "" })
        .mount()

      expect(
        checkInputByField(wrapper, "rememberSpelling")?.props("disabled")
      ).toBe(true)

      await wrapper.setProps({ noteContent: "This is the definition" })

      expect(
        checkInputByField(wrapper, "rememberSpelling")?.props("disabled")
      ).toBe(false)
    })
  })
})
