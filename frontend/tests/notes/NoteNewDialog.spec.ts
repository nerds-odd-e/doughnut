/**
 * @jest-environment jsdom
 */
import NoteNewDialog from "@/components/notes/NoteNewDialog.vue";
import { flushPromises } from "@vue/test-utils";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("adding new note", () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  const note = makeMe.aNote.title("mythical").please();

  it("search for duplicate", async () => {
    helper.apiMock.expectingPost(`/api/notes/search`).andReturnOnce([note]);
    const wrapper = helper
      .component(NoteNewDialog)
      .withStorageProps({ parentId: 123 })
      .mount();
    await wrapper.find("input#note-title").setValue("myth");
    jest.runAllTimers();
    await flushPromises();
    expect(wrapper.text()).toContain("mythical");
  });

  it("search wikidata for result suggestions", async () => {
    const searchInput = "rock";
    const searchResult = makeMe.aWikidataSearchEntity.label("rock").please();
    helper.apiMock.expectingPost(`/api/notes/search`).andReturnOnce([]);
    helper.apiMock
      .expectingGet(`/api/wikidata/search/${searchInput}`)
      .andReturnOnce([searchResult]);
    const wrapper = helper
      .component(NoteNewDialog)
      .withStorageProps({ parentId: 123 })
      .mount();
    await wrapper.find("input#note-title").setValue(searchInput);
    await wrapper.find("button#search-wikidata").trigger("click");
    await flushPromises();
    await wrapper.find('select[name="wikidataSearchResult"]').trigger("click");
    await wrapper.find("option").trigger("click");
    await flushPromises();
    expect(wrapper.text()).toContain(searchInput);
  });
});
