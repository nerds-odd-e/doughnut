import NoteControlCenterForUser from "@/components/toolbars/NoteControlCenterForUser.vue";
import { StoredApi } from "@/store/StoredApiCollection";
import { VueWrapper } from "@vue/test-utils";
import makeMe from "tests/fixtures/makeMe";
import { beforeEach, describe } from "vitest";
import helper from "../helpers";

describe("NoteControlCenterForUser", () => {
  helper.resetWithApiMock(beforeEach, afterEach);

  let wrapper: VueWrapper;

  const mountComponent = (
    note?: Generated.Note,
    updateTextContent?: StoredApi["updateTextContent"]
  ): VueWrapper => {
    return helper
      .component(NoteControlCenterForUser)
      .withStorageProps({
        storageAccessor: {
          selectedNote: note,
          api: () => ({ updateTextContent }),
        },
      })
      .mount();
  };

  const mountComponentWithNote = (
    updateTextContent?: StoredApi["updateTextContent"]
  ) => {
    const note = makeMe.aNote.please();
    wrapper = mountComponent(note, updateTextContent);
  };

  it("has only the link-note button when no exist selected note", () => {
    wrapper = mountComponent();
    expect(wrapper.findAll(".btn")[0].attributes("title")).toEqual("link note");
  });

  it("has the suggest button when having selected note", () => {
    mountComponentWithNote();
    expect(wrapper.findAll(".btn")[4].attributes("title")).toEqual("Suggest1");
  });

  it("ask api be called once when clicking the suggest button", () => {
    const updateTextContent: StoredApi["updateTextContent"] = async (
      _id,
      noteContent
    ) => {
      expect(noteContent.description).toBe("suggestion");
      return {} as Generated.NoteRealm;
    };

    mountComponentWithNote(updateTextContent);
    helper.apiMock
      .expectingPost(`/api/ai/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion" });

    wrapper.findAll(".btn")[4].trigger("click");
  });

  it.skip('ask api be called many times until res.finishReason equal "stop" when clicking the suggest button', async () => {
    let beCalledTimes = 0;

    const updateTextContent: StoredApi["updateTextContent"] = async () => {
      beCalledTimes += 1;
      helper.apiMock
        .expectingPost(`/api/ai/ask-suggestions`)
        .andReturnOnce({ suggestion: "suggestion", finishReason: "stop" });
      return {} as Generated.NoteRealm;
    };

    mountComponentWithNote(updateTextContent);

    helper.apiMock
      .expectingPost(`/api/ai/ask-suggestions`)
      .andReturnOnce({ suggestion: "suggestion", finishReason: "length" });

    await wrapper.findAll(".btn")[4].trigger("click");

    setTimeout(() => expect(beCalledTimes).toBe(2), 1);
  });
});
