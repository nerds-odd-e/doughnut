/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AssessmentQuestionInstance } from './AssessmentQuestionInstance';
export type AssessmentAttempt = {
    id: number;
    totalQuestionCount?: number;
    answersCorrect?: number;
    notebookId: number;
    notebookTitle?: string;
    submittedAt?: string;
    isPass?: boolean;
    assessmentQuestionInstances?: Array<AssessmentQuestionInstance>;
    certifiable?: boolean;
};

