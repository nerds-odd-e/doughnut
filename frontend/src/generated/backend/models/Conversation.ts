/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AssessmentQuestionInstance } from './AssessmentQuestionInstance';
import type { ConversationSubject } from './ConversationSubject';
import type { Note } from './Note';
import type { Ownership } from './Ownership';
import type { User } from './User';
export type Conversation = {
    id: number;
    subject?: ConversationSubject;
    subjectOwnership?: Ownership;
    conversationInitiator?: User;
    createdAt: string;
    updatedAt: string;
    assessmentQuestionInstance?: AssessmentQuestionInstance;
    note?: Note;
};

