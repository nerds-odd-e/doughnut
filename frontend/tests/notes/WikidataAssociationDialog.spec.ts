/**
 * @jest-environment jsdom
 */
import flushPromises from "flush-promises";
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue";
import SearchWikidataVue from "@/components/search/SearchWikidata.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

describe("Save wikidata id", () => {
  helper.resetWithApiMock(beforeEach, afterEach);

  beforeEach(() => {
    const wikiData = makeMe.aWikiDataDto.wikidataTitle("TDD").please();
    helper.apiMock.expectingGet("/api/wikidata/Q12434").andReturnOnce(wikiData);
  });

  it("should call the update API when save", async () => {
    const note = makeMe.aNote.title("TDD").please();
    helper.apiMock.expectingPost(`/api/notes/${note.id}/updateWikidataId`);

    const wrapper = helper
      .component(WikidataAssociationDialog)
      .withProps({
        note,
      })
      .mount();

    wrapper.find("#wikidataID-wikidataID").setValue("Q12434");
    await wrapper.find('input[value="Save"]').trigger("submit");
  });

  it("should ask for confirmation if the title is different", async () => {
    const note = makeMe.aNote.title("Test-Driven Development").please();
    const wrapper = helper
      .component(WikidataAssociationDialog)
      .withProps({
        note,
      })
      .mount();

    wrapper.find("#wikidataID-wikidataID").setValue("Q12434");
    await wrapper.find('input[value="Save"]').trigger("submit");
    await flushPromises();
    await wrapper.find('input[type="cancel"]').trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain(
      "Associate Test-Driven Development to Wikidata"
    );
  });

  xit("should associate wikidata ID by searching", async () => {
    const note = makeMe.aNoteRealm.please();

    const wikiData = makeMe.aWikiDataDto.please();

    helper.apiMock
      .expectingGet("/api/wikidata/Q12434")
      .andReturnOnce({ wikiData });

    helper.apiMock.expectingPost(`/api/notes/${note.id}/updateWikidataId`);

    const wrapper = helper
      .component(WikidataAssociationDialog)
      .withProps({
        note,
      })
      .mount();

    const child = wrapper.findComponent(SearchWikidataVue);
    child.vm.$emit("selected", "Q12434");
    await wrapper.vm.$nextTick();
    wrapper.emitted("selected");
    const input = wrapper.find("#wikiID-wikiID").element as HTMLInputElement;
    expect(input.value).toEqual("Q12434");

    await wrapper.find('input[value="Save"]').trigger("submit");
  });
});
