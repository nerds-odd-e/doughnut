/**
 * @jest-environment jsdom
 */
import NoteWithLinks from "@/components/notes/NoteWithLinks.vue";
import { useStore } from "@/store";
import makeMe from "../fixtures/makeMe";
import { screen } from "@testing-library/vue";
import {
  renderWithStoreAndMockRoute,
  mountWithStoreAndMockRoute,
} from "../helpers";
import { createTestingPinia } from "@pinia/testing";

afterEach(() => {
  fetch.resetMocks();
})

describe("new/updated pink banner", () => {
  const pinia = createTestingPinia();
  const store = useStore(pinia);
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
      const note = makeMe.aNote.textContentUpdatedAt(updatedAt).please();

      renderWithStoreAndMockRoute(pinia, NoteWithLinks, { props: { note } });

      expect(screen.getByRole("title").parentNode).toHaveStyle(
        `border-color: ${expectedColor};`
      );
    }
  );
});

describe("in place edit on title", () => {
  const pinia = createTestingPinia();
  const store = useStore(pinia);
  it("should display text field when one single click on title", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").please();
    store.loadNotes([noteParent]);

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
    store.loadNotes([noteParent]);

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
  const pinia = createTestingPinia();
  const store = useStore(pinia);
  it("should call addEditingToUndoHistory on submitChange", async () => {
    const note = makeMe.aNote.title("Dummy Title").please();
    store.loadNotes([note]);

    const updatedTitle = "updated";
    const { wrapper } = mountWithStoreAndMockRoute(store, NoteWithLinks, {
      props: {
        note,
      },
    });

    await wrapper.find('[role="title"]').trigger("click");
    await wrapper.find('[role="title"] input').setValue(updatedTitle);
    await wrapper.find('[role="title"] input').trigger("blur");

    expect(store.peekUndo()).toMatchObject({type: "editing"})
  });
});