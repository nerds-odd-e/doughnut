interface NoteRealmsReader {
  getNoteRealmById(id: Doughnut.ID): Generated.NoteRealm | undefined;
}

class NoteRealmCache implements NoteRealmsReader {
  noteRealms: { [id: Doughnut.ID]: Generated.NoteRealm } = {};

  notePosition;

  constructor(
    value:
      | Generated.NoteRealmWithAllDescendants
      | Generated.NoteRealmWithPosition,
  ) {
    this.notePosition = value.notePosition;
    if ("notes" in value) {
      value.notes.forEach((noteRealm) => {
        this.noteRealms[noteRealm.id] = noteRealm;
      });
    } else {
      this.noteRealms[value.noteRealm.id] = value.noteRealm;
    }
  }

  getNoteRealmById(id: Doughnut.ID | undefined) {
    if (id === undefined) return undefined;
    return this.noteRealms[id];
  }

  updateNoteRealm(noteRealm?: Generated.NoteRealm) {
    if (!noteRealm) return;
    this.noteRealms[noteRealm.id] = noteRealm;
  }

  getNotePosition(id: Doughnut.ID | undefined) {
    if (!id) return undefined;
    const ancestors: Generated.Note[] = [...this.notePosition.ancestors];
    let cursor = this.getNoteRealmById(id);
    while (
      cursor &&
      cursor.note.parentId &&
      cursor.note.id !== this.notePosition.noteId
    ) {
      cursor = this.getNoteRealmById(cursor.note.parentId);
      if (!cursor) return undefined;
      ancestors.unshift(cursor.note);
    }
    if (!cursor) return undefined;
    const { notebook } = this.notePosition;
    return {
      noteId: id,
      ancestors,
      notebook,
    } as Generated.NotePositionViewedByUser;
  }
}

export default NoteRealmCache;
export type { NoteRealmsReader };
