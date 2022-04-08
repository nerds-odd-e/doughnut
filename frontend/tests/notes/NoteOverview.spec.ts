/**
 * @jest-environment jsdom
 */

import NoteOverview from "@/components/notes/NoteOverview.vue";
import { screen } from "@testing-library/vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

describe("note overview", () => {

  beforeEach(()=>{
    helper.reset();
  });

  it("should render one note", async () => {
    const note = makeMe.aNoteRealm.title("single note").please();
    helper.store.loadNoteRealms([note]);
    helper.component(NoteOverview).withProps({ noteId: note.id, expandChildren: true }).render();
    expect(screen.getByRole("title")).toHaveTextContent("single note");
    expect(screen.getAllByRole("title")).toHaveLength(1);
  });

  it("should render one note with links", async () => {
    const note = makeMe.aNoteRealm.title("source").linkToSomeNote('target note').please();
    helper.store.loadNoteRealms([note]);
    helper.component(NoteOverview).withProps({ noteId: note.id, expandChildren: true }).render();
    await screen.findByText("target note");
  });

  it("should render note with one child", async () => {
    const noteParent = makeMe.aNoteRealm.title("parent").please();
    const noteChild = makeMe.aNoteRealm.title("child").under(noteParent).please();
    helper.store.loadNoteRealms([noteParent, noteChild]);
    helper.component(NoteOverview).withProps({ noteId: noteParent.id, expandChildren: true }).render()
    expect(screen.getAllByRole("title")).toHaveLength(2);
    await screen.findByText("parent");
    await screen.findByText("child");
  });

  it("should render note with grandchild", async () => {
    const noteParent = makeMe.aNoteRealm.title("parent").please();
    const noteChild = makeMe.aNoteRealm.title("child").under(noteParent).please();
    const noteGrandchild = makeMe.aNoteRealm.title("grandchild").under(noteChild).please();
    helper.store.loadNoteRealms([noteParent, noteChild, noteGrandchild]);
    helper.component(NoteOverview).withProps({ noteId: noteParent.id, expandChildren: true }).render()
    await screen.findByText("parent");
    await screen.findByText("child");
    await screen.findByText("grandchild");
  });

});
