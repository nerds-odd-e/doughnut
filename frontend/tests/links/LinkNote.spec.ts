import { describe, it, expect, beforeEach } from "vitest";
import LinkNoteFinalize from "@/components/links/LinkNoteFinalize.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

describe("LinkNoteFinalize", () => {
  beforeEach(() => {
    helper.reset();
  });

  it("going back", async () => {
    const note = makeMe.aNoteRealm.please();
    const wrapper = helper
      .component(LinkNoteFinalize)
      .withStorageProps({
        note,
        targetNote: note,
      })
      .mount();
    await wrapper.find(".go-back-button").trigger("click");
    expect(wrapper.emitted().goBack).toHaveLength(1);
  });
});
