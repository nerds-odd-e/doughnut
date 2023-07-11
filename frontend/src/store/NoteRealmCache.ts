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

  deleteNoteAndDescendents(noteId: Doughnut.ID) {
    this.deleteNoteFromParentChildrenList(noteId);
    this.deleteNote(noteId);
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

  private getChildrenByParentId(parentId: Doughnut.ID) {
    return this.getNoteRealmById(parentId)?.children;
  }

  private deleteNote(id: Doughnut.ID) {
    this.getChildrenByParentId(id)?.forEach((child) =>
      this.deleteNote(child.id),
    );
    delete this.noteRealms[id];
  }

  private deleteNoteFromParentChildrenList(id: Doughnut.ID) {
    const parent = this.getNoteRealmById(id)?.note.parentId;
    if (!parent) return;
    const children = this.getChildrenByParentId(parent);
    if (children) {
      const childrenIds = children.map((child) => child.id);
      const index = childrenIds.indexOf(id);
      if (index > -1) {
        children.splice(index, 1);
      }
    }
  }
}

export default NoteRealmCache;
export type { NoteRealmsReader };
