/**
 * @jest-environment jsdom
 */
import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import Modal from "@/components/commons/Modal.vue";

describe("Modal", () => {
  const Comp = Modal;
  const TestComponent = {
    template: `
      <Modal @close_request="$emit('close_request')">
        <template v-slot:header>
        </template>
        <template v-slot:body>
        </template>
        <template v-slot:footer>
        </template>
      </Modal>
    `,
    components: { Modal: Comp },
    emits: ["close_request"],
  };

  it("click on note when doing review", async () => {
    const wrapper = mount(TestComponent);
    expect(wrapper.find(".close-button").exists()).toBe(true);
    await wrapper.find(".close-button").trigger("click");
    expect(wrapper.emitted().close_request).toHaveLength(1);
  });

  it("click on note when doing review", async () => {
    const wrapper = mount(TestComponent);
    await wrapper.find(".modal-wrapper").trigger("mousedown");
    expect(wrapper.emitted().close_request).toHaveLength(1);
  });
});
