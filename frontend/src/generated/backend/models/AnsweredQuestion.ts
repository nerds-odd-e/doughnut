/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { PredefinedQuestion } from './PredefinedQuestion';
import type { ReviewPoint } from './ReviewPoint';
export type AnsweredQuestion = {
    answerId?: number;
    correct?: boolean;
    correctChoiceIndex?: number;
    choiceIndex?: number;
    answerDisplay?: string;
    reviewPoint?: ReviewPoint;
    predefinedQuestion?: PredefinedQuestion;
};

