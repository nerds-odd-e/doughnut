import { merge } from "lodash";
import Builder from "./Builder"

class NoteBuilder extends Builder {
  data: any

  constructor(parentBuilder: Builder, linkType: string) {
    super(parentBuilder)
    this.data = {}
  }

  recentlyUpdated(value: boolean): NoteBuilder {
    this.data.recentlyUpdated = value
    return this
  }

  do(): any {
    return merge({
      owns: true,
      recentlyUpdated: false,
      level: 1,
      note: {
        id: 7,
        noteContent: {
          id: 7,
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