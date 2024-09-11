/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { PredefinedQuestion } from './PredefinedQuestion';
import type { User } from './User';
export type Conversation = {
    id: number;
    predefinedQuestion?: PredefinedQuestion;
    noteCreator?: User;
    conversationInitiator?: User;
    message?: string;
};

