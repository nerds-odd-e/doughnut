/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Choice } from './Choice';
import type { ImageWithMask } from './ImageWithMask';
import type { Note } from './Note';
export type QuizQuestion = {
    id: number;
    stem?: string;
    mainTopic?: string;
    headNote?: Note;
    choices?: Array<Choice>;
    imageWithMask?: ImageWithMask;
};

