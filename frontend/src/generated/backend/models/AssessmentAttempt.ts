/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { QuizQuestion } from './QuizQuestion';
export type AssessmentAttempt = {
    id: number;
    notebookTitle?: string;
    submittedAt: string;
    isPass?: boolean;
    answersTotal?: number;
    answersCorrect?: number;
    quizQuestions?: Array<QuizQuestion>;
    notebookId?: number;
};

