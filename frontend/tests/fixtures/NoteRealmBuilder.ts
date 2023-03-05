import { merge } from "lodash";
import Builder from "./Builder";
import LinkViewedBuilder from "./LinkViewedBuilder";
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
      links: { links: {} },
      children: [],
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

  textContentUpdatedAt(value: Date): NoteRealmBuilder {
    this.noteBuilder.textContentUpdatedAt(value);
    return this;
  }

  location(location: Generated.NoteLocation): NoteRealmBuilder {
    this.data.note.location = location;
    return this;
  }

  linkToSomeNote(title: string): NoteRealmBuilder {
    return this.linkTo(new NoteRealmBuilder().title(title).do());
  }

  linkTo(note: Generated.NoteRealm): NoteRealmBuilder {
    merge(
      this.data.links.links,
      new LinkViewedBuilder("using", this.data, note).please()
    );
    return this;
  }

  do(): Generated.NoteRealm {
    this.data.note = this.noteBuilder.do();
    return this.data;
  }
}

export default NoteRealmBuilder;
