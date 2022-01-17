/**
 * @jest-environment jsdom
 */
import { render, screen } from "@testing-library/vue";
import NoteWithLinks from "@/components/notes/NoteWithLinks.vue";
import makeMe from "../fixtures/makeMe";
import { renderWithStoreAndMockRoute, mountWithStoreAndMockRoute } from '../helpers';
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
      { props: { note: noteParent, language: Languages.ID } },
    )

    expect(screen.queryByRole("outdated-tag")).toBeInTheDocument();
  });

  it("should not display outdated tag when translation is outdated and language is English", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").isTranslationOutdatedIDN(true).please();
    store.commit("loadNotes", [noteParent]);

    renderWithStoreAndMockRoute(
      store,
      NoteWithLinks,
      { props: { note: noteParent } },
    )

    expect(screen.queryByRole("outdated-tag")).not.toBeInTheDocument();
  });
});

describe("in place edit on title", () => {

  it("should display text field when one single click on title", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title").please();
    store.commit("loadNotes", [noteParent]);

    const { wrapper } = mountWithStoreAndMockRoute(
      store,
      NoteWithLinks,
      { 
        props: { 
          note: noteParent,
        } 
      },
    )

    expect(wrapper.findAll('[role="title"] input')).toHaveLength(0);
    await wrapper.find('[role="title"] h2').trigger('click');

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

    await wrapper.find('[role="title"]').trigger('click');
    await wrapper.find('[role="title"] input').trigger("blur");

    expect(fetch).toHaveBeenCalledWith(`/api/notes/${noteParent.id}`, expect.objectContaining({method: 'PATCH'}));
  });

  it("should update Indonesian title on blur when language is Indonesian", async () => {
    const noteParent = makeMe.aNote.title("Dummy Title IDN").please();
    store.commit("loadNotes", [noteParent]);

    const { wrapper } = mountWithStoreAndMockRoute(
      store,
      NoteWithLinks,
      { 
        props: { 
          note: noteParent,
          language: Languages.ID,
        } 
      },
    );

    await wrapper.find('[role="title"]').trigger('click');
    await wrapper.find('[role="title"] input').setValue('Dummy Title Updated');
    await wrapper.find('[role="title"] input').trigger('input');
    await wrapper.find('[role="title"] input').trigger("blur");

    expect(fetch).toHaveBeenCalledWith(`/api/notes/${noteParent.id}`, expect.objectContaining({method: 'PATCH'}));
  });
});

