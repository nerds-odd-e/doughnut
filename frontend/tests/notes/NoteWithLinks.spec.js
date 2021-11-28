/**
 * @jest-environment jsdom
 */
import { render, screen } from "@testing-library/vue";
import NoteWithLinks from "@/components/notes/NoteWithLinks.vue";
import makeMe from "../fixtures/makeMe";
import { renderWithStoreAndMockRoute } from '../helpers';
import store from '../../src/store';
import Languages from "../../src/constants/lang";

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
      const note = makeMe.aNote.updatedAt(updatedAt).please();
      render(NoteWithLinks, { props: { note } });

      expect(screen.getByRole("title").parentNode).toHaveStyle(
        `border-color: ${expectedColor};`
      );
    }
  );
});

describe("fallback translation", () => {
  it("should display 'No translation available' text below the title when no title translation available", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").please();
    store.commit("loadNotes", [noteParent]);

    renderWithStoreAndMockRoute(
      store,
      NoteWithLinks,
      { props: { note: noteParent, language: Languages.ID } },
    )

    expect(screen.getByRole("title-fallback")).toHaveTextContent("No translation available");
  });

  it("should not display 'No translation available' text below the title when title translation available", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").titleIDN("Judul Palsu").please();
    store.commit("loadNotes", [noteParent]);
    store.commit("changeNotesLanguage", Languages.ID);

    renderWithStoreAndMockRoute(
      store,
      NoteWithLinks,
      { props: { note: noteParent } },
    )

    expect(screen.queryByRole("title-fallback")).not.toBeInTheDocument();
  });

  it("should not display 'no translation available' below title text when we view the original language", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").please();
    store.commit("loadNotes", [noteParent]);
    store.commit("changeNotesLanguage", Languages.EN);

    renderWithStoreAndMockRoute(
      store,
      NoteWithLinks,
      { props: { note: noteParent } },
    )

    expect(screen.queryByRole("title-fallback")).not.toBeInTheDocument();    
  });
});