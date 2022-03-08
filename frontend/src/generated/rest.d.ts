/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 2.35.1025 on 2022-03-08 18:49:40.

declare namespace Generated {

    interface NoteViewedByUser {
        id: number;
        parentId?: number;
        title: string;
        shortDescription: string;
        notePicture?: string;
        createdAt: string;
        noteAccessories: NoteAccessories;
        links: { [P in LinkType]?: LinkViewed };
        childrenIds: number[];
        textContent: TextContent;
    }

    interface NotebooksViewedByUser {
        notebooks: Notebook[];
        subscriptions: Subscription[];
    }

    interface NoteAccessories {
        id: number;
        url: string;
        urlIsVideo: boolean;
        pictureUrl: string;
        pictureMask: string;
        useParentPicture: boolean;
        skipReview: boolean;
        updatedAt: string;
    }

    interface LinkViewed {
        direct: Link[];
        reverse: Link[];
    }

    interface TextContent {
        title: string;
        description: string;
        language: string;
        updatedAt: string;
    }

    interface Notebook {
        id: number;
        ownership: Ownership;
        headNote: Note;
        skipReviewEntirely: boolean;
        deletedAt: string;
    }

    interface Subscription {
        id: number;
        dailyTargetOfNewNotes: number;
        user: User;
        notebook: Notebook;
        headNote: Note;
        noteContent: NoteAccessories;
        title: string;
        shortDescription: string;
    }

    interface Link {
        id: number;
        sourceNote: Note;
        targetNote: Note;
        typeId: number;
        createdAt: string;
        linkTypeLabel: string;
        linkNameOfSource: string;
    }

    interface Ownership {
        id: number;
        circle: Circle;
    }

    interface Note {
        id: number;
        createdAt: string;
        deletedAt: string;
        notePicture: string;
        title: string;
        shortDescription: string;
        parentId?: number;
        createdAtAndUpdatedAt: string;
    }

    interface User {
        id: number;
        name: string;
        externalIdentifier: string;
        ownership: Ownership;
        dailyNewNotesCount: number;
        spaceIntervals: string;
    }

    interface Circle {
        id: number;
        name: string;
    }

    type LinkType = "related to" | "a specialization of" | "an application of" | "an instance of" | "a part of" | "tagged by" | "an attribute of" | "the opposite of" | "author of" | "using" | "an example of" | "before" | "similar to" | "confused with";

}
