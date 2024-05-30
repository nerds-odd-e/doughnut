import { Note, NoteRealm } from "@/generated/backend";
import Builder from "./Builder";
import generateId from "./generateId";

class NoteBuilder extends Builder<Note> {
  data: Note;

  constructor() {
    super();
    const id = generateId();
    this.data = {
      id,
      topic: "Note1.1.1",
      topicConstructor: "Note1.1.1",
      noteTopic: {
        id,
        topicConstructor: "Note1.1.1",
      },
      details: "<p>Desc</p>",
      wikidataId: "",
      deletedAt: "",
      createdAt: new Date().toISOString(),
      updatedAt: "2021-08-24T08:46:44.000+00:00",
    };
  }

  for(note: Note | undefined) {
    if (note) {
      this.data = note;
    }
    return this;
  }

  topicConstructor(value: string): NoteBuilder {
    this.data.topicConstructor = value;
    this.data.noteTopic.topicConstructor = value;
    return this;
  }

  wikidataId(value: string): NoteBuilder {
    this.data.wikidataId = value;
    return this;
  }

  details(value: string): NoteBuilder {
    this.data.details = value;
    return this;
  }

  under(value: NoteRealm): NoteBuilder {
    value.children ||= [];
    value.children.push(this.data);
    this.underNote(value.note);
    return this;
  }

  underNote(value: Note): NoteBuilder {
    this.data.parentId = value.id;
    this.data.noteTopic.parentNoteTopic = value.noteTopic;
    return this;
  }

  updatedAt(value: Date): NoteBuilder {
    this.data.updatedAt = value.toJSON();
    return this;
  }

  linkType(value: Note.linkType): NoteBuilder {
    this.data.linkType = value;
    this.topicConstructor(`:${value}`);
    // default target
    this.data.noteTopic.targetNoteTopic = {
      id: generateId(),
      topicConstructor: "a target",
    };
    return this;
  }

  target(note: Note): NoteBuilder {
    this.data.noteTopic.targetNoteTopic = note.noteTopic;
    return this;
  }

  do(): Note {
    return this.data;
  }
}

export default NoteBuilder;
