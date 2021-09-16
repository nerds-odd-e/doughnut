import { merge } from "lodash";
import Builder from "./Builder"

let idCounter = 1

const generateId = () => {
  return idCounter++
}

class NoteBuilder extends Builder {
  data: any

  constructor(parentBuilder?: Builder) {
    super(parentBuilder)
    this.data = {note:{ noteContent: {}}}
  }

  title(value: string): NoteBuilder {
    this.data.note.title = value
    this.data.note.noteContent.title = value
    return this
  }

  recentlyUpdated(value: boolean): NoteBuilder {
    this.data.recentlyUpdated = value
    return this
  }

  do(): any {
    const id = generateId()

    return merge({
      owns: true,
      recentlyUpdated: false,
      note: {
        id,
        noteContent: {
          id,
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
        head: false,
        shortDescription: "Desc",
        parentId: 4,
      },
      links: {},
    }, this.data)
  }
}

export default NoteBuilder