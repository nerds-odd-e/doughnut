interface NoteCacheState {
  notebooksMapByHeadNoteId: {
    [id: Doughnut.ID]: Generated.NotebookViewedByUser;
  };
  noteRealms: { [id: Doughnut.ID]: Generated.NoteRealm };
}

class NoteCache {
  state;

  constructor(state: NoteCacheState) {
    this.state = state;
  }

  loadNotePosition(notePosition: Generated.NotePositionViewedByUser) {
    notePosition.ancestors.forEach((note) => {
      const { id } = note;
      this.loadNote(id, note);
    });
    this.loadNotebook(notePosition.notebook);
  }

  loadNoteRealms(noteRealms: Generated.NoteRealm[]) {
    noteRealms.forEach((noteRealm) => {
      this.state.noteRealms[noteRealm.id] = noteRealm;
    });
  }

  private loadNotebook(notebook: Generated.NotebookViewedByUser) {
    this.state.notebooksMapByHeadNoteId[notebook.headNoteId] = notebook;
  }

  private loadNote(id: Doughnut.ID, note: Generated.Note) {
    const noteRealm = this.state.noteRealms[id];
    if (!noteRealm) {
      this.state.noteRealms[id] = { id, note };
      return;
    }
    noteRealm.note = note;
  }
}

function noteCache(state: NoteCacheState) {
  return new NoteCache(state);
}

export default noteCache;
export { NoteCacheState };
