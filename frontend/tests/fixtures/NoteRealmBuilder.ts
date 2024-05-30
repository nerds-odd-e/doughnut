import { merge } from "lodash";
import { NoteTopic, NoteRealm } from "@/generated/backend";
import Builder from "./Builder";
import LinkViewedBuilder from "./LinkViewedBuilder";
import NoteBuilder from "./NoteBuilder";
import generateId from "./generateId";

class NoteRealmBuilder extends Builder<NoteRealm> {
  data: NoteRealm;

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
    };
  }

  topicConstructor(value: string): NoteRealmBuilder {
    this.noteBuilder.topicConstructor(value);
    return this;
  }

  inCircle(circleName: string) {
    this.data.circle = {
      id: generateId(),
      name: circleName,
    };
    return this;
  }

  wikidataId(value: string): NoteRealmBuilder {
    this.noteBuilder.wikidataId(value);
    return this;
  }

  details(value: string): NoteRealmBuilder {
    this.noteBuilder.details(value);
    return this;
  }

  image(value: string): NoteRealmBuilder {
    this.noteBuilder.image(value);
    return this;
  }

  under(value: NoteRealm): NoteRealmBuilder {
    value?.children?.push(this.data.note);
    this.data.note.parentId = value.id;
    this.data.note.noteTopic.parentNoteTopic = value.note.noteTopic;

    return this;
  }

  updatedAt(value: Date): NoteRealmBuilder {
    this.noteBuilder.updatedAt(value);
    return this;
  }

  linkToSomeNote(title: string): NoteRealmBuilder {
    return this.linkTo(new NoteRealmBuilder().topicConstructor(title).do());
  }

  linkTo(note: NoteRealm): NoteRealmBuilder {
    merge(
      this.data.links,
      new LinkViewedBuilder(NoteTopic.linkType.USING, this.data, note).please(),
    );
    return this;
  }

  do(): NoteRealm {
    this.data.note = this.noteBuilder.do();
    return this.data;
  }
}

export default NoteRealmBuilder;
