import Builder from "./Builder"
import NoteRealmBuilder from "./NoteRealmBuilder"
import NotePositionBuilder from "./NotePositionBuilder"

class LinkViewedByUserBuilder extends Builder<Generated.LinkViewedByUser> {
  sourceNoteBuilder = new NoteRealmBuilder()

  do(): Generated.LinkViewedByUser {
    return {
      id: 8,
      linkTypeLabel: "a link",
      typeId: 1,
      readonly: false,
      sourceNoteWithPosition: {
        note: this.sourceNoteBuilder.do(),
        notePosition: new NotePositionBuilder().do()
      },
      targetNoteWithPosition: {
        note: new NoteRealmBuilder().do(),
        notePosition: new NotePositionBuilder().do()
      }
    };
  }
}

export default LinkViewedByUserBuilder
