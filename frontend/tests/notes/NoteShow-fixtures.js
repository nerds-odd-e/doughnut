import { merge } from "lodash";

const DEFAULT_NOTE = {
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
};

const createMockNote = (properties) => {
  return merge(DEFAULT_NOTE, properties);
};

export { createMockNote };
