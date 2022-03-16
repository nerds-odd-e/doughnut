import { merge } from "lodash";
import Builder from "./Builder";
import LinkBuilder from "./LinkBuilder";
import NoteBuilder from "./NoteBuilder";

class NoteSphereBuilder extends Builder<Generated.NoteSphere> {
  data: Generated.NoteSphere;

  noteBuilder

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.noteBuilder = new NoteBuilder()
    const noteData = this.noteBuilder.data
    this.data = {
      id: noteData.id,
      note: noteData,
      links: {},
      childrenIds: []
    };
  }

  title(value: string): NoteSphereBuilder {
    this.noteBuilder.title(value);
    return this;
  }

  description(value: string): NoteSphereBuilder {
    this.noteBuilder.description(value);
    return this;
  }

  picture(value: string): NoteSphereBuilder {
    this.noteBuilder.picture(value);
    return this;
  }

  shortDescription(value: string): NoteSphereBuilder {
    this.noteBuilder.shortDescription(value);
    return this;
  }

  under(value: Generated.NoteSphere): NoteSphereBuilder {
    value?.childrenIds?.push(this.data.note.id)
    this.data.note.parentId = value.id

    return this;
  }

  textContentUpdatedAt(value: Date): NoteSphereBuilder {
    this.noteBuilder.textContentUpdatedAt(value);
    return this;
  }

  linkToSomeNote(title: string): NoteSphereBuilder {
    return this.linkTo(new NoteSphereBuilder().title(title).do());
  }

  linkTo(note: Generated.NoteSphere): NoteSphereBuilder {
    merge(this.data.links, new LinkBuilder(undefined, "using", this.data, note).please());
    return this;
  }

  do(): Generated.NoteSphere {
    this.data.note = this.noteBuilder.do()
    return this.data
  }
}

export default NoteSphereBuilder;
