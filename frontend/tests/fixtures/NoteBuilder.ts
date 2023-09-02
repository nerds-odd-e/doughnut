import Builder from "./Builder";
import generateId from "./generateId";

class NoteBuilder extends Builder<Generated.Note> {
  data: Generated.Note;

  constructor() {
    super();
    this.data = {
      id: generateId(),
      topic: "Note1.1.1",
      description: "<p>Desc</p>",
      wikidataId: "",
      deletedAt: "",
      noteAccessories: {
        url: "",
        pictureUrl: "",
        pictureMask: "",
        useParentPicture: false,
      },
      updatedAt: "2021-08-24T08:46:44.000+00:00",
    };
  }

  for(note: Generated.Note | undefined) {
    if (note) {
      this.data = note;
    }
    return this;
  }

  topic(value: string): NoteBuilder {
    this.data.topic = value;
    return this;
  }

  wikidataId(value: string): NoteBuilder {
    this.data.wikidataId = value;
    return this;
  }

  description(value: string): NoteBuilder {
    this.data.description = value;
    return this;
  }

  picture(value: string): NoteBuilder {
    this.data.pictureWithMask = {
      notePicture: value,
      pictureMask: "",
    };
    return this;
  }

  under(value: Generated.NoteRealm): NoteBuilder {
    value.children ||= [];
    value.children.push(this.data);
    this.data.parentId = value.id;

    return this;
  }

  updatedAt(value: Date): NoteBuilder {
    this.data.updatedAt = value.toJSON();
    return this;
  }

  do(): Generated.Note {
    return this.data;
  }
}

export default NoteBuilder;
