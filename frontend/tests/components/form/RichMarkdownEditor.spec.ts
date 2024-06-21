import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "../../helpers"

describe("RichMarkdownEditor", () => {
  it("not emit update when the change is from initial value", async () => {
    const wrapper = helper
      .component(RichMarkdownEditor)
      .withProps({
        modelValue: "initial value",
        noteId: 3,
      })
      .mount()

    await flushPromises()
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })
})
