import Builder from "./Builder"
import NoteSphereBuilder from "./NoteSphereBuilder"
import NotePositionBuilder from "./NotePositionBuilder"

class LinkViewedByUserBuilder extends Builder<Generated.LinkViewedByUser> {
  sourceNoteBuilder = new NoteSphereBuilder()

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
        note: new NoteSphereBuilder().do(),
        notePosition: new NotePositionBuilder().do()
      }
    };
  }
}

export default LinkViewedByUserBuilder
