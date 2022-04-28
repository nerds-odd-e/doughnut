import { merge } from "lodash";
import Builder from "./Builder";
import LinkBuilder from "./LinkBuilder";
import NoteBuilder from "./NoteBuilder";

class NoteRealmBuilder extends Builder<Generated.NoteRealm> {
  data: Generated.NoteRealm;

  noteBuilder;

  constructor() {
    super();
    this.noteBuilder = new NoteBuilder();
    const noteData = this.noteBuilder.data;
    this.data = {
      id: noteData.id,
      note: noteData,
      links: {},
      childrenIds: [],
    };
  }

  title(value: string): NoteRealmBuilder {
    this.noteBuilder.title(value);
    return this;
  }

  description(value: string): NoteRealmBuilder {
    this.noteBuilder.description(value);
    return this;
  }

  picture(value: string): NoteRealmBuilder {
    this.noteBuilder.picture(value);
    return this;
  }

  shortDescription(value: string): NoteRealmBuilder {
    this.noteBuilder.shortDescription(value);
    return this;
  }

  under(value: Generated.NoteRealm): NoteRealmBuilder {
    value?.childrenIds?.push(this.data.note.id);
    this.data.note.parentId = value.id;

    return this;
  }

  textContentUpdatedAt(value: Date): NoteRealmBuilder {
    this.noteBuilder.textContentUpdatedAt(value);
    return this;
  }

  linkToSomeNote(title: string): NoteRealmBuilder {
    return this.linkTo(new NoteRealmBuilder().title(title).do());
  }

  linkTo(note: Generated.NoteRealm): NoteRealmBuilder {
    merge(this.data.links, new LinkBuilder("using", this.data, note).please());
    return this;
  }

  do(): Generated.NoteRealm {
    this.data.note = this.noteBuilder.do();
    return this.data;
  }
}

export default NoteRealmBuilder;
