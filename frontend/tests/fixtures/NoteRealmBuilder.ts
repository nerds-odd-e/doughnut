import { merge } from "lodash";
import Builder from "./Builder";
import LinkViewedBuilder from "./LinkViewedBuilder";
import NoteBuilder from "./NoteBuilder";
import NotePositionBuilder from "./NotePositionBuilder";

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
      children: [],
      notePosition: new NotePositionBuilder().for(noteData).please(),
    };
  }

  title(value: string): NoteRealmBuilder {
    this.noteBuilder.title(value);
    return this;
  }

  wikidataId(value: string): NoteRealmBuilder {
    this.noteBuilder.wikidataId(value);
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

  under(value: Generated.NoteRealm): NoteRealmBuilder {
    value?.children?.push(this.data.note);
    this.data.note.parentId = value.id;

    return this;
  }

  updatedAt(value: Date): NoteRealmBuilder {
    this.noteBuilder.updatedAt(value);
    return this;
  }

  linkToSomeNote(title: string): NoteRealmBuilder {
    return this.linkTo(new NoteRealmBuilder().title(title).do());
  }

  linkTo(note: Generated.NoteRealm): NoteRealmBuilder {
    merge(
      this.data.links,
      new LinkViewedBuilder("using", this.data, note).please(),
    );
    return this;
  }

  do(): Generated.NoteRealm {
    this.data.note = this.noteBuilder.do();
    return this.data;
  }
}

export default NoteRealmBuilder;
