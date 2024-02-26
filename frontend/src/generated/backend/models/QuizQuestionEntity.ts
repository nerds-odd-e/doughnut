/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from './Note';
import type { PictureWithMask } from './PictureWithMask';
export type QuizQuestionEntity = {
    id: number;
    stem?: string;
    correctAnswerIndex?: number;
    mainTopic?: string;
    pictureWithMask?: PictureWithMask;
    note?: Note;
    createdAt?: string;
};

