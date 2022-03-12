import { merge } from "lodash";
import Builder from "./Builder";
import LinkBuilder from "./LinkBuilder";

let idCounter = 1;

const generateId = () => {
  idCounter += 1;
  return idCounter;
}

class NoteBuilder extends Builder<Generated.NoteSphere> {
  data: Generated.NoteSphere;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    const id = generateId()
    this.data = {
      id,
      note: {
        id,
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

      },
      links: {},
      childrenIds: []
    };
  }

  title(value: string): NoteBuilder {
    this.data.note.title = value;
    this.data.note.textContent.title = value;
    return this;
  }

  description(value: string): NoteBuilder {
    this.data.note.textContent.description = value;
    return this;
  }

  picture(value: string): NoteBuilder {
    this.data.note.notePicture = value;
    return this;
  }

  shortDescription(value: string): NoteBuilder {
    this.data.note.shortDescription = value;
    if (!this.data.note.textContent.description) {
      this.data.note.textContent.description = value;
    }
    return this;
  }

  under(value: any): NoteBuilder {
    value.childrenIds.push(this.data.note.id)
    this.data.note.parentId = value.id

    return this;
  }

  textContentUpdatedAt(value: Date): NoteBuilder {
    this.data.note.textContent.updatedAt = value.toJSON();
    return this;
  }

  linkToSomeNote(): NoteBuilder {
    merge(this.data.links, new LinkBuilder(undefined, "using").please());
    return this;
  }

  linkTo(note: any): NoteBuilder {
    merge(this.data.links, new LinkBuilder(undefined, "using").from(this.data).to(note).please());
    return this;
  }

  do(): Generated.NoteSphere {
    return this.data
  }
}

export default NoteBuilder;
