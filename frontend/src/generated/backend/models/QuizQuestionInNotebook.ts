/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ImageWithMask } from './ImageWithMask';
import type { MultipleChoicesQuestion } from './MultipleChoicesQuestion';
import type { Notebook } from './Notebook';
export type QuizQuestionInNotebook = {
    id: number;
    multipleChoicesQuestion: MultipleChoicesQuestion;
    imageWithMask?: ImageWithMask;
    notebook: Notebook;
};

