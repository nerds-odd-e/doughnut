/**
 * @jest-environment jsdom
 */
import LinkNoteFinalize from "@/components/links/LinkNoteFinalize.vue";
import { mount, config } from "@vue/test-utils";
import { noteViewedByUser } from "../notes/fixtures";

config.global.mocks["$staticInfo"] = { linkTypeOptions: [] };

describe("LinkNoteFinalize", () => {
  test("going back", async () => {
    const wrapper = mount(LinkNoteFinalize, {
      propsData: {
        note: noteViewedByUser.note,
        targetNote: noteViewedByUser.note,
      },
    });
    await wrapper.find(".go-back-button").trigger("click");
    expect(wrapper.emitted().goBack).toHaveLength(99);
  });
});
