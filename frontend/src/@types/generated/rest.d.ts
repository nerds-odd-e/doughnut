/* tslint:disable */
/* eslint-disable */

declare namespace Generated {

    interface AiCompletionAnswerClarifyingQuestionParams extends AiCompletionParams {
        threadId: string;
        runId: string;
        toolCallId: string;
        answer: string;
    }

    interface AiCompletionParams {
        detailsToComplete: string;
    }

    interface AiCompletionRequiredAction {
        toolCallId: string;
        clarifyingQuestion?: ClarifyingQuestion;
        contentToAppend?: string;
    }

    interface AiCompletionResponse {
        threadId: string;
        runId: string;
        requiredAction?: AiCompletionRequiredAction;
        lastMessage: string;
    }

    interface AiGeneratedImage {
        b64encoded: string;
    }

    interface ApiError {
        message: string;
        errors: { [index: string]: string };
        errorType: ErrorType;
    }

    interface ChatRequest {
        userMessage: string;
    }

    interface ChatResponse {
        assistantMessage: string;
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

    interface ClarifyingQuestionAndAnswer {
        questionFromAI: ClarifyingQuestion;
        answerFromUser: string;
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
        answeredQuestion: AnsweredQuestion;
        answer: Answer;
        suggestedQuestionForFineTuning: SuggestedQuestionForFineTuning;
    }

    interface GlobalAiModelSettings {
        questionGenerationModel: string;
        evaluationModel: string;
        othersModel: string;
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

    interface NoteCreationDTO extends NoteUpdateTopicDTO {
        linkTypeToParent?: LinkType;
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
        links: { [P in LinkType]?: LinkViewed };
        children: Note[];
        note: Note;
        notePosition: NotePositionViewedByUser;
    }

    interface NoteUpdateDetailsDTO {
        details: string;
    }

    interface NoteUpdateTopicDTO {
        topicConstructor: string;
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

    interface QuestionSuggestionCreationParams {
        comment: string;
        isPositiveFeedback: boolean;
    }

    interface QuestionSuggestionParams {
        comment: string;
        preservedQuestion: MCQWithAnswer;
        preservedNoteContent: string;
        positiveFeedback: boolean;
        realCorrectAnswers: string;
    }

    interface QuizQuestion {
        quizQuestionId: number;
        questionType: QuestionType;
        stem: string;
        mainTopic: string;
        notebookPosition?: NotePositionViewedByUser;
        choices: Choice[];
        pictureWithMask?: PictureWithMask;
    }

    interface QuizQuestionContestResult {
        reason: string;
        rejected: boolean;
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

    interface ClarifyingQuestion {
        /**
         * content of the question as plain string.
         */
        question: string;
    }

    interface User extends EntityIdentifiedByIdOnly {
        name: string;
        externalIdentifier: string;
        ownership: Ownership;
        dailyNewNotesCount: number;
        spaceIntervals: string;
        aiQuestionTypeOnlyForReview: boolean;
        admin: boolean;
    }

    interface AnsweredQuestion {
        answerId: number;
        correct: boolean;
        correctChoiceIndex?: number;
        choiceIndex?: number;
        answerDisplay: string;
        reviewPoint?: ReviewPoint;
        quizQuestion: QuizQuestion;
    }

    interface Answer extends EntityIdentifiedByIdOnly {
        spellingAnswer?: string;
        choiceIndex?: number;
    }

    interface SuggestedQuestionForFineTuning extends EntityIdentifiedByIdOnly {
        comment: string;
        preservedQuestion: MCQWithAnswer;
        preservedNoteContent: string;
        realCorrectAnswers: string;
        createdAt?: string;
        positiveFeedback: boolean;
    }

    interface Link extends Thingy {
        sourceNote: Note;
        targetNote: Note;
        linkType: LinkType;
    }

    interface ReviewPoint extends EntityIdentifiedByIdOnly {
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
        skipReview: boolean;
        level: number;
    }

    interface Note extends NoteBase {
        topic: string;
        parentId?: number;
        updatedAt: string;
        wikidataId: string;
        pictureWithMask?: PictureWithMask;
    }

    interface Ownership {
        id: number;
        circle?: Circle;
    }

    interface Subscription extends EntityIdentifiedByIdOnly {
        headNote: Note;
        title: string;
        dailyTargetOfNewNotes: number;
        user: User;
        notebook: Notebook;
    }

    interface MCQWithAnswer extends MultipleChoicesQuestion {
        /**
         * Index of the correct choice. 0-based.
         */
        correctChoiceIndex: number;
    }

    interface Choice {
        display: string;
        pictureWithMask?: PictureWithMask;
        picture: boolean;
    }

    interface PictureWithMask {
        notePicture: string;
        pictureMask: string;
    }

    interface EntityIdentifiedByIdOnly {
        id: number;
    }

    interface Thingy extends EntityIdentifiedByIdOnly {
    }

    interface Thing extends EntityIdentifiedByIdOnly {
        createdAt: string;
        note?: Note;
        link?: Link;
    }

    interface NoteAccessories {
        url: string;
        pictureUrl: string;
        pictureMask: string;
        useParentPicture: boolean;
    }

    interface NoteBase extends Thingy {
        noteAccessories: NoteAccessories;
        /**
         * The details of the note is in markdown format.
         */
        details: string;
        topicConstructor: string;
        deletedAt: string;
    }

    interface Circle extends EntityIdentifiedByIdOnly {
        name: string;
    }

    interface Notebook extends EntityIdentifiedByIdOnly {
        ownership: Ownership;
        headNote: Note;
        skipReviewEntirely: boolean;
        deletedAt: string;
    }

    interface MultipleChoicesQuestion {
        /**
         * The stem of the multiple-choice question. Provide background or disclosure necessary to clarify the question when needed.
         */
        stem: string;
        /**
         * All choices. Only one should be correct.
         */
        choices: string[];
    }

    type ErrorType = "OPENAI_UNAUTHORIZED" | "BINDING_ERROR" | "OPENAI_TIMEOUT" | "OPENAI_SERVICE_ERROR" | "WIKIDATA_SERVICE_ERROR";

    type LinkType = "no link" | "related to" | "a specialization of" | "an application of" | "an instance of" | "a part of" | "tagged by" | "an attribute of" | "the opposite of" | "author of" | "using" | "an example of" | "before" | "similar to" | "confused with";

    type QuestionType = "CLOZE_SELECTION" | "SPELLING" | "PICTURE_TITLE" | "PICTURE_SELECTION" | "LINK_TARGET" | "LINK_SOURCE" | "LINK_SOURCE_WITHIN_SAME_LINK_TYPE" | "CLOZE_LINK_TARGET" | "DESCRIPTION_LINK_TARGET" | "WHICH_SPEC_HAS_INSTANCE" | "FROM_SAME_PART_AS" | "FROM_DIFFERENT_PART_AS" | "AI_QUESTION";

}
