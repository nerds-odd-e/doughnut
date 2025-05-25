/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { MCQWithAnswer } from './MCQWithAnswer';
export type SuggestedQuestionForFineTuning = {
    id: number;
    comment?: string;
    preservedQuestion: MCQWithAnswer;
    preservedNoteContent?: string;
    realCorrectAnswers?: string;
    createdAt?: string;
    positiveFeedback?: boolean;
};

