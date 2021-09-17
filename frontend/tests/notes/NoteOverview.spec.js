import { screen } from "@testing-library/vue"
import NoteOverview from "@/components/notes/NoteOverview.vue"
import { renderWithStoreAndMockRoute } from "../helpers"
import makeMe from "../fixtures/makeMe"

describe("note overview", () => {

  it("should render one note", async () => {
    const note = makeMe.aNote.title('single note').please()
    renderWithStoreAndMockRoute(NoteOverview, { props: {noteId: note.id} }, null, (store) => {
      store.commit('addNote', note)
    })
    await screen.findByText('single note')
  });

  it("should render note with one child", async () => {
    const noteParent = makeMe.aNote.title('parent').please()
    const noteChild = makeMe.aNote.title('child').please()
    renderWithStoreAndMockRoute(NoteOverview, { props: {noteId: noteParent.id} }, null, (store) => {
      store.commit('addNote', noteParent)
      store.commit('addNote', noteChild)
      store.commit('loadParentChildren', {[noteParent.id]: [noteChild.id]})
    })
    await screen.findByText('parent')
    await screen.findByText('child')
  });

  it("should render note with grandchild", async () => {
    const noteParent = makeMe.aNote.title('parent').please()
    const noteChild = makeMe.aNote.title('child').please()
    const noteGrandchild = makeMe.aNote.title('grandchild').please()
    renderWithStoreAndMockRoute(NoteOverview, { props: {noteId: noteParent.id} }, null, (store) => {
      store.commit('addNote', noteParent)
      store.commit('addNote', noteChild)
      store.commit('addNote', noteGrandchild)
      store.commit('loadParentChildren', {
        [noteParent.id]: [noteChild.id],
        [noteChild.id]: [noteGrandchild.id],
      })
    })
    await screen.findByText('parent')
    await screen.findByText('child')
    await screen.findByText('grandchild')
  });

});

