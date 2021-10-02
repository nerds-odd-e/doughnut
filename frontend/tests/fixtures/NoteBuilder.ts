import { merge } from "lodash";
import Builder from "./Builder";
import LinkBuilder from "./LinkBuilder";

let idCounter = 1;

const generateId = () => {
  return idCounter++;
};

class NoteBuilder extends Builder {
  data: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
      id: generateId(),
      parentId: 8888,
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

  under(value: any): NoteBuilder {
    value.childrenIds.push(this.data.id)

    return this;
  }

  updatedAt(value: Date): NoteBuilder {
    this.data.noteContent.updatedAt = value.toJSON();
    return this;
  }

  linkTo(): NoteBuilder {
    merge(this.data.links, new LinkBuilder(undefined, "using").please());
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
