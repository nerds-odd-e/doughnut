/**
 * @jest-environment jsdom
 */
import flushPromises from "flush-promises";
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

describe("Save wikidata id", () => {
  helper.resetWithApiMock(beforeEach, afterEach);

  beforeEach(() => {
    const wikidata = makeMe.aWikidataEntity.wikidataTitle("TDD").please();
    helper.apiMock.expectingGet("/api/wikidata/Q12434").andReturnOnce(wikidata);
  });

  it("should call the update API when save", async () => {
    const note = makeMe.aNote.title("TDD").please();
    helper.apiMock.expectingPost(`/api/notes/${note.id}/updateWikidataId`);

    const wrapper = helper
      .component(WikidataAssociationDialog)
      .withStorageProps({
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
      .withStorageProps({
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
});
