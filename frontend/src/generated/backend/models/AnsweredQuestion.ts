/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Answer } from './Answer';
import type { PredefinedQuestion } from './PredefinedQuestion';
import type { ReviewPoint } from './ReviewPoint';
export type AnsweredQuestion = {
    reviewPoint?: ReviewPoint;
    predefinedQuestion: PredefinedQuestion;
    answer: Answer;
    answerDisplay?: string;
};

