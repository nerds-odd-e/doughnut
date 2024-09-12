/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ReviewQuestionInstance } from './ReviewQuestionInstance';
import type { User } from './User';
export type Conversation = {
    id: number;
    reviewQuestionInstance?: ReviewQuestionInstance;
    noteCreator?: User;
    conversationInitiator?: User;
    message?: string;
};

