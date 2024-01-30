import Builder from "./Builder";
import NoteRealmBuilder from "./NoteRealmBuilder";

class LinkBuilder extends Builder<Generated.Thing> {
  sourceNoteBuilder = new NoteRealmBuilder();

  do(): Generated.Thing {
    return {
      id: 8,
      linkType: "related to",
      sourceNote: this.sourceNoteBuilder.do().note,
      targetNote: new NoteRealmBuilder().do().note,
      createdAt: new Date().toISOString(),
      link: {
        id: 8,
        linkType: "related to",
        sourceNote: this.sourceNoteBuilder.do().note,
        targetNote: new NoteRealmBuilder().do().note,
      },
    };
  }
}

export default LinkBuilder;
