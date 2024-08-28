/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { QuizQuestionAndAnswer } from './QuizQuestionAndAnswer';
import type { User } from './User';
export type Conversation = {
    id: number;
    quizQuestionAndAnswer?: QuizQuestionAndAnswer;
    noteCreator?: User;
    conversationInitiator?: User;
    message?: string;
};

