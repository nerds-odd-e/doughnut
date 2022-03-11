import { merge } from "lodash";
import Builder from "./Builder";
import LinkBuilder from "./LinkBuilder";

let idCounter = 1;

const generateId = () => {
  idCounter += 1;
  return idCounter;
}

class NoteBuilder extends Builder<Generated.NoteViewedByUser> {
  data: Generated.NoteViewedByUser;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
      id: generateId(),
      title: '',
      shortDescription: '',
      createdAt: '',
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
        title: '',
        description: '',
        updatedAt: '',

      },
      links: {},
      childrenIds: []
    };
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

  under(value: any): NoteBuilder {
    value.childrenIds.push(this.data.id)
    this.data.parentId = value.id

    return this;
  }

  textContentUpdatedAt(value: Date): NoteBuilder {
    this.data.textContent.updatedAt = value.toJSON();
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

  do(): Generated.NoteViewedByUser {
    return merge(
      {
        noteAccessories: {
          url: null,
          urlIsVideo: false,
          pictureUrl: null,
          pictureMask: null,
          useParentPicture: false,
          skipReview: false,
          updatedAt: "2021-08-24T08:46:44.000+00:00",
        },
        textContent: {
          title: "Note1.1.1",
          description: "Desc",
          updatedAt: "2021-08-24T08:46:44.000+00:00",
        },
        createdAt: "2021-08-24T08:46:44.000+00:00",
        title: "Note1.1.1",
        notePicture: null,
        links: {},
      },
      this.data
    );
  }
}

export default NoteBuilder;
