/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ImageWithMask } from './ImageWithMask';
import type { MultipleChoicesQuestion } from './MultipleChoicesQuestion';
import type { QuizQuestion } from './QuizQuestion';
export type QuestionAndAnswer = {
    id: number;
    quizQuestion: QuizQuestion;
    multipleChoicesQuestion: MultipleChoicesQuestion;
    checkSpell?: boolean;
    imageWithMask?: ImageWithMask;
    correctAnswerIndex?: number;
    approved?: boolean;
};

