import Builder from "./Builder";
import generateId from "./generateId";

class NoteBuilder extends Builder<Generated.Note> {
  data: Generated.Note;

  constructor() {
    super();
    this.data = {
      id: generateId(),
      title: "Note1.1.1",
      wikidataId: "",
      deletedAt: "",
      noteAccessories: {
        url: "",
        urlIsVideo: false,
        pictureUrl: "",
        pictureMask: "",
        useParentPicture: false,
        skipReview: false,
        updatedAt: "",
      },
      textContent: {
        title: "Note1.1.1",
        description: "Desc",
        updatedAt: "2021-08-24T08:46:44.000+00:00",
      },
    };
  }

  for(note: Generated.Note | undefined) {
    if (note) {
      this.data = note;
    }
    return this;
  }

  title(value: string): NoteBuilder {
    this.data.title = value;
    this.data.textContent.title = value;
    return this;
  }

  wikidataId(value: string): NoteBuilder {
    this.data.wikidataId = value;
    return this;
  }

  description(value: string): NoteBuilder {
    this.data.textContent.description = value;
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

  textContentUpdatedAt(value: Date): NoteBuilder {
    this.data.textContent.updatedAt = value.toJSON();
    return this;
  }

  do(): Generated.Note {
    return this.data;
  }
}

export default NoteBuilder;
