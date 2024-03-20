/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { QuizQuestion } from './QuizQuestion';
import type { ReviewPoint } from './ReviewPoint';
export type AnsweredQuestion = {
    answerId?: number;
    correct?: boolean;
    correctChoiceIndex?: number;
    choiceIndex?: number;
    answerDisplay?: string;
    reviewPoint?: ReviewPoint;
    quizQuestion?: QuizQuestion;
};

