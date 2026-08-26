import { NoteController } from "@generated/donut-backend-api/sdk.gen"
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
    },
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
})
