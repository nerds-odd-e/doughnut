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

  describe("search wikidata entry", () => {
    let wrapper;

    beforeEach(() => {
      helper.apiMock.expectingPost(`/api/notes/search`).andReturnOnce([]);
      wrapper = helper
        .component(NoteNewDialog)
        .withStorageProps({ parentId: 123 })
        .mount();
    });

    const titleInput = () => {
      return wrapper.find("input#note-title");
    };

    const searchAndSelectFirstResult = async (key: string) => {
      await titleInput().setValue(key);
      await wrapper.find("button#search-wikidata").trigger("click");
      await flushPromises();
      const result = await wrapper.find('select[name="wikidataSearchResult"]');
      result.findAll("option").at(1)?.setValue();
      await flushPromises();
    };

    it("search wikidata for result suggestions and replace title if title is the same but case is different", async () => {
      const searchResult = makeMe.aWikidataSearchEntity.label("Rock").please();
      helper.apiMock
        .expectingGet(`/api/wikidata/search/${"rock"}`)
        .andReturnOnce([searchResult]);
      await searchAndSelectFirstResult("rock");
      expect(<HTMLInputElement>titleInput().element.value).toBe("Rock");
    });

    it("search wikidata for result suggestions and replace title if title is the same but case is different", async () => {
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Canine")
        .please();
      helper.apiMock
        .expectingGet(`/api/wikidata/search/${"dog"}`)
        .andReturnOnce([searchResult]);
      await searchAndSelectFirstResult("dog");
      expect(<HTMLInputElement>titleInput().element.value).toBe("dog");
    });
  });
});
