import Builder from "./Builder";
import generateId from "./generateId";

class NoteBuilder extends Builder<Generated.Note> {
  data: Generated.Note;

  constructor() {
    super();
    this.data = {
      id: generateId(),
      topicConstructor: "Note1.1.1",
      details: "<p>Desc</p>",
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
    this.data.topicConstructor = value;
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
