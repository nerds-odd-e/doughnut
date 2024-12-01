/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Answer } from './Answer';
import type { Note } from './Note';
import type { PredefinedQuestion } from './PredefinedQuestion';
export type AnsweredQuestion = {
    note?: Note;
    predefinedQuestion: PredefinedQuestion;
    answer: Answer;
    answerDisplay?: string;
    recallPromptId: number;
};

