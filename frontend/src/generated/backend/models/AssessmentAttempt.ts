/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AssessmentQuestionInstance } from './AssessmentQuestionInstance';
export type AssessmentAttempt = {
    id: number;
    notebookTitle?: string;
    submittedAt?: string;
    isPass?: boolean;
    totalQuestionCount?: number;
    answersCorrect?: number;
    assessmentQuestionInstances?: Array<AssessmentQuestionInstance>;
    certifiable?: boolean;
    notebookId: number;
};

