import Builder from "./Builder"
import NoteBuilder from "./NoteBuilder"
import NotePositionBuilder from "./NotePositionBuilder"

class LinkViewedByUserBuilder extends Builder {
  data: any;

  do(): any {
    return {
      id: 8,
      linkTypeLabel: "a link",
      sourceNoteWithPosition: {
        note: new NoteBuilder().do(),
        notePosition: new NotePositionBuilder().do()
      },
      targetNoteWithPosition: {
        note: new NoteBuilder().do(),
        notePosition: new NotePositionBuilder().do()
      }
    }
  }
}

export default LinkViewedByUserBuilder
