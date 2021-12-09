/**
 * @jest-environment jsdom
 */
import { render, screen } from "@testing-library/vue";
import NoteWithLinks from "@/components/notes/NoteWithLinks.vue";
import makeMe from "../fixtures/makeMe";
import { renderWithStoreAndMockRoute } from '../helpers';
import store from '../../src/store';
import Languages from "../../src/models/languages";

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

    renderWithStoreAndMockRoute(
      store,
      NoteWithLinks,
      { props: { note: noteParent } },
    )

    expect(screen.queryByRole("title-fallback")).not.toBeInTheDocument();    
  });
});

describe("outdated translations", () => {
  it("should not display outdated translation tag on indonesian translation beside title text when translation is not outdated", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").isTranslationOutdatedIDN(false).please();
    store.commit("loadNotes", [noteParent]);

    renderWithStoreAndMockRoute(
      store,
      NoteWithLinks,
      { props: { note: noteParent } },
    )

    expect(screen.queryByRole("outdated-tag")).not.toBeInTheDocument();    
  });

  it("should display outdated translation tag on indonesian translation beside title text when translation is outdated", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").isTranslationOutdatedIDN(true).please();
    store.commit("loadNotes", [noteParent]);

    renderWithStoreAndMockRoute(
      store,
      NoteWithLinks,
      { props: { note: noteParent } },
    )

    expect(screen.queryByRole("outdated-tag")).toBeInTheDocument();    
  });
});