/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ReviewQuestionInstance } from './ReviewQuestionInstance';
export type AssessmentAttempt = {
    id: number;
    notebookTitle?: string;
    submittedAt: string;
    isPass?: boolean;
    answersTotal?: number;
    answersCorrect?: number;
    reviewQuestionInstances?: Array<ReviewQuestionInstance>;
    notebookId: number;
};

