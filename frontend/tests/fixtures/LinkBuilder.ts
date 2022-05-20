import Builder from "./Builder";
import NoteRealmBuilder from "./NoteRealmBuilder";

class LinkBuilder extends Builder<Generated.Link> {
  sourceNoteBuilder = new NoteRealmBuilder();

  do(): Generated.Link {
    return {
      id: 8,
      linkTypeLabel: "a link",
      clozeSource: "xxx",
      typeId: 1,
      sourceNote: this.sourceNoteBuilder.do().note,
      targetNote: new NoteRealmBuilder().do().note,
    };
  }
}

export default LinkBuilder;
