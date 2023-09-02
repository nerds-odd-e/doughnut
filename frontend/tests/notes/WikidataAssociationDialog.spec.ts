import { VueWrapper, flushPromises } from "@vue/test-utils";
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("Save wikidata id", () => {
  const wikidataId = "Q123";
  async function putWikidataIdAndSubmit(note: Generated.Note) {
    const wrapper = helper
      .component(WikidataAssociationDialog)
      .withStorageProps({
        note,
      })
      .mount();
    wrapper.find("#wikidataID-wikidataID").setValue(wikidataId);
    await wrapper.find('input[value="Save"]').trigger("submit");
    await flushPromises();
    return wrapper;
  }

  async function cancelOperation(wrapper: VueWrapper) {
    await wrapper.find('input[type="cancel"]').trigger("click");
    await flushPromises();
  }

  async function confirmDifference(wrapper: VueWrapper) {
    await wrapper.find('input[value="Confirm"]').trigger("submit");
    await flushPromises();
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-function, @typescript-eslint/no-unused-vars
  function doNothing(_: VueWrapper) {}

  it.each`
    noteTitle   | wikidataTitle | userAction           | shouldSave
    ${"dog"}    | ${"dog"}      | ${doNothing}         | ${true}
    ${"Dog"}    | ${"dog"}      | ${doNothing}         | ${true}
    ${"Canine"} | ${"dog"}      | ${doNothing}         | ${false}
    ${"Canine"} | ${"dog"}      | ${cancelOperation}   | ${false}
    ${"Canine"} | ${"dog"}      | ${confirmDifference} | ${true}
    ${"Canine"} | ${""}         | ${doNothing}         | ${true}
  `(
    "associate $noteTitle with $wikidataTitle and choose to $userAction",
    async ({ noteTitle, wikidataTitle, userAction, shouldSave }) => {
      const note = makeMe.aNote.topic(noteTitle).please();
      const wikidata = makeMe.aWikidataEntity
        .wikidataTitle(wikidataTitle)
        .please();

      helper.apiMock
        .expectingGet(`/api/wikidata/entity-data/${wikidataId}`)
        .andReturnOnce(wikidata);

      if (shouldSave) {
        helper.apiMock.expectingPost(`/api/notes/${note.id}/updateWikidataId`);
      }
      const wrapper = await putWikidataIdAndSubmit(note);
      userAction(wrapper);
      helper.apiMock.assertNoUnexpectedOrMissedCalls();
    },
  );
});
