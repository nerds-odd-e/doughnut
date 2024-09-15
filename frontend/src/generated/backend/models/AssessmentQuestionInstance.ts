/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BareQuestion } from './BareQuestion';
import type { ReviewQuestionInstance } from './ReviewQuestionInstance';
export type AssessmentQuestionInstance = {
    id: number;
    bareQuestion: BareQuestion;
    reviewQuestionInstance: ReviewQuestionInstance;
    answered?: boolean;
    answeredCorrectly?: boolean;
};

