import { flushPromises } from "@vue/test-utils";
import DetailsEditor from "@/components/form/DetailsEditor.vue";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("DetailsEditor", () => {
  it("not emit update when the change is from initial value", async () => {
    const wrapper = helper
      .component(DetailsEditor)
      .withProps({
        modelValue: "initial value",
      })
      .mount();

    await flushPromises();
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined();
  });
});
