/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoteTopic } from './NoteTopic';
import type { QuizQuestionAndAnswer } from './QuizQuestionAndAnswer';
export type Note = {
    noteTopic: NoteTopic;
    details?: string;
    parentId?: number;
    updatedAt: string;
    id: number;
    createdAt: string;
    readonly deletedAt?: string;
    wikidataId?: string;
    quizQuestionAndAnswers?: Array<QuizQuestionAndAnswer>;
};

