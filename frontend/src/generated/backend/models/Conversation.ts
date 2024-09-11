/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { QuizQuestion } from './QuizQuestion';
import type { User } from './User';
export type Conversation = {
    id: number;
    quizQuestion?: QuizQuestion;
    noteCreator?: User;
    conversationInitiator?: User;
    message?: string;
};

