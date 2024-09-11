/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { QuestionAndAnswer } from './QuestionAndAnswer';
import type { ReviewPoint } from './ReviewPoint';
export type AnsweredQuestion = {
    answerId?: number;
    correct?: boolean;
    correctChoiceIndex?: number;
    choiceIndex?: number;
    answerDisplay?: string;
    reviewPoint?: ReviewPoint;
    questionAndAnswer?: QuestionAndAnswer;
};

