import Builder from "./Builder";
import LinkBuilder from "./LinkBuilder";
import { merge } from "lodash";

let idCounter = 1;

const generateId = () => {
  return (idCounter++).toString();
};

class NoteBuilder extends Builder {
  data: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
      id: generateId(),
      parentId: null,
      noteContent: {},
      links: {},
      childrenIds: []
    };
  }

  title(value: string): NoteBuilder {
    this.data.title = value;
    this.data.noteContent.title = value;
    return this;
  }

  titleIDN(value: string): NoteBuilder {
    this.data.noteContent.titleIDN = value;
    return this;
  }

  description(value: string): NoteBuilder {
    this.data.description = value;
    this.data.noteContent.description = value;
    return this;
  }

  descriptionIDN(value: string): NoteBuilder {
    this.data.noteContent.descriptionIDN = value;
    return this;
  }

  picture(value: string): NoteBuilder {
    this.data.notePicture = value;
    return this;
  }

  shortDescription(value: string): NoteBuilder {
    this.data.shortDescription = value;
    if(!this.data.noteContent.description) {
      this.data.noteContent.description = value;
    }
    return this;
  }

  under(value: any): NoteBuilder {
    value.childrenIds.push(this.data.id)
    this.data.parentId = value.id

    return this;
  }

  updatedAt(value: Date): NoteBuilder {
    this.data.noteContent.updatedAt = value.toJSON();
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

  do(): any {
    return merge(
      {
        noteContent: {
          title: "Note1.1.1",
          description: "Desc",
          url: null,
          urlIsVideo: false,
          pictureUrl: null,
          pictureMask: null,
          useParentPicture: false,
          skipReview: false,
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
