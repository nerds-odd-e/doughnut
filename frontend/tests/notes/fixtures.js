import { basicNote } from "./fixtures-basic";
const noteViewedByUser = {
  owns: true,
  notebook: {
    id: 5,
    skipReviewEntirely: false,
    ownership: {
      isFromCircle: false,
    },
  },
  navigation: {},

  ancestors: [
    {
      id: 1,
      createdAt: "2021-06-15T07:18:58.000+00:00",
      notePicture: "",
      head: true,
      noteTypeDisplay: "Child Note",
      ancestors: [],
      title: "asdf",
    },
    {
      id: 3,
      createdAt: "2021-06-15T07:22:00.000+00:00",
      notePicture: "",
      head: false,
      noteTypeDisplay: "Child Note",
      title: "2",
    },
  ],

  children: [
    {
      id: 3,
      createdAt: "2021-06-16T03:33:44.000+00:00",
      title: "2",
      notePicture: "",
      head: false,
      noteTypeDisplay: "Child Note",
    },
  ],

  note: basicNote,
  links: {
    HAS: {
      direct: [],
      reverse: [
        {
          id: 1,
          sourceNote: {
            id: 3,
            createdAt: "2021-06-14T11:01:06.000+00:00",
            notePicture: "",
            head: false,
            title: "bbb",
          },
          type: "iw a specialization of",
          createdAt: "2021-06-14T11:01:26.000+00:00",
          linkType: "BELONGS_TO",
        },
      ],
    },
  },
};

const linkViewedByUser = {
  id: 8,
  linkTypeLabel: "a link",
  sourceNoteViewedByUser: noteViewedByUser,
  targetNoteViewedByUser: noteViewedByUser,
};

const reviewPointViewedByUser = {
  reviewPoint: {
    id: 3,
  },
  noteViewedByUser: noteViewedByUser,
};

export { noteViewedByUser, linkViewedByUser, reviewPointViewedByUser };
