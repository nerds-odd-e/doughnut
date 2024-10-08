/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AssessmentQuestionInstance } from './AssessmentQuestionInstance';
import type { Ownership } from './Ownership';
import type { User } from './User';
export type Conversation = {
    id: number;
    assessmentQuestionInstance?: AssessmentQuestionInstance;
    subjectOwnership?: Ownership;
    conversationInitiator?: User;
    message?: string;
    createdAt: string;
    updatedAt: string;
};

