/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from './Note';
import type { PictureWithMask } from './PictureWithMask';
export type QuizQuestionAIQuestion = {
    id: number;
    note?: Note;
    createdAt?: string;
    rawJsonQuestion?: string;
    correctAnswerIndex?: number;
    stem?: string;
    mainTopic?: string;
    pictureWithMask?: PictureWithMask;
};

