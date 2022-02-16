/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NoteWithLinks from "@/components/notes/NoteWithLinks.vue";
import makeMe from "../fixtures/makeMe";
import {
  renderWithStoreAndMockRoute,
  mountWithStoreAndMockRoute,
} from "../helpers";
import store from "../../src/store";

describe("new/updated pink banner", () => {
  beforeAll(() => {
    Date.now = jest.fn(() => new Date(Date.UTC(2017, 1, 14)).valueOf());
  });

  test.each([
    [new Date(Date.UTC(2017, 1, 15)), "rgb(208,237,23)"],
    [new Date(Date.UTC(2017, 1, 13)), "rgb(189,209,64)"],
    [new Date(Date.UTC(2017, 1, 12)), "rgb(181,197,82)"],
    [new Date(Date.UTC(2016, 1, 12)), "rgb(150,150,150)"],
  ])(
    "should show fresher color if recently updated",
    (updatedAt, expectedColor) => {
      const note = makeMe.aNote.textContentUpdatedAt(updatedAt).please();

      renderWithStoreAndMockRoute(store, NoteWithLinks, { props: { note } });

      expect(screen.getByRole("title").parentNode).toHaveStyle(
        `border-color: ${expectedColor};`
      );
    }
  );
});

describe("in place edit on title", () => {
  it("should display text field when one single click on title", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").please();
    store.commit("loadNotes", [noteParent]);

    const { wrapper } = mountWithStoreAndMockRoute(store, NoteWithLinks, {
      props: {
        note: noteParent,
      },
    });

    expect(wrapper.findAll('[role="title"] input')).toHaveLength(0);
    await wrapper.find('[role="title"] h2').trigger("click");

    expect(wrapper.findAll('[role="title"] input')).toHaveLength(1);
    expect(wrapper.findAll('[role="title"] h2')).toHaveLength(0);
  });

  it("should back to label when blur text field title", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").please();
    store.commit("loadNotes", [noteParent]);

    const { wrapper } = mountWithStoreAndMockRoute(store, NoteWithLinks, {
      props: {
        note: noteParent,
      },
    });

    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue("updated");
    await wrapper.find('[role="title"] input').trigger("blur");

    expect(fetch).toHaveBeenCalledWith(
      `/api/text_content/${noteParent.id}`,
      expect.objectContaining({ method: "PATCH" })
    );
  });

});

describe("undo editing", () => {

  it("should call addEditingToUndoHistory on submitChange", async () => {
    const note = makeMe.aNote.title("Dummy Title").please();
    store.commit("loadNotes", [note]);

    const updatedTitle = "updated";
    const { wrapper } = mountWithStoreAndMockRoute(store, NoteWithLinks, {
      props: {
        note: note,
      },
    });

    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue(updatedTitle);
    await wrapper.find('[role="title"] input').trigger("blur");

    expect(store.getters.peekUndo()).toMatchObject({type: 'editing'})
  });
});
