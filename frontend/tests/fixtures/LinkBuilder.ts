import { Thing } from "@/generated/backend";
import Builder from "./Builder";
import NoteRealmBuilder from "./NoteRealmBuilder";

class LinkBuilder extends Builder<Thing> {
  sourceNoteBuilder = new NoteRealmBuilder();

  do(): Thing {
    return {
      id: 8,
      linkType: "related to",
      sourceNote: this.sourceNoteBuilder.do().note,
      targetNote: new NoteRealmBuilder().do().note,
    };
  }
}

export default LinkBuilder;
