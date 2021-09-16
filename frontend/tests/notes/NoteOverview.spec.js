import { screen } from "@testing-library/vue"
import NoteOverview from "@/components/notes/NoteOverview.vue"
import { renderWithStoreAndMockRoute } from "../helpers"
import makeMe from "../fixtures/makeMe"

describe("note overview", () => {

  it("should render one note", async () => {
    const note = makeMe.aNote.title('single note').please()
    renderWithStoreAndMockRoute(NoteOverview, { props: {noteId: note.note.id} }, null, (store) => {
      store.commit('addNote', note)
    })
    await screen.findByText('single note')
  });

  it("should render note with one child", async () => {
    const noteParent = makeMe.aNote.title('parent').please()
    const noteChild = makeMe.aNote.title('child').please()
    renderWithStoreAndMockRoute(NoteOverview, { props: {noteId: noteParent.note.id} }, null, (store) => {
      store.commit('addNote', noteParent)
      store.commit('addNote', noteChild)
      store.commit('loadParentChildren', {[noteParent.note.id]: [noteChild.note.id]})
    })
    await screen.findByText('parent')
    await screen.findByText('child')
  });

});

