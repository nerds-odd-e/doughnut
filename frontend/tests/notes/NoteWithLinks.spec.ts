/**
 * @jest-environment jsdom
 */
import NoteWithLinks from "@/components/notes/NoteWithLinks.vue";
import createNoteStorage from "../../src/store/createNoteStorage";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("new/updated pink banner", () => {
  beforeAll(() => {
    Date.now = jest.fn(() => new Date(Date.UTC(2017, 1, 14)).valueOf());
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

describe("in place edit on title", () => {
  it("should display text field when one single click on title", async () => {
    const noteParent = makeMe.aNoteRealm.title("Dummy Title").please();

    const wrapper = helper
      .component(NoteWithLinks)
      .withStorageProps({
        note: noteParent.note,
        links: noteParent.links,
      })
      .mount();

    expect(wrapper.findAll('[role="title"] input')).toHaveLength(0);
    await wrapper.find('[role="title"] h2').trigger("click");

    expect(wrapper.findAll('[role="title"] input')).toHaveLength(1);
    expect(wrapper.findAll('[role="title"] h2')).toHaveLength(0);
  });

  it("should back to label when blur text field title", async () => {
    const noteParentSphere = makeMe.aNoteRealm.title("Dummy Title").please();
    helper.apiMock.expectingPatch(`/api/text_content/${noteParentSphere.id}`);

    const wrapper = helper
      .component(NoteWithLinks)
      .withStorageProps({
        note: noteParentSphere.note,
        links: noteParentSphere.links,
      })
      .mount();

    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue("updated");
    await wrapper.find('[role="title"] input').trigger("blur");
  });
});

describe("undo editing", () => {
  it("should call addEditingToUndoHistory on submitChange", async () => {
    const histories = createNoteStorage();

    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please();
    helper.apiMock.expectingPatch(`/api/text_content/${noteRealm.id}`);

    const updatedTitle = "updated";
    const wrapper = helper
      .component(NoteWithLinks)
      .withProps({
        note: noteRealm.note,
        links: noteRealm.links,
        storageAccessor: histories,
      })
      .mount();

    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue(updatedTitle);
    await wrapper.find('[role="title"] input').trigger("blur");

    expect(histories.peekUndo()).toMatchObject({ type: "editing" });
  });
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
    expect(element.attributes("title")).toMatch("Wikidata");
  });
});

describe("note associated with location", () => {
  it("should display a map placeholder", async () => {
    const noteRealm = makeMe.aNoteRealm
      .title("Dummy Title")
      .location({ longitude: 123, latitude: 456 })
      .please();

    const wrapper = helper
      .component(NoteWithLinks)
      .withStorageProps({
        note: noteRealm.note,
        links: noteRealm.links,
      })
      .mount();

    const element = await wrapper.find(".map-applet");
    element.isVisible();
    expect(element.exists()).toBe(true);
  });
});
