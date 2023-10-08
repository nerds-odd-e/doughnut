import { flushPromises } from "@vue/test-utils";
import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("RichMarkdownEditor", () => {
  it("not emit update when the change is from initial value", async () => {
    const wrapper = helper
      .component(RichMarkdownEditor)
      .withProps({
        modelValue: "initial value",
      })
      .mount();

    await flushPromises();
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined();
  });
});
