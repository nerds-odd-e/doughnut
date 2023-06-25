import { flushPromises } from "@vue/test-utils";
import DescriptionEditor from "@/components/form/DescriptionEditor.vue";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("DescriptionEditor", () => {
  it("not emit update when the change is from initial value", async () => {
    const wrapper = helper
      .component(DescriptionEditor)
      .withProps({
        modelValue: "initial value",
      })
      .mount();

    await flushPromises();
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined();
  });
});
