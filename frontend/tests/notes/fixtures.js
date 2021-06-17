const noteViewedByUser = {
    "navigation": {
    },

    ownership: {
        isFromCircle: false,
    },
    ancestors: [
        {
            "id": 1,
            "createdAt": "2021-06-15T07:18:58.000+00:00",
            "notePicture": "",
            "head": true,
            "noteTypeDisplay": "Child Note",
            "ancestors": [],
            "title": "asdf"
        },
        {
            "id": 3,
            "createdAt": "2021-06-15T07:22:00.000+00:00",
            "notePicture": "",
            "head": false,
            "noteTypeDisplay": "Child Note",
            "title": "2"
        }
    ],

    children: [
        {
          "id": 3,
          "createdAt": "2021-06-16T03:33:44.000+00:00",
          "title": "2",
          "notePicture": "",
          "head": false,
          "noteTypeDisplay": "Child Note"
        }
    ],

    "note": {
        "id": 2,
        noteTypeDisplay: "note",

        "noteContent": {
            "id": 2,
            "title": "asdf",
            "description": "asdf",
            "url": "",
            "urlIsVideo": false,
            "pictureUrl": "",
            "pictureMask": "",
            "useParentPicture": false,
            "skipReview": false,
            "hideTitleInArticle": false,
            "showAsBulletInArticle": false,
            "updatedAt": "2021-06-14T11:00:56.000+00:00"
        },
        "createdAt": "2021-06-14T11:00:56.000+00:00",
        "notePicture": "",
        "head": false,
        "title": "asdf"
    },
    "links": {
        "HAS": {
            "direct": [],
            "reverse": [
                {
                    "id": 1,
                    "sourceNote": {
                        "id": 3,
                        "createdAt": "2021-06-14T11:01:06.000+00:00",
                        "notePicture": "",
                        "head": false,
                        "title": "bbb"
                    },
                    "type": "belongs to",
                    "createdAt": "2021-06-14T11:01:26.000+00:00",
                    "linkType": "BELONGS_TO"
                }
            ]
        }
    }
}

export {noteViewedByUser}