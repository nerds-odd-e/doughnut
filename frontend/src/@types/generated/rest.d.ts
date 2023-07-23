/* tslint:disable */
/* eslint-disable */

declare namespace Generated {

    interface AIGeneratedQuestion {
        /**
         * The stem of the multiple-choice question
         */
        stem: string;
        /**
         * All choices. Only one should be correct.
         */
        choices: string[];
        /**
         * Index of the correct choice. 0-based.
         */
        correctChoiceIndex: number;
        /**
         * Background information or disclosure necessary to clarify the question. Use only if the stem would be unclear or ambiguous without this information. Will be put before stem.
         */
        background?: string;
    }

    interface AiCompletion {
        moreCompleteContent: string;
        finishReason: string;
    }

    interface AiCompletionRequest {
        prompt: string;
        incompleteContent: string;
    }

    interface AiGeneratedImage {
        b64encoded: string;
    }

    interface ApiError {
        message: string;
        errors: { [index: string]: string };
        errorType: ErrorType;
    }

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

    interface CurrentUserInfo {
        user: User;
        externalIdentifier: string;
    }

    interface DueReviewPoints {
        toRepeat: number[];
        dueInDays: number;
    }

    interface DummyForGeneratingTypes {
        answerViewedByUser: AnswerViewedByUser;
        answerResult: AnswerResult;
        answer: Answer;
    }

    interface InitialInfo {
        thingId: number;
        skipReview: boolean;
    }

    interface LinkCreation {
        linkType: LinkType;
        fromTargetPerspective: boolean;
        moveUnder: boolean;
        asFirstChild: boolean;
    }

    interface LinkViewed {
        direct: Link[];
        reverse: Link[];
    }

    interface LinksOfANote {
        links: { [P in LinkType]?: LinkViewed };
    }

    interface NoteCreation {
        linkTypeToParent: LinkType;
        textContent: TextContent;
        wikidataId?: string;
    }

    interface NoteInfo {
        reviewPoint: ReviewPoint;
        note: NoteRealm;
        createdAt: string;
        reviewSetting: ReviewSetting;
    }

    interface NotePositionViewedByUser {
        noteId: number;
        notebook: NotebookViewedByUser;
        ancestors: Note[];
    }

    interface NoteRealm {
        id: number;
        links: LinksOfANote;
        children: Note[];
        note: Note;
    }

    interface NoteRealmWithAllDescendants {
        notePosition: NotePositionViewedByUser;
        notes: NoteRealm[];
    }

    interface NoteRealmWithPosition {
        notePosition: NotePositionViewedByUser;
        noteRealm: NoteRealm;
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

    interface QuizQuestion {
        quizQuestionId: number;
        rawJsonQuestion: string;
        questionType: QuestionType;
        description: string;
        mainTopic: string;
        hintLinks: LinksOfANote;
        viceReviewPointIdList: number[];
        notebookPosition?: NotePositionViewedByUser;
        options: Option[];
        pictureWithMask?: PictureWithMask;
    }

    interface RedirectToNoteResponse {
        noteId: number;
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
        adjustment: number;
    }

    interface UserForOtherUserView {
        name: string;
    }

    interface WikidataAssociationCreation {
        wikidataId: string;
    }

    interface WikidataEntityData {
        WikidataTitleInEnglish: string;
        WikipediaEnglishUrl: string;
    }

    interface WikidataSearchEntity {
        id: string;
        label: string;
        description: string;
    }

    interface User {
        id: number;
        name: string;
        externalIdentifier: string;
        ownership: Ownership;
        dailyNewNotesCount: number;
        spaceIntervals: string;
        aiQuestionTypeOnlyForReview: boolean;
    }

    interface AnswerViewedByUser {
        answerResult: AnswerResult;
        answerDisplay: string;
        reviewPoint: ReviewPoint;
        quizQuestion: QuizQuestion;
    }

    interface AnswerResult {
        answerId: number;
        correct: boolean;
    }

    interface Answer {
        spellingAnswer?: string;
        choiceIndex?: number;
    }

    interface Link extends Thingy {
        sourceNote: Note;
        targetNote: Note;
        linkType: LinkType;
    }

    interface TextContent {
        title: string;
        description: string;
        updatedAt: string;
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

    interface ReviewSetting {
        id: number;
        rememberSpelling: boolean;
        level: number;
    }

    interface Note extends Thingy {
        title: string;
        parentId?: number;
        noteAccessories: NoteAccessories;
        location?: NoteLocation;
        wikidataId: string;
        textContent: TextContent;
        deletedAt: string;
        pictureWithMask?: PictureWithMask;
    }

    interface Ownership {
        id: number;
        circle?: Circle;
    }

    interface Subscription {
        headNote: Note;
        title: string;
        id: number;
        dailyTargetOfNewNotes: number;
        user: User;
        notebook: Notebook;
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

    interface Thingy {
        id: number;
    }

    interface Thing {
        id: number;
        createdAt: string;
        note?: Note;
        link?: Link;
    }

    interface NoteAccessories {
        url: string;
        urlIsVideo: boolean;
        pictureUrl: string;
        pictureMask: string;
        useParentPicture: boolean;
        skipReview: boolean;
        questionGenerationInstruction: string;
        updatedAt: string;
    }

    interface NoteLocation {
        latitude: number;
        longitude: number;
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

    type ErrorType = "OPENAI_UNAUTHORIZED" | "BINDING_ERROR" | "OPENAI_TIMEOUT" | "OPENAI_SERVICE_ERROR";

    type LinkType = "no link" | "related to" | "a specialization of" | "an application of" | "an instance of" | "a part of" | "tagged by" | "an attribute of" | "the opposite of" | "author of" | "using" | "an example of" | "before" | "similar to" | "confused with";

    type QuestionType = "JUST_REVIEW" | "CLOZE_SELECTION" | "SPELLING" | "PICTURE_TITLE" | "PICTURE_SELECTION" | "LINK_TARGET" | "LINK_SOURCE" | "LINK_SOURCE_WITHIN_SAME_LINK_TYPE" | "CLOZE_LINK_TARGET" | "DESCRIPTION_LINK_TARGET" | "WHICH_SPEC_HAS_INSTANCE" | "FROM_SAME_PART_AS" | "FROM_DIFFERENT_PART_AS" | "AI_QUESTION";

}
