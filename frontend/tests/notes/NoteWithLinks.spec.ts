import NoteWithLinks from "@/components/notes/NoteWithLinks.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("new/updated pink banner", () => {
  beforeAll(() => {
    Date.now = vi.fn(() => new Date(Date.UTC(2017, 1, 14)).valueOf());
  });

  it.each([
    [new Date(Date.UTC(2017, 1, 15)), "rgb(208,237,23)"],
    [new Date(Date.UTC(2017, 1, 13)), "rgb(189,209,64)"],
    [new Date(Date.UTC(2017, 1, 12)), "rgb(181,197,82)"],
    [new Date(Date.UTC(2016, 1, 12)), "rgb(150,150,150)"],
  ])(
    "should show fresher color if recently updated",
    (updatedAt, expectedColor) => {
      const note = makeMe.aNoteRealm.textContentUpdatedAt(updatedAt).please();

      const wrapper = helper
        .component(NoteWithLinks)
        .withStorageProps({ note: note.note, links: note.links })
        .mount();

      expect(wrapper.find(".note-body").element).toHaveStyle(
        `border-color: ${expectedColor};`
      );
    }
  );
});

describe("note associated with wikidata", () => {
  it("should display icon besides title when note is linked", async () => {
    const noteRealm = makeMe.aNoteRealm
      .title("Dummy Title")
      .wikidataId("DummyId")
      .please();

    const wrapper = helper
      .component(NoteWithLinks)
      .withStorageProps({
        note: noteRealm.note,
        links: noteRealm.links,
      })
      .mount();

    const element = await wrapper.find('[role="button"]');
    element.isVisible();
    expect(element.attributes("title")).toMatch("Wiki Association");
  });
});
