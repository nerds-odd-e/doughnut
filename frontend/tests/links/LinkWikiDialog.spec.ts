/**
 * @jest-environment jsdom
 */
import LinkWikiDialog from "@/components/links/LinkWikiDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

describe("Save wikidata id", () => {
  helper.resetWithApiMock(beforeEach, afterEach);

  it("should display modal when one single click on associate button", async () => {
    const note = makeMe.aNoteRealm.please();
    const stubResponse = {
      note,
    };

    helper.apiMock
      .expectingPost(`/api/notes/${note.id}/updateWikidataId`)
      .andReturnOnce(stubResponse);
    const wrapper = helper
      .component(LinkWikiDialog)
      .withProps({
        note,
      })
      .mount();

    wrapper.find("#wikiID-wikiID").setValue("Q12434");
    await wrapper.find('input[value="Save"]').trigger("submit");
  });
});
