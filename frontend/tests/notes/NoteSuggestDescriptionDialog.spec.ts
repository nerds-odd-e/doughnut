import { beforeEach } from 'vitest';
import { describe } from 'vitest';
import { flushPromises, VueWrapper } from "@vue/test-utils";
import NoteSuggestDescriptionDialog from "@/components/notes/NoteSuggestDescriptionDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

describe("NoteSuggestDescriptionDialog", () => {
  helper.resetWithApiMock(beforeEach, afterEach);

  const note = makeMe.aNote.please();
  let wrapper: VueWrapper;
  const mountDialog = (): VueWrapper => {
    return helper
      .component(NoteSuggestDescriptionDialog)
      .withStorageProps({ selectedNote: note })
      .mount();
  };

  describe("when getting complete suggestion", () => {
    beforeEach(() => {
      helper.apiMock
        .expectingPost(`/api/ai/ask-suggestions`)
        .andReturnOnce({ suggestion: "suggestion" });
      wrapper = mountDialog();
    });

    it("fetches from api", async () => {
      await flushPromises();
      expect(wrapper.get("textarea").element).toHaveValue("suggestion");
    });

    it("uses right parameters", async () => {
      helper.apiMock.verifyCall(
        "/api/ai/ask-suggestions",
        expect.objectContaining({
          body: expect.stringContaining(
            `"prompt":"Tell me about \\"${note.title}\\""`
          ),
        })
      );
    });

    it("has the buttons disabled initially", async () => {
      expect(wrapper.get("button.btn-primary").element).toBeDisabled();
      expect(wrapper.get("button.btn-secondary").element).toBeDisabled();
    });

    it("can append content to the description", async () => {
      await flushPromises();
      helper.apiMock.expectingPatch(`/api/text_content/${note.id}`);
      wrapper.get("button.btn-primary").trigger("click");
      helper.apiMock.verifyCall(
        `/api/text_content/${note.id}`,
        expect.objectContaining({
          body: expect.objectContaining({ description: "suggestion" }),
        })
      );
    });

    describe("when the text is changed and ask again", () => {
      beforeEach(async () => {
        await flushPromises();
        wrapper.get("textarea").setValue("ask differently");
        helper.apiMock.expectingPost(`/api/ai/ask-suggestions`);
        wrapper.get("button.btn-secondary").trigger("click");
      });

      it("can ask again when the text is changed", async () => {
        helper.apiMock.verifyCall(
          "/api/ai/ask-suggestions",
          expect.objectContaining({
            body: expect.stringContaining(`"prompt":"ask differently"`),
          })
        );
      });
    });
  });
});
