import NoteControlCenterForUser from "@/components/toolbars/NoteControlCenterForUser.vue";
import { VueWrapper } from "@vue/test-utils";
import makeMe from "tests/fixtures/makeMe";
import { beforeEach, describe } from "vitest";
import helper from "../helpers";

describe("NoteControlCenterForUser", () => {
  helper.resetWithApiMock(beforeEach, afterEach);
  let wrapper: VueWrapper;

  const note = makeMe.aNote.please();

  const mountComponent = (): VueWrapper => {
    return helper
      .component(NoteControlCenterForUser)
      .withStorageProps({ selectedNote: note })
      .mount();
  };

  beforeEach(() => {
    wrapper = mountComponent();
  });

  it("has only the link-note button when no exist selected note", () => {
    expect(wrapper.findAll(".btn")[0].attributes("title")).toEqual("link note");
  });
});
