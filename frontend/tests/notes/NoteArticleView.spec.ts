/**
 * @jest-environment jsdom
 */

import { flushPromises } from "@vue/test-utils";
import NoteArticleView from "@/components/notes/views/NoteArticleView.vue";
import { screen } from "@testing-library/vue";
import NoteRealmCache from "../../src/store/noteRealmCache";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("note overview", () => {
  it("should render one note", async () => {
    const note = makeMe.aNoteRealm.title("single note").please();
    helper
      .component(NoteArticleView)
      .withProps({
        noteId: note.id,
        noteRealms: new NoteRealmCache([note]),
        expandChildren: true,
      })
      .render();
    expect(screen.getByRole("title")).toHaveTextContent("single note");
    expect(screen.getAllByRole("title")).toHaveLength(1);
  });

  it("should render one note with links", async () => {
    const note = makeMe.aNoteRealm
      .title("source")
      .linkToSomeNote("target note")
      .please();
    helper
      .component(NoteArticleView)
      .withProps({
        noteId: note.id,
        noteRealms: new NoteRealmCache([note]),
        expandChildren: true,
      })
      .render();
    await screen.findByText("target note");
  });

  it("should render note with one child", async () => {
    const noteParent = makeMe.aNoteRealm.title("parent").please();
    const noteChild = makeMe.aNoteRealm
      .title("child")
      .under(noteParent)
      .please();
    helper
      .component(NoteArticleView)
      .withProps({
        noteId: noteParent.id,
        noteRealms: new NoteRealmCache([noteParent, noteChild]),
        expandChildren: true,
      })
      .render();
    await flushPromises();
    expect(screen.getAllByRole("title")).toHaveLength(2);
    await screen.findByText("parent");
    await screen.findByText("child");
  });

  it("should render note with grandchild", async () => {
    const noteParent = makeMe.aNoteRealm.title("parent").please();
    const noteChild = makeMe.aNoteRealm
      .title("child")
      .under(noteParent)
      .please();
    const noteGrandchild = makeMe.aNoteRealm
      .title("grandchild")
      .under(noteChild)
      .please();
    helper
      .component(NoteArticleView)
      .withProps({
        noteId: noteParent.id,
        noteRealms: new NoteRealmCache([noteParent, noteChild, noteGrandchild]),
        expandChildren: true,
      })
      .render();
    await screen.findByText("parent");
    await screen.findByText("child");
    await screen.findByText("grandchild");
  });
});
