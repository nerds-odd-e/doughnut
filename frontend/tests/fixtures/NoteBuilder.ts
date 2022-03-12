import Builder from "./Builder";
import generateId from "./generateId";

class NoteBuilder extends Builder<Generated.Note> {
  data: Generated.Note;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
        id: generateId(),
        title: "Note1.1.1",
        shortDescription: '',
        createdAt: "2021-08-24T08:46:44.000+00:00",
        noteAccessories: {
          url: '',
          urlIsVideo: false,
          pictureUrl: '',
          pictureMask: '',
          useParentPicture: false,
          skipReview: false,
          updatedAt: '',
        },
        textContent: {
          title: "Note1.1.1",
          description: "Desc",
          updatedAt: "2021-08-24T08:46:44.000+00:00",

        },

      }
  }

  title(value: string): NoteBuilder {
    this.data.title = value;
    this.data.textContent.title = value;
    return this;
  }

  description(value: string): NoteBuilder {
    this.data.textContent.description = value;
    return this;
  }

  picture(value: string): NoteBuilder {
    this.data.notePicture = value;
    return this;
  }

  shortDescription(value: string): NoteBuilder {
    this.data.shortDescription = value;
    if (!this.data.textContent.description) {
      this.data.textContent.description = value;
    }
    return this;
  }

  under(value: Generated.NoteSphere): NoteBuilder {
    value.childrenIds.push(this.data.id)
    this.data.parentId = value.id

    return this;
  }

  textContentUpdatedAt(value: Date): NoteBuilder {
    this.data.textContent.updatedAt = value.toJSON();
    return this;
  }

  do(): Generated.Note {
    return this.data
  }
}

export default NoteBuilder;
