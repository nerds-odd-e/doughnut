/**
 * @jest-environment jsdom
 */
import LinkWikiDialog from "@/components/links/LinkWikiDialog.vue";
import SearchWikidataVue from "@/components/search/SearchWikidata.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

describe("Save wikidata id", () => {
  helper.resetWithApiMock(beforeEach, afterEach);

  it("should display modal when one single click on associate button", async () => {
    const note = makeMe.aNoteRealm.please();

    const wikiData = makeMe.aWikiDataDto.please();

    helper.apiMock
      .expectingGet("/api/wikidata/Q12434")
      .andReturnOnce({ wikiData });

    helper.apiMock
      .expectingPost(`/api/notes/${note.id}/updateWikidataId`)
      .andReturnOnce({ note });

    const wrapper = helper
      .component(LinkWikiDialog)
      .withProps({
        note,
      })
      .mount();

    wrapper.find("#wikiID-wikiID").setValue("Q12434");
    await wrapper.find('input[value="Save"]').trigger("submit");
  });

  xit("should associate wikidata ID by searching", async () => {
    const note = makeMe.aNoteRealm.please();

    const wikiData = makeMe.aWikiDataDto.please();

    helper.apiMock
      .expectingGet("/api/wikidata/Q12434")
      .andReturnOnce({ wikiData });

    helper.apiMock
      .expectingPost(`/api/notes/${note.id}/updateWikidataId`);

    const wrapper = helper
      .component(LinkWikiDialog)
      .withProps({
        note,
      })
      .mount();
    
    const child = wrapper.findComponent(SearchWikidataVue);
    child.vm.$emit('selected', "Q12434");
    await wrapper.vm.$nextTick();
    wrapper.emitted('selected');
    const input = wrapper.find("#wikiID-wikiID").element as HTMLInputElement;
    expect(input.value).toEqual("Q12434");

    await wrapper.find('input[value="Save"]').trigger("submit");

  });
});
