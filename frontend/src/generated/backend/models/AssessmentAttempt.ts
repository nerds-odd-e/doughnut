/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type AssessmentAttempt = {
    id: number;
    submittedAt: string;
    answersTotal?: number;
    answersCorrect?: number;
    assessmentHistory?: AssessmentAttempt;
    isPass?: boolean;
    notebookTitle?: string;
};

