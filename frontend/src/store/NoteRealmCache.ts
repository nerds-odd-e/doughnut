interface NoteRealmsReader {
  getNoteRealmById(id: Doughnut.ID): Generated.NoteRealm | undefined;
}

class NoteRealmCache implements NoteRealmsReader {
  noteRealms: { [id: Doughnut.ID]: Generated.NoteRealm } = {};

  constructor(noteRealms: Generated.NoteRealm[]) {
    noteRealms.forEach((noteRealm) => {
      this.noteRealms[noteRealm.id] = noteRealm;
    });
  }

  deleteNoteAndDescendents(noteId: Doughnut.ID) {
    this.deleteNoteFromParentChildrenList(noteId);
    this.deleteNote(noteId);
  }

  getNoteRealmById(id: Doughnut.ID | undefined) {
    if (id === undefined) return undefined;
    return this.noteRealms[id];
  }

  private getChildrenByParentId(parentId: Doughnut.ID) {
    return this.getNoteRealmById(parentId)?.children;
  }

  private deleteNote(id: Doughnut.ID) {
    this.getChildrenByParentId(id)?.forEach((child) =>
      this.deleteNote(child.id)
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
export { NoteRealmsReader };
