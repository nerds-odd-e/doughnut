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

  it("should show remember spelling checkbox by default when isLinkNote is not provided", () => {
    const wrapper = helper
      .component(NoteRecallSettingForm)
      .withProps(defaultProps)
      .mount()

    const checkInputs = wrapper.findAllComponents({ name: "CheckInput" })
    const rememberSpellingInput = checkInputs.find(
      (c) => c.props("field") === "rememberSpelling"
    )

    expect(rememberSpellingInput).toBeDefined()
  })
})
