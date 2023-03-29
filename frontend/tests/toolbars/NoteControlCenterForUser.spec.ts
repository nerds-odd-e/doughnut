import NoteControlCenterForUser from "@/components/toolbars/NoteControlCenterForUser.vue";
import { VueWrapper } from "@vue/test-utils";
import makeMe from "tests/fixtures/makeMe";
import { beforeEach, describe } from "vitest";
import helper from "../helpers";

describe("NoteControlCenterForUser", () => {
  helper.resetWithApiMock(beforeEach, afterEach);
  let wrapper: VueWrapper;

  const mountComponent = (note?: Generated.Note): VueWrapper => {
    return helper
      .component(NoteControlCenterForUser)
      .withStorageProps({ storageAccessor: { selectedNote: note } })
      .mount();
  };

  it("has only the link-note button when no exist selected note", () => {
    wrapper = mountComponent();
    expect(wrapper.findAll(".btn")[0].attributes("title")).toEqual("link note");
  });

  it("has the suggest button when having selected note", () => {
    const note = makeMe.aNote.please();
    wrapper = mountComponent(note);
    expect(wrapper.findAll(".btn")[4].attributes("title")).toEqual("Suggest1");
  });
});
