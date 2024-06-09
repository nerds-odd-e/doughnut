/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ImageWithMask } from './ImageWithMask';
import type { MultipleChoicesQuestion } from './MultipleChoicesQuestion';
import type { Note } from './Note';
export type QuizQuestion = {
    id: number;
    multipleChoicesQuestion: MultipleChoicesQuestion;
    headNote: Note;
    imageWithMask?: ImageWithMask;
    createdAt?: string;
};

