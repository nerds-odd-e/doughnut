/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 2.35.1025 on 2022-05-06 18:24:16.

declare namespace Generated {

    interface CircleForUserView {
        id: number;
        name: string;
        invitationCode: string;
        notebooks: NotebooksViewedByUser;
        members: UserForOtherUserView[];
    }

    interface CircleJoiningByInvitation {
        invitationCode: string;
    }

    interface CommentCreation {
        description: string;
    }

    interface CurrentUserInfo {
        user: User;
        externalIdentifier: string;
    }

    interface DummyForGeneratingTypes {
        answerViewedByUser: AnswerViewedByUser;
        answerResult: AnswerResult;
        answer: Answer;
        comment: Comment;
    }

    interface InitialInfo {
        thingId: number;
        skipReview: boolean;
        reviewSetting: ReviewSetting;
    }

    interface LinkRequest {
        typeId: number;
        moveUnder: boolean;
        asFirstChild: boolean;
    }

    interface LinkViewed {
        direct: Link[];
        reverse: Link[];
    }

    interface LinkViewedByUser {
        id: number;
        sourceNoteWithPosition: NoteWithPosition;
        linkTypeLabel: string;
        typeId: number;
        targetNoteWithPosition: NoteWithPosition;
        readonly: boolean;
    }

    interface NoteCreation {
        linkTypeToParent: number;
        textContent: TextContent;
    }

    interface NotePositionViewedByUser {
        noteId: number;
        notebook: NotebookViewedByUser;
        ancestors: Note[];
    }

    interface NoteRealm {
        id: number;
        links: { [P in LinkType]?: LinkViewed };
        children: Note[];
        note: Note;
    }

    interface NoteWithPosition {
        notePosition: NotePositionViewedByUser;
        note: NoteRealm;
    }

    interface NotebookViewedByUser {
        id: number;
        headNoteId: number;
        headNote: Note;
        skipReviewEntirely: boolean;
        fromBazaar: boolean;
        ownership: Ownership;
    }

    interface NotebooksViewedByUser {
        notebooks: NotebookViewedByUser[];
        subscriptions: Subscription[];
    }

    interface NotesBulk {
        notePosition: NotePositionViewedByUser;
        notes: NoteRealm[];
    }

    interface QuizQuestionViewedByUser {
        quizQuestion: QuizQuestion;
        questionType: QuestionType;
        description: string;
        mainTopic: string;
        hintLinks: { [P in LinkType]?: LinkViewed };
        viceReviewPointIdList: number[];
        scope: Note[];
        options: Option[];
        pictureWithMask?: PictureWithMask;
    }

    interface RedirectToNoteResponse {
        noteId: number;
    }

    interface RepetitionForUser {
        reviewPoint: number;
        quizQuestion?: QuizQuestionViewedByUser;
        toRepeatCount: number;
    }

    interface ReviewPointViewedByUser {
        reviewPoint: ReviewPoint;
        linkViewedByUser?: LinkViewedByUser;
        reviewSetting: ReviewSetting;
    }

    interface ReviewStatus {
        toRepeatCount: number;
        learntCount: number;
        notLearntCount: number;
        toInitialReviewCount: number;
    }

    interface SearchTerm {
        allMyNotebooksAndSubscriptions: boolean;
        allMyCircles: boolean;
        note?: number;
        searchKey: string;
    }

    interface SelfEvaluation {
        selfEvaluation: SelfEvaluate;
    }

    interface UserForOtherUserView {
        name: string;
    }

    interface User {
        id: number;
        name: string;
        externalIdentifier: string;
        ownership: Ownership;
        dailyNewNotesCount: number;
        spaceIntervals: string;
    }

    interface AnswerViewedByUser {
        answerId: number;
        answerDisplay: string;
        correct: boolean;
        reviewPoint: ReviewPointViewedByUser;
        quizQuestion: QuizQuestionViewedByUser;
    }

    interface AnswerResult {
        answerId: number;
        correct: boolean;
        nextRepetition?: RepetitionForUser;
    }

    interface Answer {
        spellingAnswer: string;
        answerNoteId: number;
        question: QuizQuestion;
    }

    interface Comment {
        id: number;
        user: User;
        content: string;
    }

    interface ReviewSetting {
        id: number;
        rememberSpelling: boolean;
        level: number;
    }

    interface Link extends Thingy {
        sourceNote: Note;
        targetNote: Note;
        typeId: number;
        clozeSource: string;
        linkTypeLabel: string;
    }

    interface TextContent {
        title: string;
        description: string;
        updatedAt: string;
    }

    interface Note extends Thingy {
        noteAccessories: NoteAccessories;
        textContent: TextContent;
        title: string;
        pictureWithMask?: PictureWithMask;
        parentId?: number;
        shortDescription: string;
    }

    interface Ownership {
        id: number;
        circle?: Circle;
    }

    interface Subscription {
        id: number;
        dailyTargetOfNewNotes: number;
        user: User;
        notebook: Notebook;
        title: string;
        headNote: Note;
        shortDescription: string;
    }

    interface QuizQuestion {
        id: number;
        reviewPoint: number;
        questionTypeId: number;
        categoryLink: number;
        optionNoteIds: string;
        viceReviewPointIds: string;
        createdAt: string;
    }

    interface Option {
        noteId: number;
        display: string;
        pictureWithMask?: PictureWithMask;
        picture: boolean;
    }

    interface PictureWithMask {
        notePicture: string;
        pictureMask: string;
    }

    interface ReviewPoint {
        id: number;
        thing: Thing;
        lastReviewedAt: string;
        nextReviewAt: string;
        initialReviewedAt: string;
        repetitionCount: number;
        forgettingCurveIndex: number;
        removedFromReview: boolean;
    }

    interface Thingy {
        id: number;
    }

    interface NoteAccessories {
        url: string;
        urlIsVideo: boolean;
        pictureUrl: string;
        pictureMask: string;
        useParentPicture: boolean;
        skipReview: boolean;
        updatedAt: string;
    }

    interface Circle {
        id: number;
        name: string;
    }

    interface Notebook {
        id: number;
        ownership: Ownership;
        headNote: Note;
        skipReviewEntirely: boolean;
        deletedAt: string;
    }

    interface Thing {
        id: number;
        createdAt: string;
        note?: Note;
        link?: Link;
    }

    type LinkType = "related to" | "a specialization of" | "an application of" | "an instance of" | "a part of" | "tagged by" | "an attribute of" | "the opposite of" | "author of" | "using" | "an example of" | "before" | "similar to" | "confused with";

    type QuestionType = "CLOZE_SELECTION" | "SPELLING" | "PICTURE_TITLE" | "PICTURE_SELECTION" | "LINK_TARGET" | "LINK_SOURCE" | "LINK_SOURCE_WITHIN_SAME_LINK_TYPE" | "CLOZE_LINK_TARGET" | "DESCRIPTION_LINK_TARGET" | "WHICH_SPEC_HAS_INSTANCE" | "FROM_SAME_PART_AS" | "FROM_DIFFERENT_PART_AS";

    type SelfEvaluate = "reset" | "satisfying" | "sad" | "happy";

}
