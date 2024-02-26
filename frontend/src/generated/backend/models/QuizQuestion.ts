/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Choice } from './Choice';
import type { Note } from './Note';
import type { PictureWithMask } from './PictureWithMask';
export type QuizQuestion = {
    id: number;
    stem?: string;
    mainTopic?: string;
    headNote?: Note;
    choices?: Array<Choice>;
    pictureWithMask?: PictureWithMask;
};

