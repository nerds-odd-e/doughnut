/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { QuestionAndAnswer } from './QuestionAndAnswer';
import type { User } from './User';
export type Conversation = {
    id: number;
    questionAndAnswer?: QuestionAndAnswer;
    noteCreator?: User;
    conversationInitiator?: User;
    message?: string;
};

