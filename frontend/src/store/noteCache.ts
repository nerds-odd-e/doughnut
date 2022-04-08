interface NoteCacheState {
  notebooks: Generated.NotebookViewedByUser[]
  notebooksMapByHeadNoteId: { [id: Doughnut.ID]: Generated.NotebookViewedByUser }
  noteRealms: { [id: Doughnut.ID]: Generated.NoteRealm }
}

class NoteCache {
  state

  constructor(state: NoteCacheState) {
    this.state = state
  }

  loadNotebooks(notebooks: Generated.NotebookViewedByUser[]) {
    this.state.notebooks = notebooks
    notebooks.forEach(nb => { this.loadNotebook(nb) })
  }

  loadNotePosition(notePosition: Generated.NotePositionViewedByUser) {
    notePosition.ancestors.forEach((note) => {
      const { id } = note;
      this.loadNote(id, note)
    });
    this.loadNotebook(notePosition.notebook)
  }

  loadNoteRealms(noteRealms: Generated.NoteRealm[]) {
    noteRealms.forEach((noteRealm) => {
      this.state.noteRealms[noteRealm.id] = noteRealm;
    });
  }

  deleteNoteAndDescendents(noteId: Doughnut.ID) {
    this.deleteNoteFromParentChildrenList(noteId)
    this.deleteNote(noteId)
  }

  getNoteRealmById(id: Doughnut.ID | undefined) {
    if (id === undefined) return undefined;
    return this.state.noteRealms[id]
  }

  getNotePosition(id: Doughnut.ID | undefined) {
    if (!id) return undefined
    const ancestors: Generated.Note[] = []
    let cursor = this.getNoteRealmById(id)
    while (cursor && cursor.note.parentId) {
      cursor = this.getNoteRealmById(cursor.note.parentId)
      if (!cursor) return undefined
      ancestors.unshift(cursor.note)
    }
    if (!cursor) return undefined
    const notebook = this.state.notebooksMapByHeadNoteId[cursor.id]
    return { noteId: id, ancestors, notebook } as Generated.NotePositionViewedByUser
  }

  private loadNotebook(notebook: Generated.NotebookViewedByUser) {
    this.state.notebooksMapByHeadNoteId[notebook.headNoteId] = notebook
  }

  private loadNote(id: Doughnut.ID, note: Generated.Note) {
    const noteRealm = this.state.noteRealms[id];
    if (!noteRealm) {
      this.state.noteRealms[id] = { id, note }
      return
    }
    noteRealm.note = note
  }

  private getChildrenIdsByParentId(parentId: Doughnut.ID) {
    return this.getNoteRealmById(parentId)?.childrenIds
  }

  private deleteNote(id: Doughnut.ID) {
    this.getChildrenIdsByParentId(id)?.forEach((cid: Doughnut.ID) => this.deleteNote(cid))
    delete this.state.noteRealms[id]
  }

  private deleteNoteFromParentChildrenList(id: Doughnut.ID) {
    const parent = this.getNoteRealmById(id)?.note.parentId
    if (!parent) return
    const children = this.getChildrenIdsByParentId(parent)
    if (children) {
      const index = children.indexOf(id)
      if (index > -1) {
        children.splice(index, 1);
      }
    }
  }
}

function noteCache(state: NoteCacheState) {
  return new NoteCache(state)
}

export default noteCache
export { NoteCacheState }